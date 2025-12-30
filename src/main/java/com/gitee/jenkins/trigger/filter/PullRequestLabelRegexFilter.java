package com.gitee.jenkins.trigger.filter;

import java.util.Collection;

import org.apache.commons.lang.StringUtils;

public class PullRequestLabelRegexFilter implements PullRequestLabelFilter {
    private final String regex;

    public PullRequestLabelRegexFilter(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean isPullRequestAllowed(Collection<String> labels) {
        if (labels.isEmpty() || StringUtils.isEmpty(regex)) {
            return true;
        }
        for (String label: labels) {
            if (label.matches(regex)) {
                return true;
            }
        }
        return false;
    }
}
