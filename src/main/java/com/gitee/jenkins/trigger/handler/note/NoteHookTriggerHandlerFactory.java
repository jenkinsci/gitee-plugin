package com.gitee.jenkins.trigger.handler.note;

/**
 * @author Nikolay Ustinov
 */
public final class NoteHookTriggerHandlerFactory {

    private NoteHookTriggerHandlerFactory() {}

    public static NoteHookTriggerHandler newNoteHookTriggerHandler(boolean triggerOnCommitComment, boolean triggerOnNoteRequest, String noteRegex, boolean ciSkipFroTestNotRequired, boolean cancelIncompleteBuildOnSamePullRequest, boolean ignorePullRequestConflicts) {
        if (triggerOnCommitComment || triggerOnNoteRequest) {
            return new NoteHookTriggerHandlerImpl(triggerOnCommitComment, triggerOnNoteRequest, noteRegex, ciSkipFroTestNotRequired, cancelIncompleteBuildOnSamePullRequest, ignorePullRequestConflicts);
        } else {
            return new NopNoteHookTriggerHandler();
        }
    }
}
