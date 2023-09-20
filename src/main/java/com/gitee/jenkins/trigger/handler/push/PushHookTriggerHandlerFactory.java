package com.gitee.jenkins.trigger.handler.push;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class PushHookTriggerHandlerFactory {

    private PushHookTriggerHandlerFactory() {}

    public static PushHookTriggerHandler newPushHookTriggerHandler(boolean triggerOnPush, boolean skipWorkInProgressPullRequest, boolean ciBuildForDeleteRef) {
        if (triggerOnPush) {
            return new PushHookTriggerHandlerList(retrieveHandlers(triggerOnPush, skipWorkInProgressPullRequest, ciBuildForDeleteRef));
        } else {
            return new NopPushHookTriggerHandler();
        }
    }

    private static List<PushHookTriggerHandler> retrieveHandlers(boolean triggerOnPush, boolean skipWorkInProgressPullRequest, boolean ciBuildForDeleteRef) {
        List<PushHookTriggerHandler> result = new ArrayList<>();
        if (triggerOnPush) {
            result.add(new PushHookTriggerHandlerImpl(skipWorkInProgressPullRequest, ciBuildForDeleteRef));
        }

        return result;
    }
}
