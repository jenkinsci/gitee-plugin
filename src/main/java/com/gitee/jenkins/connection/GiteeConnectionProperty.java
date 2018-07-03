package com.gitee.jenkins.connection;


import com.gitee.jenkins.gitee.api.GiteeClient;
import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Run;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

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

    public GiteeClient getClient() {
        if (StringUtils.isNotEmpty(giteeConnection)) {
            GiteeConnectionConfig connectionConfig = (GiteeConnectionConfig) Jenkins.getInstance().getDescriptor(GiteeConnectionConfig.class);
            return connectionConfig != null ? connectionConfig.getClient(giteeConnection) : null;
        }
        return null;
    }

    public static GiteeClient getClient(Run<?, ?> build) {
        final GiteeConnectionProperty connectionProperty = build.getParent().getProperty(GiteeConnectionProperty.class);
        if (connectionProperty != null) {
            return connectionProperty.getClient();
        }
        return null;
    }

    @Extension
    @Symbol("giteeConnection")
    public static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Gitee connection";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return true;
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(GiteeConnectionProperty.class, formData);
        }

        public ListBoxModel doFillGiteeConnectionItems() {
            ListBoxModel options = new ListBoxModel();
            GiteeConnectionConfig descriptor = (GiteeConnectionConfig) Jenkins.getInstance().getDescriptor(GiteeConnectionConfig.class);
            for (GiteeConnection connection : descriptor.getConnections()) {
                options.add(connection.getName(), connection.getName());
            }
            return options;
        }
    }
}
