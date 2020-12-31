package com.gitee.jenkins.trigger.handler.push;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.hook.model.Commit;
import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import com.gitee.jenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Job;
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import com.gitee.jenkins.util.BuildUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static com.gitee.jenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

/**
 * @author Robin MüllerPushHookTriggerHandlerImpl
 * @author Yashin Luo
 */
class PushHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PushHook> implements PushHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(PushHookTriggerHandlerImpl.class.getName());

    private static final String NO_COMMIT = "0000000000000000000000000000000000000000";

    @Override
    public void handle(Job<?, ?> job, PushHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        if (isNoRemoveBranchPush(hook)) {
            super.handle(job, hook, buildInstructionFilter, skipLastCommitHasBeenBuild, branchFilter, pullRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(PushHook hook, BuildInstructionFilter buildInstructionFilter) {
        List<Commit> commits = hook.getCommits();
        if (commits != null && !commits.isEmpty()) {
            return !buildInstructionFilter.isBuildAllow(commits.get(commits.size() - 1).getMessage());
        }
        return false;
    }

    @Override
    protected boolean isCommitSkip(Job<?, ?> project, PushHook hook) {
        String sha = hook.getAfter();
        if (hook != null && sha != null) {
            Run<?, ?> pushBuild = BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, sha);
            if (pushBuild != null && StringUtils.equals(getRefFromBuild(pushBuild), hook.getRef())) {
                LOGGER.log(Level.INFO, "Last commit in push has already been built sha=" + sha);
                return true;
            }
        }
        return false;
    }

    @Override
    protected CauseData retrieveCauseData(PushHook hook) {
//        fixme 判断是否push tag，Gitee 钩子未有相关数据
//        CauseData.ActionType actionType = hook.getObjectKind().equals("tag_push") ? CauseData.ActionType.TAG_PUSH : CauseData.ActionType.PUSH;
        CauseData.ActionType actionType = CauseData.ActionType.PUSH;
        return causeData()
                .withActionType(actionType)
                .withSourceProjectId(hook.getProjectId())
                .withTargetProjectId(hook.getProjectId())
                .withBranch(getTargetBranch(hook))
                .withSourceBranch(getTargetBranch(hook))
                .withUserName(hook.getUserName())
                .withUserEmail(hook.getUserEmail())
                .withSourceRepoHomepage(hook.getProject().getHomepage())
                .withSourceRepoName(hook.getProject().getName())
                .withSourceNamespace(hook.getProject().getNamespace())
                .withSourceRepoUrl(hook.getProject().getUrl())
                .withSourceRepoSshUrl(hook.getProject().getSshUrl())
                .withSourceRepoHttpUrl(hook.getProject().getGitHttpUrl())
                .withPullRequestTitle("")
                .withPullRequestDescription("")
                .withPullRequestId(null)
                .withPullRequestIid(null)
                .withPullRequestState(null)
                .withMergedByUser("")
                .withPullRequestAssignee("")
                .withPullRequestTargetProjectId(null)
                .withTargetBranch(getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withTriggeredByUser(retrievePushedBy(hook))
                .withBefore(hook.getBefore())
                .withAfter(hook.getAfter())
                .withRef(hook.getRef())
                .withLastCommit(hook.getAfter())
                .withSha(hook.getAfter())
                .withCreated(hook.getCreated())
                .withDeleted(hook.getDeleted())
                .withTargetProjectUrl(hook.getProject().getUrl())
                .withJsonBody(hook.getJsonBody())
                .build();
    }

    @Override
    protected String getTargetBranch(PushHook hook) {
        return hook.getRef() == null ? null : hook.getRef().replaceFirst("^refs/heads/", "");
    }

    @Override
    protected String getTriggerType() {
        return "push";
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(PushHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook, gitSCM), retrieveUrIish(hook));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(PushHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getProjectId())
            .withSha(hook.getAfter())
            .withRef(getTargetBranch(hook))
            .build();
    }

    private String retrievePushedBy(final PushHook hook) {

        final String userName = hook.getUserName();
        if (StringUtils.isNotBlank(userName)) {
            return userName;
        }

        final List<Commit> commits = hook.getCommits();
        if (commits != null && !commits.isEmpty()) {
            return commits.get(commits.size() - 1).getAuthor().getName();
        }

        return null;
    }

    private String retrieveRevisionToBuild(PushHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        if (inNoBranchDelete(hook)) {
            if (gitSCM != null && gitSCM.getRepositories().size() == 1) {
                String repositoryName = gitSCM.getRepositories().get(0).getName();
                return hook.getRef().replaceFirst("^refs/heads", "remotes/" + repositoryName);
            } else {
                return hook.getAfter();
            }
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean inNoBranchDelete(PushHook hook) {
        return hook.getAfter() != null && !hook.getAfter().equals(NO_COMMIT);
    }

    private boolean isNoRemoveBranchPush(PushHook hook) {
        return hook.getAfter() != null && !hook.getAfter().equals(NO_COMMIT);
    }

    private String getRefFromBuild(Run<?, ?> pushBuild) {
        GiteeWebHookCause cause = pushBuild.getCause(GiteeWebHookCause.class);
        return cause == null ? null : cause.getData().getRef();
    }
}
