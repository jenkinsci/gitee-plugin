package com.gitee.jenkins.environment;

import com.gitee.jenkins.cause.GiteeWebHookCause;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
import hudson.Extension;
import hudson.matrix.MatrixRun;
import hudson.matrix.MatrixBuild;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;

/**
 * @author Robin Müller
 */
@Extension
public class GiteeEnvironmentContributor extends EnvironmentContributor {
    @Override
    public void buildEnvironmentFor(@NonNull Run r, @NonNull EnvVars envs, @NonNull TaskListener listener) throws IOException, InterruptedException {
        GiteeWebHookCause cause = null;
        if (r instanceof MatrixRun run) {
            MatrixBuild parent = run.getParentBuild();
            if (parent != null) {
                cause = parent.getCause(GiteeWebHookCause.class);
            }
        } else {
            cause = (GiteeWebHookCause) r.getCause(GiteeWebHookCause.class);
        }
        if (cause != null) {
            envs.overrideAll(cause.getData().getBuildVariables());
        }
    }
}
