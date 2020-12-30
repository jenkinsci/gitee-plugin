package com.gitee.jenkins.trigger.handler.pull;

import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.ActionDesc;
import com.gitee.jenkins.gitee.hook.model.State;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class PullRequestHookTriggerHandlerFactory {

    private PullRequestHookTriggerHandlerFactory() {}

    public static PullRequestHookTriggerHandler newPullRequestHookTriggerHandler(boolean triggerOnOpenPullRequest,
                                                                                 String triggerOnUpdatePullRequest,
                                                                                 boolean triggerOnAcceptedPullRequest,
                                                                                 boolean triggerOnClosedPullRequest,
                                                                                 boolean skipWorkInProgressPullRequest,
                                                                                 boolean triggerOnApprovedPullRequest,
                                                                                 boolean triggerOnTestedPullRequest,
                                                                                 boolean cancelPendingBuildsOnUpdate,
                                                                                 boolean ciSkipFroTestNotRequired,
                                                                                 boolean cancelIncompleteBuildOnSamePullRequest) {
        if (triggerOnOpenPullRequest
            || !("0".equals(triggerOnUpdatePullRequest) || "false".equals(triggerOnUpdatePullRequest))
            || triggerOnAcceptedPullRequest
            || triggerOnClosedPullRequest
            || triggerOnApprovedPullRequest
            || triggerOnTestedPullRequest) {

        	return new PullRequestHookTriggerHandlerImpl(
        	    retrieveAllowedStates(triggerOnOpenPullRequest,
                    triggerOnUpdatePullRequest,
                    triggerOnAcceptedPullRequest,
                    triggerOnClosedPullRequest,
                    triggerOnApprovedPullRequest,
                    triggerOnTestedPullRequest),
                retrieveAllowedActions(triggerOnOpenPullRequest,
                    triggerOnUpdatePullRequest,
                    triggerOnAcceptedPullRequest,
                    triggerOnClosedPullRequest,
                    triggerOnApprovedPullRequest,
                    triggerOnTestedPullRequest),
                retrieveAllowedActionDesces(triggerOnUpdatePullRequest),
                skipWorkInProgressPullRequest,
                cancelPendingBuildsOnUpdate,
                ciSkipFroTestNotRequired,
                cancelIncompleteBuildOnSamePullRequest);
        } else {
            return new NopPullRequestHookTriggerHandler();
        }
    }


    private static List<ActionDesc> retrieveAllowedActionDesces(String triggerOnUpdatePullRequest) {
        List<ActionDesc> allowedActionDesces =new ArrayList<>();

        switch(triggerOnUpdatePullRequest){
            case "1":
                allowedActionDesces.add(ActionDesc.source_branch_changed);
                break;
            case "2":
                allowedActionDesces.add(ActionDesc.target_branch_changed);
                break;
            case "3":
                allowedActionDesces.add(ActionDesc.source_branch_changed);
                allowedActionDesces.add(ActionDesc.target_branch_changed);
                break;
            case "true":
                allowedActionDesces.add(ActionDesc.source_branch_changed);
                allowedActionDesces.add(ActionDesc.target_branch_changed);
                break;
        }
        return allowedActionDesces;
    }


    private static List<Action> retrieveAllowedActions(boolean triggerOnOpenPullRequest,
                                                       String triggerOnUpdatePullRequest,
                                                       boolean triggerOnAcceptedPullRequest,
                                                       boolean triggerOnClosedPullRequest,
                                                       boolean triggerOnApprovedPullRequest,
                                                       boolean triggerOnTestedPullRequest) {
        List<Action> allowedActions =new ArrayList<>();

        if (triggerOnOpenPullRequest) {
            allowedActions.add(Action.open);
        }

        if (!("0".equals(triggerOnUpdatePullRequest) || "false".equals(triggerOnUpdatePullRequest))) {
            allowedActions.add(Action.update);
        }

		if (triggerOnAcceptedPullRequest) {
            allowedActions.add(Action.merge);
        }

        if (triggerOnClosedPullRequest) {
            allowedActions.add(Action.close);
        }

        if (triggerOnApprovedPullRequest) {
            allowedActions.add(Action.approved);
        }

        if (triggerOnTestedPullRequest) {
            allowedActions.add(Action.tested);
        }

		return allowedActions;
	}

	private static List<State> retrieveAllowedStates(boolean triggerOnOpenPullRequest,
                                                     String triggerOnUpdatePullRequest,
                                                     boolean triggerOnAcceptedPullRequest,
                                                     boolean triggerOnClosedPullRequest,
                                                     boolean triggerOnApprovedPullRequest,
                                                     boolean triggerOnTestedPullRequest) {
        List<State> result = new ArrayList<>();
        if (triggerOnOpenPullRequest
            || !("0".equals(triggerOnUpdatePullRequest) || "false".equals(triggerOnUpdatePullRequest))
            || triggerOnApprovedPullRequest
            || triggerOnTestedPullRequest) {

            result.add(State.opened);
            result.add(State.open);
            result.add(State.reopened);
            result.add(State.updated);
        }

        if (triggerOnAcceptedPullRequest)  {
        	result.add(State.merged);
        }
        if (triggerOnClosedPullRequest) {
        	result.add(State.closed);
        }

        return result;
    }
}
