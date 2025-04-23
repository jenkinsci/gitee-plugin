package com.gitee.jenkins.workflow;

import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.*;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class AcceptGiteePullRequestStep extends Step {

    private static final Logger LOGGER = Logger.getLogger(AcceptGiteePullRequestStep.class.getName());

    private String mergeCommitMessage;

    @DataBoundConstructor
    public AcceptGiteePullRequestStep(String mergeCommitMessage) {
        this.mergeCommitMessage = StringUtils.isEmpty(mergeCommitMessage) ? null : mergeCommitMessage;
    }

	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new AcceptGiteePullRequestStepExecution(context, this);
	}
	
    public String getMergeCommitMessage() {
        return mergeCommitMessage;
    }

    @DataBoundSetter
    public void setMergeCommitMessage(String mergeCommitMessage) {
        this.mergeCommitMessage = StringUtils.isEmpty(mergeCommitMessage) ? null : mergeCommitMessage;
    }

    public static class AcceptGiteePullRequestStepExecution extends AbstractSynchronousStepExecution<Void> {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient AcceptGiteePullRequestStep step;

        AcceptGiteePullRequestStepExecution(StepContext context, AcceptGiteePullRequestStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }
        
        @Override
        protected Void run() throws Exception {
            GiteeWebHookCause cause = run.getCause(GiteeWebHookCause.class);
            if (cause != null) {
                PullRequest pullRequest = cause.getData().getPullRequest();
                if (pullRequest != null) {
                    GiteeClient client = getClient(run);
                    if (client == null) {
                        println("No Gitee connection configured");
                    } else {
                        try {
                            client.acceptPullRequest(pullRequest, step.mergeCommitMessage, false);
                        } catch (WebApplicationException | ProcessingException e) {
                            printf("Failed to accept pull request for project '%s': %s%n", pullRequest.getProjectId(), e.getMessage());
                            LOGGER.log(Level.SEVERE, String.format("Failed to accept pull request for project '%s'", pullRequest.getProjectId()), e);
                        }
                    }
                }
            }
            return null;
        }

        private void println(String message) {
            TaskListener listener = getTaskListener();
            if (listener == null) {
                LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", message);
            } else {
                listener.getLogger().println(message);
            }
        }

        private void printf(String message, Object... args) {
            TaskListener listener = getTaskListener();
            if (listener == null) {
                LOGGER.log(Level.FINE, "failed to print message {0} due to null TaskListener", String.format(message, args));
            } else {
                listener.getLogger().printf(message, args);
            }
        }

        private TaskListener getTaskListener() {
            StepContext context = getContext();
            if (!context.isReady()) {
                return null;
            }
            try {
                return context.get(TaskListener.class);
            } catch (Exception x) {
                return null;
            }
        }
    }

    @Extension
    public static final class DescriptorImpl extends StepDescriptor {

        @Override
        public String getDisplayName() {
            return "Accept Gitee Pull Request";
        }

        @Override
        public String getFunctionName() {
            return "acceptGiteeMR";
        }
        
		@Override
		public Set<Class<?>> getRequiredContext() {
			return ImmutableSet.of(TaskListener.class, Run.class);
		}
    }
}
