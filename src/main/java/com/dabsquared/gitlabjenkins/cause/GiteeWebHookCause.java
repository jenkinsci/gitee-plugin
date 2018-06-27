package com.dabsquared.gitlabjenkins.cause;

import hudson.triggers.SCMTrigger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Robin Müller
 * @author Yashin
 */
public class GiteeWebHookCause extends SCMTrigger.SCMTriggerCause {

    private final CauseData data;

    public GiteeWebHookCause(CauseData data) {
        super("");
        this.data = checkNotNull(data, "data must not be null");
    }

    public CauseData getData() {
        return data;
    }

    @Override
    public String getShortDescription() {
        return data.getShortDescription();
    }
}
