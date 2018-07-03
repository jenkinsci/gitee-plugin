package com.gitee.jenkins.workflow;

import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecution;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.export.ExportedBean;

import com.google.common.collect.ImmutableSet;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * @author <a href="mailto:robin.mueller@1und1.de">Robin Müller</a>
 */
@ExportedBean
public class GiteeCommitStatusStep extends Step {

    private String name;

    @DataBoundConstructor
    public GiteeCommitStatusStep(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }
    
	@Override
	public StepExecution start(StepContext context) throws Exception {
		return new GiteeCommitStatusStepExecution(context, this);
	}

    public String getName() {
        return name;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = StringUtils.isEmpty(name) ? null : name;
    }

    public static class GiteeCommitStatusStepExecution extends StepExecution {
        private static final long serialVersionUID = 1;

        private final transient Run<?, ?> run;

        private final transient GiteeCommitStatusStep step;

        private BodyExecution body;

        GiteeCommitStatusStepExecution(StepContext context, GiteeCommitStatusStep step) throws Exception {
            super(context);
            this.step = step;
            run = context.get(Run.class);
        }
        
        @Override
        public boolean start() throws Exception {
            final String name = StringUtils.isEmpty(step.name) ? "jenkins" : step.name;
            body = getContext().newBodyInvoker()
                .withCallback(new BodyExecutionCallback() {
                    @Override
                    public void onStart(StepContext context) {
                        PendingBuildsAction action = run.getAction(PendingBuildsAction.class);
                        if (action != null) {
                            action.startBuild(name);
                        }
                    }

                    @Override
                    public void onSuccess(StepContext context, Object result) {
                        context.onSuccess(result);
                    }

                    @Override
                    public void onFailure(StepContext context, Throwable t) {
                        context.onFailure(t);
                    }
                })
                .start();
            return false;
        }

        @Override
        public void stop(@Nonnull Throwable cause) throws Exception {
            // should be no need to do anything special (but verify in JENKINS-26148)
            if (body != null) {
                body.cancel(cause);
            }
        }

        private TaskListener getTaskListener(StepContext context) {
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
            return "Update the commit status in Gitee depending on the build status";
        }

        @Override
        public String getFunctionName() {
            return "giteeCommitStatus";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

		@Override
		public Set<Class<?>> getRequiredContext() {
			return ImmutableSet.of(TaskListener.class, Run.class);
		}
    }
}
