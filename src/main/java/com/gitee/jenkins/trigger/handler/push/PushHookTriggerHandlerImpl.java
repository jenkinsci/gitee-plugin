package com.gitee.jenkins.trigger.handler.push;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.gitee.hook.model.Commit;
import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.MergeRequestLabelFilter;
import com.gitee.jenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.eclipse.jgit.util.StringUtils;

import java.util.List;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static com.gitee.jenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<PushHook> implements PushHookTriggerHandler {

    private static final String NO_COMMIT = "0000000000000000000000000000000000000000";

    @Override
    public void handle(Job<?, ?> job, PushHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isNoRemoveBranchPush(hook)) {
            super.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(PushHook hook) {
        List<Commit> commits = hook.getCommits();
        return commits != null &&
               !commits.isEmpty() &&
               commits.get(commits.size() - 1).getMessage() != null &&
               commits.get(commits.size() - 1).getMessage().contains("[ci-skip]");
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
                .withSourceRepoHttpUrl(hook.getProject().getHttpUrl())
                .withMergeRequestTitle("")
                .withMergeRequestDescription("")
                .withMergeRequestId(null)
                .withMergeRequestIid(null)
                .withMergeRequestState(null)
                .withMergedByUser("")
                .withMergeRequestAssignee("")
                .withMergeRequestTargetProjectId(null)
                .withTargetBranch(getTargetBranch(hook))
                .withTargetRepoName("")
                .withTargetNamespace("")
                .withTargetRepoSshUrl("")
                .withTargetRepoHttpUrl("")
                .withTriggeredByUser(retrievePushedBy(hook))
                .withBefore(hook.getBefore())
                .withAfter(hook.getAfter())
                .withLastCommit(hook.getAfter())
                .withTargetProjectUrl(hook.getProject().getWebUrl())
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
        if (!StringUtils.isEmptyOrNull(userName)) {
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
}
