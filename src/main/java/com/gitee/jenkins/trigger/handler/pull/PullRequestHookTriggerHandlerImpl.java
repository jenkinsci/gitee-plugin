package com.gitee.jenkins.trigger.handler.pull;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.gitee.hook.model.*;
import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.publisher.GiteeMessagePublisher;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import com.gitee.jenkins.trigger.handler.AbstractWebHookTriggerHandler;
import com.gitee.jenkins.util.BuildUtil;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static com.gitee.jenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;
import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;

/**
 * @author Robin Müller
 * @author Yashin Luo
 */
class PullRequestHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PullRequestHook> implements PullRequestHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(PullRequestHookTriggerHandlerImpl.class.getName());

    private final Collection<State> allowedStates;
    private final boolean skipWorkInProgressPullRequest;
    private final boolean ciSkipFroTestNotRequired;
	private final Collection<Action> allowedActions;
	private final Collection<ActionDesc> allowedActionDesces;
    private final boolean cancelPendingBuildsOnUpdate;
    private final boolean cancelIncompleteBuildOnSamePullRequest;
    private boolean ignorePullRequestConflicts;

    PullRequestHookTriggerHandlerImpl(Collection<State> allowedStates, boolean skipWorkInProgressPullRequest, boolean cancelPendingBuildsOnUpdate, boolean ciSkipFroTestNotRequired, boolean cancelIncompleteBuildOnSamePullRequest, boolean ignorePullRequestConflicts) {
        this(allowedStates, EnumSet.allOf(Action.class), EnumSet.allOf(ActionDesc.class), skipWorkInProgressPullRequest, cancelPendingBuildsOnUpdate, ciSkipFroTestNotRequired, cancelIncompleteBuildOnSamePullRequest, ignorePullRequestConflicts);
    }

    PullRequestHookTriggerHandlerImpl(Collection<State> allowedStates, Collection<Action> allowedActions, Collection<ActionDesc> allowedActionDesces, boolean skipWorkInProgressPullRequest, boolean cancelPendingBuildsOnUpdate, boolean ciSkipFroTestNotRequired, boolean cancelIncompleteBuildOnSamePullRequest, boolean ignorePullRequestConflicts) {
        this.allowedStates = allowedStates;
        this.allowedActions = allowedActions;
        this.allowedActionDesces = allowedActionDesces;
        this.skipWorkInProgressPullRequest = skipWorkInProgressPullRequest;
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
        this.ciSkipFroTestNotRequired = ciSkipFroTestNotRequired;
        this.cancelIncompleteBuildOnSamePullRequest = cancelIncompleteBuildOnSamePullRequest;
        this.ignorePullRequestConflicts = ignorePullRequestConflicts;
    }

    @Override
    public void handle(Job<?, ?> job, PullRequestHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        PullRequestObjectAttributes objectAttributes = hook.getPullRequest();

        try {
            LOGGER.log(Level.INFO, "request hook  state=" + hook.getState() + ", action = " + hook.getAction() + " pr iid = " + objectAttributes.getNumber() + " hook name = " + hook.getHookName());
            if (isAllowedByConfig(hook)
                && isNotSkipWorkInProgressPullRequest(objectAttributes)) {
                List<String> labelsNames = new ArrayList<>();
                if (hook.getLabels() != null) {
                    for (PullRequestLabel label : hook.getLabels()) {
                        labelsNames.add(label.getTitle());
                    }
                }

                // 若pr不可自动合并则评论至pr
                if (!ignorePullRequestConflicts && !objectAttributes.isMergeable()) {
                    LOGGER.log(Level.INFO, "This pull request can not be merge");
                    GiteeMessagePublisher publisher = GiteeMessagePublisher.getFromJob(job);
                    GiteeClient client = getClient(job);

                    if (publisher != null && client != null) {
                        PullRequest pullRequest = new PullRequest(objectAttributes);
                        LOGGER.log(Level.INFO, "sending message to gitee.....");
                        client.createPullRequestNote(pullRequest, ":bangbang: This pull request can not be merge! The build will not be triggered. Please manual merge conflict.");
                    }
                    return;
                }

                // 若PR不需要测试，且有设定值，则跳过构建
                if ( ciSkipFroTestNotRequired && !objectAttributes.getNeedTest()) {
                    LOGGER.log(Level.INFO, "Skip because this pull don't need test.");
                    return;
                }

                if (pullRequestLabelFilter.isPullRequestAllowed(labelsNames)) {
                    super.handle(job, hook, buildInstructionFilter, skipLastCommitHasBeenBuild, branchFilter, pullRequestLabelFilter);
                }
            }
            else {
                LOGGER.log(Level.INFO, "request is not allow, hook state=" + hook.getState() + ", action = " + hook.getAction() + ", action desc = " + hook.getActionDesc());
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "request is not allow, hook ----- #" + hook.toString());
            throw e;
        }

    }

    @Override
    protected boolean isCiSkip(PullRequestHook hook, BuildInstructionFilter buildInstructionFilter) {
        if (buildInstructionFilter != null && hook.getPullRequest() != null) {
            return !buildInstructionFilter.isBuildAllow(hook.getPullRequest().getBody());
        }
        return false;
    }

    @Override
    protected boolean isCommitSkip(Job<?, ?> project, PullRequestHook hook) {
        PullRequestObjectAttributes objectAttributes = hook.getPullRequest();

        if (objectAttributes != null && objectAttributes.getMergeCommitSha() != null) {
            Run<?, ?> mergeBuild = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, objectAttributes.getMergeCommitSha());
            if (mergeBuild != null && StringUtils.equals(getTargetBranchFromBuild(mergeBuild), objectAttributes.getTargetBranch())) {
                LOGGER.log(Level.INFO, "Last commit in Pull Request has already been built in build #" + mergeBuild.getNumber());
                return true;
            }
        }
        return false;
    }

    @Override
    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, PullRequestHook hook) {
        if (!this.cancelPendingBuildsOnUpdate) {
            return;
        }
        if (!hook.getAction().equals(Action.update)) {
            return;
        }
        this.pendingBuildsHandler.cancelPendingBuilds(job, hook.getPullRequest().getSourceProjectId(), hook.getPullRequest().getSourceBranch());
    }

    @Override
    protected void cancelIncompleteBuildIfNecessary(Job<?, ?> job, PullRequestHook hook) {
        if (!cancelIncompleteBuildOnSamePullRequest) {
            return;
        }

        for (Run<?, ?> build : job.getBuilds()) {
            if (!job.isBuilding()) {
                break;
            }

            if (!build.isBuilding()) {
                continue;
            }

            CauseAction causeAction = build.getAction(CauseAction.class);
            GiteeWebHookCause giteeWebHookCause = null;
            for (Cause cause : causeAction.getCauses()) {
                if (cause instanceof GiteeWebHookCause) {
                    giteeWebHookCause = (GiteeWebHookCause) cause;
                    break;
                }
            }

            if (giteeWebHookCause == null) {
                continue;
            }
            CauseData causeData = giteeWebHookCause.getData();
            if (causeData.getSourceRepoHttpUrl().equals(hook.getPullRequest().getSource().getGitHttpUrl())
                && causeData.getTargetRepoHttpUrl().equals(hook.getPullRequest().getTarget().getGitHttpUrl())
                && causeData.getRef().equals(hook.getPullRequest().getMergeReferenceName())) {
                try {
                    doStop(build);
                } catch (ServletException | IOException e) {
                    LOGGER.log(Level.WARNING, "Unable to abort incomplete build", e);
                }
            }

        }

    }

    @Override
    protected String getTargetBranch(PullRequestHook hook) {
        return hook.getPullRequest() == null ? null : hook.getPullRequest().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "pull request";
    }

    @Override
    protected CauseData retrieveCauseData(PullRequestHook hook) {
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
                .withPullRequestTitle(hook.getPullRequest().getTitle())
                .withPullRequestDescription(hook.getPullRequest().getBody())
                .withPullRequestId(hook.getPullRequest().getId())
                .withPullRequestIid(hook.getPullRequest().getNumber())
                .withPullRequestState(hook.getState().toString())
                .withMergedByUser(hook.getUser() == null ? null : hook.getUser().getUsername())
                .withPullRequestAssignee(hook.getAssignee() == null ? null : hook.getAssignee().getUsername())
                .withPullRequestTargetProjectId(hook.getPullRequest().getTargetProjectId())
                .withTargetBranch(hook.getPullRequest().getTargetBranch())
                .withTargetRepoName(hook.getPullRequest().getTarget().getName())
                .withTargetNamespace(hook.getPullRequest().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getPullRequest().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getPullRequest().getTarget().getGitHttpUrl())
                .withTriggeredByUser(hook.getPullRequest().getHead().getUser().getName())
                .withLastCommit(hook.getPullRequest().getMergeCommitSha())
                .withSha(hook.getPullRequest().getMergeCommitSha())
                .withAfter(hook.getPullRequest().getMergeCommitSha())
                .withRef(hook.getPullRequest().getMergeReferenceName())
                .withTargetProjectUrl(hook.getPullRequest().getTarget().getUrl())
                .withPathWithNamespace(hook.getRepo().getPathWithNamespace())
                .withJsonBody(hook.getJsonBody())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PullRequestHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        // 没有配置git源码管理
        if (gitSCM == null) {
            return new RevisionParameterAction(retrieveRevisionToBuild(hook));
        }
        URIish urIish = retrieveUrIish(hook, gitSCM);
        // webhook与git源码管理仓库对不上
        if (urIish == null) {
            return new RevisionParameterAction(retrieveRevisionToBuild2(hook));
        }
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), urIish);
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(PullRequestHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getPullRequest().getSourceProjectId())
            .withSha(hook.getPullRequest().getMergeCommitSha())
            .withRef(hook.getPullRequest().getSourceBranch())
            .build();
    }

    private String retrieveRevisionToBuild(PullRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getPullRequest() != null) {
            if (hook.getPullRequest().getMergeCommitSha() != null) {
                return hook.getPullRequest().getMergeCommitSha();
            }
        }
        return retrieveRevisionToBuild2(hook);
    }

    private String retrieveRevisionToBuild2(PullRequestHook hook) throws NoRevisionToBuildException {
        if (hook.getPullRequest() != null) {
            if (hook.getPullRequest().getMergeReferenceName() != null) {
                return hook.getPullRequest().getMergeReferenceName();
            }
        }
        throw new NoRevisionToBuildException();
    }

    private String getTargetBranchFromBuild(Run<?, ?> mergeBuild) {
        GiteeWebHookCause cause = mergeBuild.getCause(GiteeWebHookCause.class);
        return cause == null ? null : cause.getData().getTargetBranch();
    }

	private boolean isAllowedByConfig(PullRequestHook hook) {
		return allowedStates.contains(hook.getState())
        	&& allowedActions.contains(hook.getAction())
            && (hook.getAction() != Action.update || allowedActionDesces.contains(hook.getActionDesc()));
	}

	// Gitee 无此状态，暂时屏蔽
    private boolean isNotSkipWorkInProgressPullRequest(PullRequestObjectAttributes objectAttributes) {
//        Boolean workInProgress = objectAttributes.getWorkInProgress();
//        if (skipWorkInProgressPullRequest && workInProgress != null && workInProgress) {
//            LOGGER.log(Level.INFO, "Skip WIP Pull Request #{0} ({1})", toArray(objectAttributes.getNumber(), objectAttributes.getTitle()));
//            return false;
//        }
        return true;
    }
}
