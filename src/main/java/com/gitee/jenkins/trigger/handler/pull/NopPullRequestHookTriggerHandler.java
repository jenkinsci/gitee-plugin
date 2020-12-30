package com.gitee.jenkins.trigger.handler.pull;

import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopPullRequestHookTriggerHandler implements PullRequestHookTriggerHandler {
    @Override
    public void handle(Job<?, ?> job, PullRequestHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        // nothing to do
    }
}
