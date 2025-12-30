package com.gitee.jenkins.trigger.filter;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * @author Robin Müller
 */
public class PullRequestLabelFilterConfig {

    private String include;
    private String exclude;

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

    public static class PullRequestLabelFilterConfigBuilder {
        private String includeLabelSpec;
        private String excludeLabelSpec;

        public static PullRequestLabelFilterConfigBuilder pullRequestFilterConfig() {
            return new PullRequestLabelFilterConfigBuilder();
        }

        public PullRequestLabelFilterConfigBuilder withIncludeBranchesSpec(String includeLabelSpec) {
            this.includeLabelSpec = includeLabelSpec;
            return this;
        }

        public PullRequestLabelFilterConfigBuilder withExcludeBranchesSpec(String excludeLabelSpec) {
            this.excludeLabelSpec = excludeLabelSpec;
            return this;
        }

        public PullRequestLabelFilterConfig build() {
            return new PullRequestLabelFilterConfig(includeLabelSpec, excludeLabelSpec);
        }
    }
}
