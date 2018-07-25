package com.gitee.jenkins.trigger.handler.merge;

import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface PullRequestHookTriggerHandler extends WebHookTriggerHandler<PullRequestHook> { }
