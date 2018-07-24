package com.gitee.jenkins.trigger.handler.note;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.gitee.hook.model.PushHook;
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

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
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
    public void handle(Job<?, ?> job, NoteHook hook, boolean ciSkip, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {
        if (isValidTriggerPhrase(hook.getObjectAttributes().getNote())) {
            super.handle(job, hook, ciSkip, skipLastCommitHasBeenBuild, branchFilter, mergeRequestLabelFilter);
        }
    }

    @Override
    protected boolean isCiSkip(NoteHook hook) {
        return hook.getMergeRequest() != null
                && hook.getMergeRequest().getBody() != null
                && hook.getMergeRequest().getBody().contains("[ci-skip]");
    }

    @Override
    protected boolean isCommitSkip(Job<?, ?> project, NoteHook hook) {
        return false;
    }
    @Override
    protected String getTargetBranch(NoteHook hook) {
        return hook.getMergeRequest() == null ? null : hook.getMergeRequest().getTargetBranch();
    }

    @Override
    protected String getTriggerType() {
        return "note";
    }

    @Override
    protected CauseData retrieveCauseData(NoteHook hook) {
        return causeData()
                .withActionType(CauseData.ActionType.NOTE)
                .withSourceProjectId(hook.getMergeRequest().getSourceProjectId())
                .withTargetProjectId(hook.getMergeRequest().getTargetProjectId())
                .withBranch(hook.getMergeRequest().getSourceBranch())
                .withSourceBranch(hook.getMergeRequest().getSourceBranch())
                .withUserName(hook.getMergeRequest().getHead().getUser().getName())
                .withUserEmail(hook.getMergeRequest().getHead().getUser().getEmail())
                .withSourceRepoHomepage(hook.getMergeRequest().getSource().getHomepage())
                .withSourceRepoName(hook.getMergeRequest().getSource().getName())
                .withSourceNamespace(hook.getMergeRequest().getSource().getNamespace())
                .withSourceRepoUrl(hook.getMergeRequest().getSource().getUrl())
                .withSourceRepoSshUrl(hook.getMergeRequest().getSource().getSshUrl())
                .withSourceRepoHttpUrl(hook.getMergeRequest().getSource().getGitHttpUrl())
                .withMergeRequestTitle(hook.getMergeRequest().getTitle())
                .withMergeRequestDescription(hook.getMergeRequest().getBody())
                .withMergeRequestId(hook.getMergeRequest().getId())
                .withMergeRequestIid(hook.getMergeRequest().getNumber())
                .withMergeRequestTargetProjectId(hook.getMergeRequest().getTargetProjectId())
                .withTargetBranch(hook.getMergeRequest().getTargetBranch())
                .withTargetRepoName(hook.getMergeRequest().getTarget().getName())
                .withTargetNamespace(hook.getMergeRequest().getTarget().getNamespace())
                .withTargetRepoSshUrl(hook.getMergeRequest().getTarget().getSshUrl())
                .withTargetRepoHttpUrl(hook.getMergeRequest().getTarget().getGitHttpUrl())
                .withTriggeredByUser(hook.getMergeRequest().getHead().getUser().getName())
                .withLastCommit(hook.getMergeRequest().getMergeCommitSha())
                .withTargetProjectUrl(hook.getMergeRequest().getTarget().getUrl())
                .withTriggerPhrase(hook.getObjectAttributes().getNote())
                .build();
    }

    @Override
    protected RevisionParameterAction createRevisionParameter(NoteHook hook, GitSCM gitSCM) throws NoRevisionToBuildException {
        return new RevisionParameterAction(retrieveRevisionToBuild(hook), retrieveUrIish(hook, gitSCM));
    }

    @Override
    protected BuildStatusUpdate retrieveBuildStatusUpdate(NoteHook hook) {
        return buildStatusUpdate()
            .withProjectId(hook.getMergeRequest().getSourceProjectId())
            .withSha(hook.getMergeRequest().getMergeCommitSha())
            .withRef(hook.getMergeRequest().getSourceBranch())
            .build();
    }

    private String retrieveRevisionToBuild(NoteHook hook) throws NoRevisionToBuildException {
        if (hook.getMergeRequest() != null
                && hook.getMergeRequest().getMergeCommitSha() != null) {

            return hook.getMergeRequest().getMergeCommitSha();
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
