package com.dabsquared.gitlabjenkins.environment;

import com.dabsquared.gitlabjenkins.cause.GiteeWebHookCause;
import hudson.EnvVars;
import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * @author Robin Müller
 */
@Extension
public class GitLabEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@Nonnull Run r, @Nonnull EnvVars envs, @Nonnull TaskListener listener) throws IOException, InterruptedException {
        GiteeWebHookCause cause = null;
        if (r instanceof MatrixRun) {
            MatrixBuild parent = ((MatrixRun)r).getParentBuild();
            if (parent != null) {
                cause = (GiteeWebHookCause) parent.getCause(GiteeWebHookCause.class);
            }
        } else {
            cause = (GiteeWebHookCause) r.getCause(GiteeWebHookCause.class);
        }
        if (cause != null) {
            envs.overrideAll(cause.getData().getBuildVariables());
        }
    }
}
