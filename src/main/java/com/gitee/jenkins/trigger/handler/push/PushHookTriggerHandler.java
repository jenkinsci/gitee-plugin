package com.gitee.jenkins.trigger.handler.push;

import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.trigger.handler.WebHookTriggerHandler;

/**
 * @author Robin Müller
 */
public interface PushHookTriggerHandler extends WebHookTriggerHandler<PushHook> { }
