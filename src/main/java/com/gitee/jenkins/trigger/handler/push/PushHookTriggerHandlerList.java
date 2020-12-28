package com.gitee.jenkins.trigger.handler.push;

import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import hudson.model.Job;

import java.util.List;

/**
 * @author Robin Müller
 */
class PushHookTriggerHandlerList implements PushHookTriggerHandler {

    private final List<PushHookTriggerHandler> handlers;

    PushHookTriggerHandlerList(List<PushHookTriggerHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void handle(Job<?, ?> job, PushHook hook, BuildInstructionFilter buildInstructionFilter, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        for (PushHookTriggerHandler handler : handlers) {
            handler.handle(job, hook, buildInstructionFilter, skipLastCommitHasBeenBuild, branchFilter, pullRequestLabelFilter);
        }
    }
}
