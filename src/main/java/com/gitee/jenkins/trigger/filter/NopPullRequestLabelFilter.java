package com.gitee.jenkins.trigger.filter;

import java.util.Collection;

/**
 * @author Robin Müller
 */
class NopPullRequestLabelFilter implements PullRequestLabelFilter {
    @Override
    public boolean isPullRequestAllowed(Collection<String> labels) {
        return true;
    }
}
