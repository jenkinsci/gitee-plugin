package com.gitee.jenkins.trigger.filter;

import java.util.Collection;

/**
 * @author Robin Müller
 */
public interface PullRequestLabelFilter {
    boolean isPullRequestAllowed(Collection<String> labels);
}
