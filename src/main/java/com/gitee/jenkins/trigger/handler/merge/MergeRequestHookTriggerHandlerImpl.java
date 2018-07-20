package com.gitee.jenkins.trigger.handler.merge;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.MergeRequestHook;
import com.gitee.jenkins.gitee.hook.model.MergeRequestObjectAttributes;
import com.gitee.jenkins.gitee.hook.model.MergeRequestLabel;
import com.gitee.jenkins.gitee.hook.model.State;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.MergeRequestLabelFilter;
import com.gitee.jenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.gitee.jenkins.util.BuildUtil;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static com.gitee.jenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;
import static com.gitee.jenkins.util.LoggerUtil.toArray;

/**
 * @author Robin Müller
 * @author Yashin Luo
 */
class MergeRequestHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<MergeRequestHook> implements MergeRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(MergeRequestHookTriggerHandlerImpl.class.getName());

    private final Collection<State> allowedStates;
    private final boolean skipWorkInProgressMergeRequest;
	private final Collection<Action> allowedActions;
    private final boolean cancelPendingBuildsOnUpdate;

    MergeRequestHookTriggerHandlerImpl(Collection<State> allowedStates, boolean skipWorkInProgressMergeRequest, boolean cancelPendingBuildsOnUpdate) {
        this(allowedStates, EnumSet.allOf(Action.class), skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate);
    }

    MergeRequestHookTriggerHandlerImpl(Collection<State> allowedStates, Collection<Action> allowedActions, boolean skipWorkInProgressMergeRequest, boolean cancelPendingBuildsOnUpdate) {
        this.allowedStates = allowedStates;
        this.allowedActions = allowedActions;
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
    }

    @Override
    public void handle(Job<?, ?> job, MergeRequestHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        MergeRequestObjectAttributes objectAttributes = hook.getPullRequest();

        try {
            LOGGER.log(Level.INFO, "request hook  state=" + hook.getState() + ", action = " + hook.getAction() + " pr iid = " + objectAttributes.getNumber() + " hook name = " + hook.getHookName());
            if (isAllowedByConfig(hook)
                && isLastCommitNotYetBuild(job, hook)
                && isNotSkipWorkInProgressMergeRequest(objectAttributes)) {

                List<String> labelsNames = new ArrayList<>();
                if (hook.getLabels() != null) {
                    for (MergeRequestLabel label : hook.getLabels()) {
                        labelsNames.add(label.getTitle());
                    }
                }

                if (mergeRequestLabelFilter.isMergeRequestAllowed(labelsNames)) {
                    super.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
                }
            }
            else {
                LOGGER.log(Level.INFO, "request is not allow, hook state=" + hook.getState() + ", action = " + hook.getAction());
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "request is not allow, hook ----- #" + hook.toString());
            throw e;
        }

    }

    @Override
    protected boolean isCiSkip(MergeRequestHook hook) {
        return hook.getPullRequest() != null
                && hook.getPullRequest().getBody() != null
                && hook.getPullRequest().getBody().contains("[ci-skip]");
    }

    @Override
    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, MergeRequestHook hook) {
        if (!this.cancelPendingBuildsOnUpdate) {
            return;
        }
        if (!hook.getAction().equals(Action.update)) {
            return;
        }
        this.pendingBuildsHandler.cancelPendingBuilds(job, hook.getPullRequest().getSourceProjectId(), hook.getPullRequest().getSourceBranch());
    }

    @Override
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getPullRequest() == null ? null : hook.getPullRequest().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    @Override
    protected CauseData retrieveCauseData(MergeRequestHook hook) {
        return   causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(hook.getPullRequest().getSourceProjectId())
                .withTargetProjectId(hook.getPullRequest().getTargetProjectId())
                .withBranch(hook.getPullRequest().getSourceBranch())
                .withSourceBranch(hook.getPullRequest().getSourceBranch())
                .withUserName(hook.getPullRequest().getHead().getUser().getName())
                .withUserEmail(hook.getPullRequest().getHead().getUser().getEmail())
                .withSourceRepoHomepage(hook.getPullRequest().getSource().getHomepage())
                .withSourceRepoName(hook.getPullRequest().getSource().getName())
                .withSourceNamespace(hook.getPullRequest().getSource().getNamespace())
                .withSourceRepoUrl(hook.getPullRequest().getSource().getUrl())
                .withSourceRepoSshUrl(hook.getPullRequest().getSource().getSshUrl())
                .withSourceRepoHttpUrl(hook.getPullRequest().getSource().getGitHttpUrl())
                .withMergeRequestTitle(hook.getPullRequest().getTitle())
                .withMergeRequestDescription(hook.getPullRequest().getBody())
                .withMergeRequestId(hook.getPullRequest().getId())
                .withMergeRequestIid(hook.getPullRequest().getNumber())
                .withMergeRequestState(hook.getState().toString())
                .withMergedByUser(hook.getUser() == null ? null : hook.getUser().getUsername())
                .withMergeRequestAssignee(hook.getAssignee() == null ? null : hook.getAssignee().getUsername())
                .withMergeRequestTargetProjectId(hook.getPullRequest().getTargetProjectId())
                .withTargetBranch(hook.getPullRequest().getTargetBranch())
                .withTargetRepoName(hook.getPullRequest().getTarget().getName())
                .withTargetNamespace(hook.getPullRequest().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getPullRequest().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getPullRequest().getTarget().getGitHttpUrl())
                .withTriggeredByUser(hook.getPullRequest().getHead().getUser().getName())
                .withLastCommit(hook.getPullRequest().getMergeCommitSha())
                .withTargetProjectUrl(hook.getPullRequest().getTarget().getUrl())
                .withPathWithNamespace(hook.getRepo().getPathWithNamespace())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(MergeRequestHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook, gitSCM));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(MergeRequestHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getPullRequest().getSourceProjectId())
            .withSha(hook.getPullRequest().getMergeCommitSha())
            .withRef(hook.getPullRequest().getSourceBranch())
            .build();
    }

    private String retrieveRevisionToBuild(MergeRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getPullRequest() != null
                && hook.getPullRequest().getMergeReferenceName() != null) {
            return hook.getPullRequest().getMergeReferenceName();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestHook hook) {
        MergeRequestObjectAttributes objectAttributes = hook.getPullRequest();
        if (hook != null && hook.getAction() == Action.approved) {
            LOGGER.log(Level.FINEST, "Skipping LastCommitNotYetBuild check for approve action");
            return true;
        }

        if (objectAttributes != null && objectAttributes.getMergeCommitSha() != null) {
            Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, objectAttributes.getMergeCommitSha());
            if (mergeBuild != null && StringUtils.equals(getTargetBranchFromBuild(mergeBuild), objectAttributes.getTargetBranch())) {
                LOGGER.log(Level.INFO, "Last commit in Merge Request has already been built in build #" + mergeBuild.getNumber());
                return false;
            }
        }
        return true;
    }

    private String getTargetBranchFromBuild(Run<?, ?> mergeBuild) {
        GiteeWebHookCause cause = mergeBuild.getCause(GiteeWebHookCause.class);
        return cause == null ? null : cause.getData().getTargetBranch();
    }

	private boolean isAllowedByConfig(MergeRequestHook hook) {
		return allowedStates.contains(hook.getState())
        	&& allowedActions.contains(hook.getAction());
	}

    private boolean isNotSkipWorkInProgressMergeRequest(MergeRequestObjectAttributes objectAttributes) {
        Boolean workInProgress = objectAttributes.getWorkInProgress();
        if (skipWorkInProgressMergeRequest && workInProgress != null && workInProgress) {
            LOGGER.log(Level.INFO, "Skip WIP Merge Request #{0} ({1})", toArray(objectAttributes.getNumber(), objectAttributes.getTitle()));
            return false;
        }
        return true;
    }
}
