package com.gitee.jenkins.trigger.handler.note;

/**
 * @author Nikolay Ustinov
 */
public final class NoteHookTriggerHandlerFactory {

    private NoteHookTriggerHandlerFactory() {}

    public static NoteHookTriggerHandler newNoteHookTriggerHandler(boolean triggerOnNoteRequest, String noteRegex, boolean ciSkipFroTestNotRequired, boolean cancelIncompleteBuildOnSamePullRequest) {
        if (triggerOnNoteRequest) {
            return new NoteHookTriggerHandlerImpl(noteRegex, ciSkipFroTestNotRequired, cancelIncompleteBuildOnSamePullRequest);
        } else {
            return new NopNoteHookTriggerHandler();
        }
    }
}
