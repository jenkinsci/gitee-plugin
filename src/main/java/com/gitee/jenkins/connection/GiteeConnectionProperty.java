package com.gitee.jenkins.connection;


import com.gitee.jenkins.gitee.api.GiteeClient;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import java.lang.StackWalker.Option;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * @author Robin Müller
 */
public class GiteeConnectionProperty extends JobProperty<Job<?, ?>> {

    private String giteeConnection;

    @DataBoundConstructor
    public GiteeConnectionProperty(String giteeConnection) {
        this.giteeConnection = giteeConnection;
    }

    public String getGiteeConnection() {
        return giteeConnection;
    }

    public Optional<GiteeClient> getClient() {
        if (StringUtils.isNotEmpty(giteeConnection)) {
            Optional<GiteeConnectionConfig> opt = Optional.ofNullable((GiteeConnectionConfig) Jenkins.get().getDescriptor(GiteeConnectionConfig.class));
            return opt.map(connectionConfig -> connectionConfig.getClient(giteeConnection));
        }
        return Optional.empty();
    }

    public static Optional<GiteeClient> getClient(Run<?, ?> build) {
        final Optional<GiteeConnectionProperty> opt = Optional.ofNullable(build.getParent().getProperty(GiteeConnectionProperty.class));
        return opt.map(connectionProperty -> connectionProperty.getClient()).orElse(Optional.empty());
    }

    public static Optional<GiteeClient> getClient(Job<?, ?> job) {
        final Optional<GiteeConnectionProperty> opt = Optional.ofNullable(job.getProperty(GiteeConnectionProperty.class));
        return opt.map(connectionProperty -> connectionProperty.getClient()).orElse(Optional.empty());
    }


    @Extension
    @Symbol("giteeConnection")
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Gitee connection";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            return req.bindJSON(GiteeConnectionProperty.class, formData);
        }

        @RequirePOST
        public ListBoxModel doFillGiteeConnectionItems() {
            if (Jenkins.get().hasPermission(Item.CONFIGURE)) {
                ListBoxModel options = new ListBoxModel();
                GiteeConnectionConfig descriptor = (GiteeConnectionConfig) Jenkins.get().getDescriptor(GiteeConnectionConfig.class);
                for (GiteeConnection connection : descriptor.getConnections()) {
                    options.add(connection.getName(), connection.getName());
                }
                return options;
            } else {
                return null;
            }
        }
    }
}
