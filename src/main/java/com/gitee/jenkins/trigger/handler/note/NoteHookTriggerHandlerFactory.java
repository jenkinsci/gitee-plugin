package com.gitee.jenkins.trigger.handler.note;

/**
 * @author Nikolay Ustinov
 */
public final class NoteHookTriggerHandlerFactory {

    private NoteHookTriggerHandlerFactory() {}

    public static NoteHookTriggerHandler newNoteHookTriggerHandler(boolean triggerOnNoteRequest, String noteRegex, boolean ciSkipFroTestNotRequired) {
        if (triggerOnNoteRequest) {
            return new NoteHookTriggerHandlerImpl(noteRegex, ciSkipFroTestNotRequired);
        } else {
            return new NopNoteHookTriggerHandler();
        }
    }
}
