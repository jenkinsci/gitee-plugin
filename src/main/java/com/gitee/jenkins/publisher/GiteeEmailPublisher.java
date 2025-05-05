package com.gitee.jenkins.publisher;

import java.io.IOException;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.GiteeClient;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Mailer;
import hudson.tasks.Notifier;

public class GiteeEmailPublisher extends Notifier {
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        GiteeClient client = GiteeConnectionProperty.getClient(build);
        if (client == null) {
            listener.getLogger().println("No Gitee connection configured");
            return true;
        }
        // implement functionality here
        return true;
    }
}
