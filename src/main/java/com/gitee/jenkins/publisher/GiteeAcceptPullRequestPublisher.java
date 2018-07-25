package com.gitee.jenkins.publisher;


import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public class GiteeAcceptPullRequestPublisher extends PullRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GiteeAcceptPullRequestPublisher.class.getName());

    @DataBoundConstructor
    public GiteeAcceptPullRequestPublisher() { }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GiteeAcceptPullRequestPublisher_DisplayName();
        }
    }

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GiteeClient client, PullRequest pullRequest) {
        try {
            if (build.getResult() == Result.SUCCESS) {
                client.acceptPullRequest(pullRequest, "Pull Request accepted by jenkins build success", false);
            }
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to accept pull request for project '%s': %s%n", pullRequest.getProjectId(), e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to accept pull request for project '%s'", pullRequest.getProjectId()), e);
        }
    }
}
