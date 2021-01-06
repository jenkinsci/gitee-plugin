package com.gitee.jenkins.trigger.handler.note;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.gitee.hook.model.*;
import com.gitee.jenkins.publisher.GiteeMessagePublisher;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import com.gitee.jenkins.trigger.handler.AbstractWebHookTriggerHandler;
import hudson.model.*;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;

import javax.servlet.ServletException;
import java.io.IOException;
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

    private final boolean triggerOnCommitComment;
    private final boolean triggerOnNoteRequest;
    private final String noteRegex;
    private final boolean ciSkipFroTestNotRequired;
    private final boolean cancelIncompleteBuildOnSamePullRequest;

    NoteHookTriggerHandlerImpl(boolean triggerOnCommitComment, boolean triggerOnNoteRequest, String noteRegex, boolean ciSkipFroTestNotRequired, boolean cancelIncompleteBuildOnSamePullRequest) {
        this.triggerOnCommitComment = triggerOnCommitComment;
        this.triggerOnNoteRequest = triggerOnNoteRequest;
        this.noteRegex = noteRegex;
        this.ciSkipFroTestNotRequired = ciSkipFroTestNotRequired;
        this.cancelIncompleteBuildOnSamePullRequest = cancelIncompleteBuildOnSamePullRequest;
    }

    @Override
    public void handle(Job<?, ?> job, NoteHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        if (isValidTrigger(hook)) {
            // 若pr不可自动合并则评论至pr
            PullRequestObjectAttributes objectAttributes = hook.getPullRequest();
            if (objectAttributes != null && !objectAttributes.isMergeable()) {
                LOGGER.log(Level.INFO, "This pull request can not be merge");
                // fixme 无法获取 publisher
                // java.lang.ClassCastException: org.jenkinsci.plugins.workflow.job.WorkflowJob cannot be cast to hudson.model.AbstractProject
                //	at com.gitee.jenkins.publisher.GiteeMessagePublisher.getFromJob(GiteeMessagePublisher.java:65)
                //	at com.gitee.jenkins.trigger.handler.note.NoteHookTriggerHandlerImpl.handle(NoteHookTriggerHandlerImpl.java:47)
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
            if (objectAttributes != null && ciSkipFroTestNotRequired && !objectAttributes.getNeedTest()) {
                LOGGER.log(Level.INFO, "Skip because this pull don't need test.");
                return;
            }


            super.handle(job, hook, buildInstructionFilter, skipLastCommitHasBeenBuild, branchFilter, pullRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(NoteHook hook, BuildInstructionFilter buildInstructionFilter) {
        return hook.getPullRequest() == null ? false : !buildInstructionFilter.isBuildAllow(hook.getPullRequest().getBody());
    }

    @Override
    protected boolean isCommitSkip(Job<?, ?> project, NoteHook hook) {
        return false;
    }

    @Override
    protected void cancelIncompleteBuildIfNecessary(Job<?, ?> job, NoteHook hook) {
        if (!cancelIncompleteBuildOnSamePullRequest || hook.getPullRequest() == null) {
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
    protected String getTargetBranch(NoteHook hook) {
        return hook.getPullRequest() == null ? null : hook.getPullRequest().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "note";
    }

    @Override
    protected CauseData retrieveCauseData(NoteHook hook) {
        // 兼容来自commit的评论
        if (hook.getPullRequest() == null) {
            return causeData()
                .withActionType(CauseData.ActionType.COMMIT_COMMENT)
                .withUserName(hook.getComment().getUser().getUsername())
                .withUserEmail(hook.getComment().getUser().getEmail())
                .withPullRequestTitle("")
                .withBranch("")
                .withSourceBranch("")
                .withSourceProjectId(hook.getProject().getId())
                .withSourceRepoHomepage(hook.getProject().getHomepage())
                .withSourceRepoName(hook.getProject().getName())
                .withSourceNamespace(hook.getProject().getNamespace())
                .withSourceRepoUrl(hook.getProject().getUrl())
                .withSourceRepoSshUrl(hook.getProject().getSshUrl())
                .withSourceRepoHttpUrl(hook.getProject().getGitHttpUrl())
                .withTargetBranch("")
                .withTargetProjectId(hook.getProject().getId())
                .withTargetRepoName(hook.getProject().getName())
                .withTargetNamespace(hook.getProject().getNamespace())
                .withTargetRepoSshUrl(hook.getProject().getSshUrl())
                .withTargetRepoHttpUrl(hook.getProject().getGitHttpUrl())
                .withTriggeredByUser(hook.getComment().getUser().getName())
                .withTriggerPhrase(hook.getComment().getBody())
                .withSha(hook.getComment().getCommitId())
                .withPathWithNamespace(hook.getProject().getPathWithNamespace())
                .build();
        }

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
                .withSha(hook.getPullRequest().getMergeCommitSha())
                .withAfter(hook.getPullRequest().getMergeCommitSha())
                .withRef(hook.getPullRequest().getMergeReferenceName())
                .withTargetProjectUrl(hook.getPullRequest().getTarget().getUrl())
                .withTriggerPhrase(hook.getComment().getBody())
                .withPathWithNamespace(hook.getPullRequest().getBase().getRepo().getPathWithNamespace())
                .withJsonBody(hook.getJsonBody())
                .withNoteBody(hook.getComment().getBody())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(NoteHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
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
    protected BuildStatusUpdate retrieveBuildStatusUpdate(NoteHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getPullRequest().getSourceProjectId())
            .withSha(hook.getPullRequest().getMergeCommitSha())
            .withRef(hook.getPullRequest().getTargetBranch())
            .build();
    }

    private String retrieveRevisionToBuild(NoteHook hook) throws NoRevisionToBuildException {
        if (hook.getPullRequest() != null) {
            if (hook.getPullRequest().getMergeCommitSha() != null) {
                return hook.getPullRequest().getMergeCommitSha();
            }
        }

        return retrieveRevisionToBuild2(hook);
    }

    private String retrieveRevisionToBuild2(NoteHook hook) throws NoRevisionToBuildException {
        if (hook.getPullRequest() != null) {
            if (hook.getPullRequest().getMergeReferenceName() != null) {
                return hook.getPullRequest().getMergeReferenceName();
            }
        }

        // 兼容来自commit的评论
        if (hook.getComment() != null
            && StringUtils.isNotBlank(hook.getComment().getCommitId())) {
            return hook.getComment().getCommitId();
        }

        throw new NoRevisionToBuildException();
    }

    private boolean isValidTrigger(NoteHook hook) {
        // commit评论pullRequest为null
        return ((triggerOnCommitComment && hook.getPullRequest() == null) || (triggerOnNoteRequest && hook.getPullRequest() != null))
            && (isValidTriggerPhrase(hook.getComment().getBody()) && isValidTriggerAction(hook.getAction()));
    }

    private boolean isValidTriggerAction(NoteAction action) {
        return action == NoteAction.comment;
    }

    private boolean isValidTriggerPhrase(String note) {
        if (StringUtils.isEmpty(this.noteRegex)) {
            return false;
        }
        final Pattern pattern = Pattern.compile(this.noteRegex);
        return pattern.matcher(note).matches();
    }
}
