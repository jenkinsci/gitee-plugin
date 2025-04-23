package com.gitee.jenkins.publisher;

import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;

import java.io.IOException;

import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;

/**
 * @author Robin Müller
 */
public abstract class PullRequestNotifier extends Notifier implements MatrixAggregatable {
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        GiteeClient client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No Gitee connection configured");
            return true;
        }

        PullRequest pullRequest = getPullRequest(build);
        if (pullRequest != null) {
            perform(build, listener, client, pullRequest);
        }
        return true;
    }

    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                perform(build, launcher, listener);
                return super.endBuild();
            }
        };
    }

    protected abstract void perform(Run<?, ?> build, TaskListener listener, GiteeClient client, PullRequest pullRequest);

    PullRequest getPullRequest(Run<?, ?> run) {
        GiteeWebHookCause cause = run.getCause(GiteeWebHookCause.class);
        return cause == null ? null : cause.getData().getPullRequest();

    }
}
