package com.gitee.jenkins.trigger.handler.merge;

import com.gitee.jenkins.gitee.hook.model.MergeRequestHook;
import com.gitee.jenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface MergeRequestHookTriggerHandler extends WebHookTriggerHandler<MergeRequestHook> { }
