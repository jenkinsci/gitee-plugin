package com.gitee.jenkins.trigger.handler.note;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.gitee.hook.model.PullRequestObjectAttributes;
import com.gitee.jenkins.publisher.GiteeMessagePublisher;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import com.gitee.jenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;
import static com.gitee.jenkins.trigger.handler.builder.generated.BuildStatusUpdateBuilder.buildStatusUpdate;

/**
 * @author Nikolay Ustinov
 * @author Yashin Luo
 */
class NoteHookTriggerHandlerImpl extends AbstractWebHookTriggerHandler<NoteHook> implements NoteHookTriggerHandler {

    private static final Logger LOGGER = Logger.getLogger(NoteHookTriggerHandlerImpl.class.getName());

    private final String noteRegex;

    NoteHookTriggerHandlerImpl(String noteRegex) {
        this.noteRegex = noteRegex;
    }

    @Override
    public void handle(Job<?, ?> job, NoteHook hook, boolean ciSkip, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        if (isValidTriggerPhrase(hook.getComment().getBody())) {
            // 若pr不可自动合并则评论至pr
            PullRequestObjectAttributes objectAttributes = hook.getPullRequest();
            if (objectAttributes != null && !objectAttributes.isMergeable()) {
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
            super.handle(job, hook, ciSkip, skipLastCommitHasBeenBuild, branchFilter, pullRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(NoteHook hook) {
        return hook.getPullRequest() != null
                && hook.getPullRequest().getBody() != null
                && hook.getPullRequest().getBody().contains("[ci-skip]");
    }

    @Override
    protected boolean isCommitSkip(Job<?, ?> project, NoteHook hook) {
        return false;
    }
    @Override
    protected String getTargetBranch(NoteHook hook) {
        return hook.getPullRequest() == null ? null : hook.getPullRequest().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "note";
    }

    @Override
    protected CauseData retrieveCauseData(NoteHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.NOTE)
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
                .withPullRequestTargetProjectId(hook.getPullRequest().getTargetProjectId())
                .withTargetBranch(hook.getPullRequest().getTargetBranch())
                .withTargetRepoName(hook.getPullRequest().getTarget().getName())
                .withTargetNamespace(hook.getPullRequest().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getPullRequest().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getPullRequest().getTarget().getGitHttpUrl())
                .withTriggeredByUser(hook.getPullRequest().getHead().getUser().getName())
                .withLastCommit(hook.getPullRequest().getMergeCommitSha())
                .withTargetProjectUrl(hook.getPullRequest().getTarget().getUrl())
                .withTriggerPhrase(hook.getComment().getBody())
                .withPathWithNamespace(hook.getPullRequest().getBase().getRepo().getPathWithNamespace())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(NoteHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook, gitSCM));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(NoteHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getPullRequest().getSourceProjectId())
            .withSha(hook.getPullRequest().getMergeCommitSha())
            .withRef(hook.getPullRequest().getSourceBranch())
            .build();
    }

    private String retrieveRevisionToBuild(NoteHook hook) throws NoRevisionToBuildException {
        if (hook.getPullRequest() != null
            && hook.getPullRequest().getMergeReferenceName() != null) {
            return hook.getPullRequest().getMergeReferenceName();
        } else {
            throw new NoRevisionToBuildException();
        }
    }

    private boolean isValidTriggerPhrase(String note) {
        if (StringUtils.isEmpty(this.noteRegex)) {
            return false;
        }
        final Pattern pattern = Pattern.compile(this.noteRegex);
        return pattern.matcher(note).matches();
    }
}
