package com.gitee.jenkins.trigger.handler.note;

import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopNoteHookTriggerHandler implements NoteHookTriggerHandler {
    @Override
    public void handle(Job<?, ?> job, NoteHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        // nothing to do
    }
}
