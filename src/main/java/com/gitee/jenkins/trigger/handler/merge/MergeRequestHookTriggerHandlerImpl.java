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
        MergeRequestObjectAttributes objectAttributes = hook.getObjectAttributes();

        try {
            LOGGER.log(Level.INFO, "request hook  state=" + hook.getState() + ", action = " + hook.getAction() + " pr iid = " + objectAttributes.getIid() + " hook name = " + hook.getHookName());
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
        return hook.getObjectAttributes() != null
                && hook.getObjectAttributes().getDescription() != null
                && hook.getObjectAttributes().getDescription().contains("[ci-skip]");
    }

    @Override
    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, MergeRequestHook hook) {
        if (!this.cancelPendingBuildsOnUpdate) {
            return;
        }
        if (!hook.getAction().equals(Action.update)) {
            return;
        }
        this.pendingBuildsHandler.cancelPendingBuilds(job, hook.getObjectAttributes().getSourceProjectId(), hook.getObjectAttributes().getSourceBranch());
    }

    @Override
    protected String getTargetBranch(MergeRequestHook hook) {
        return hook.getObjectAttributes() == null ? null : hook.getObjectAttributes().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "merge request";
    }

    @Override
    protected CauseData retrieveCauseData(MergeRequestHook hook) {
        return   causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(hook.getObjectAttributes().getSourceProjectId())
                .withTargetProjectId(hook.getObjectAttributes().getTargetProjectId())
                .withBranch(hook.getObjectAttributes().getSourceBranch())
                .withSourceBranch(hook.getObjectAttributes().getSourceBranch())
                .withUserName(hook.getObjectAttributes().getHead().getUser().getName())
                .withUserEmail(hook.getObjectAttributes().getHead().getUser().getEmail())
                .withSourceRepoHomepage(hook.getObjectAttributes().getSource().getHomepage())
                .withSourceRepoName(hook.getObjectAttributes().getSource().getName())
                .withSourceNamespace(hook.getObjectAttributes().getSource().getNamespace())
                .withSourceRepoUrl(hook.getObjectAttributes().getSource().getUrl())
                .withSourceRepoSshUrl(hook.getObjectAttributes().getSource().getSshUrl())
                .withSourceRepoHttpUrl(hook.getObjectAttributes().getSource().getHttpUrl())
                .withMergeRequestTitle(hook.getObjectAttributes().getTitle())
                .withMergeRequestDescription(hook.getObjectAttributes().getDescription())
                .withMergeRequestId(hook.getObjectAttributes().getId())
                .withMergeRequestIid(hook.getObjectAttributes().getIid())
                .withMergeRequestState(hook.getState().toString())
                .withMergedByUser(hook.getUser() == null ? null : hook.getUser().getUsername())
                .withMergeRequestAssignee(hook.getAssignee() == null ? null : hook.getAssignee().getUsername())
                .withMergeRequestTargetProjectId(hook.getObjectAttributes().getTargetProjectId())
                .withTargetBranch(hook.getObjectAttributes().getTargetBranch())
                .withTargetRepoName(hook.getObjectAttributes().getTarget().getName())
                .withTargetNamespace(hook.getObjectAttributes().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getObjectAttributes().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getObjectAttributes().getTarget().getHttpUrl())
                .withTriggeredByUser(hook.getObjectAttributes().getHead().getUser().getName())
                .withLastCommit(hook.getObjectAttributes().getMergeCommitSha())
                .withTargetProjectUrl(hook.getObjectAttributes().getTarget().getUrl())
                .withPathWithNamespace(hook.getProject().getPathWithNamespace())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(MergeRequestHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(MergeRequestHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getObjectAttributes().getSourceProjectId())
            .withSha(hook.getObjectAttributes().getMergeCommitSha())
            .withRef(hook.getObjectAttributes().getSourceBranch())
            .build();
    }

    private String retrieveRevisionToBuild(MergeRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getObjectAttributes() != null
                && hook.getObjectAttributes().getMergeCommitSha() != null) {
            return hook.getObjectAttributes().getMergeCommitSha();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isLastCommitNotYetBuild(Job<?, ?> project, MergeRequestHook hook) {
        MergeRequestObjectAttributes objectAttributes = hook.getObjectAttributes();
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
            LOGGER.log(Level.INFO, "Skip WIP Merge Request #{0} ({1})", toArray(objectAttributes.getIid(), objectAttributes.getTitle()));
            return false;
        }
        return true;
    }
}
