package com.gitee.jenkins.trigger.handler.pull;

import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface PullRequestHookTriggerHandler extends WebHookTriggerHandler<PullRequestHook> { }
