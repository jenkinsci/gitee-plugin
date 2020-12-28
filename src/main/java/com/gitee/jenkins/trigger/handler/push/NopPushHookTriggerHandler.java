package com.gitee.jenkins.trigger.handler.push;

import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopPushHookTriggerHandler implements PushHookTriggerHandler {
    @Override
    public void handle(Job<?, ?> job, PushHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        // nothing to do
    }
}
