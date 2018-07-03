package com.gitee.jenkins.trigger.handler.pipeline;

import com.gitee.jenkins.gitee.hook.model.PipelineHook;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.MergeRequestLabelFilter;
import hudson.model.Job;

/**
 * @author Milena Zachow
 */
class NopPipelineHookTriggerHandler implements PipelineHookTriggerHandler {

    @Override
    public void handle(Job<?, ?> job, PipelineHook hook, boolean ciSkip, BranchFilter branchFilter, MergeRequestLabelFilter mergeRequestLabelFilter) {

    }
}
