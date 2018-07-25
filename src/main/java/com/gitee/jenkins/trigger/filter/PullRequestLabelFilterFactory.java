package com.gitee.jenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public class PullRequestLabelFilterFactory {

    private PullRequestLabelFilterFactory() { }

    public static PullRequestLabelFilter newPullRequestLabelFilter(PullRequestLabelFilterConfig config) {
        if (config == null) {
            return new NopPullRequestLabelFilter();
        } else {
            return new PullRequestLabelFilterImpl(config.getInclude(), config.getExclude());
        }
    }
}
