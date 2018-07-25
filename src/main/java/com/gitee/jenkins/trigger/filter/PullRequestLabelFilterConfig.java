package com.gitee.jenkins.trigger.filter;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Robin Müller
 */
public class PullRequestLabelFilterConfig {

    private String include;
    private String exclude;

    /**
     * @deprecated use {@link #PullRequestLabelFilterConfig()} with setters to configure an instance of this class.
     */
    @Deprecated
    public PullRequestLabelFilterConfig(String include, String exclude) {
        this.include = include;
        this.exclude = exclude;
    }

    @DataBoundConstructor
    public PullRequestLabelFilterConfig() { }

    public String getInclude() {
        return include;
    }

    public String getExclude() {
        return exclude;
    }

    @DataBoundSetter
    public void setInclude(String include) {
        this.include = include;
    }

    @DataBoundSetter
    public void setExclude(String exclude) {
        this.exclude = exclude;
    }
}
