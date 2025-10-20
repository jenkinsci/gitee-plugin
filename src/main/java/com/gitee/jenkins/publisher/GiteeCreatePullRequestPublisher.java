package com.gitee.jenkins.publisher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.util.LoggerUtil;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;

public class GiteeCreatePullRequestPublisher extends Notifier implements MatrixAggregatable {
    private static final Logger LOGGER = Logger.getLogger(GiteeCreatePullRequestPublisher.class.getName());

    private String repo;
    private String owner;
    private String title;
    private String base;
    private String head;
    private boolean addDatetime;

    @DataBoundConstructor
    public GiteeCreatePullRequestPublisher() { }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public String getRepo() {
        return repo;
    }

    public String getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getBase() {
        return base;
    }

    public String getHead() {
        return head;
    }

    public boolean getAddDatetime() {
        return addDatetime;
    }

    @DataBoundSetter
    public void setRepo(String repo) {
        this.repo = repo;
    }

    @DataBoundSetter
    public void setOwner(String owner) {
        this.owner = owner;
    } 

    @DataBoundSetter
    public void setTitle(String title) {
        this.title = title;
    }

    @DataBoundSetter
    public void setBase(String base) {
        this.base = base;
    }

    @DataBoundSetter
    public void setHead(String head) {
        this.head = head;
    }

    @DataBoundSetter
    public void setAddDatetime(boolean addDatetime) {
        this.addDatetime = addDatetime;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {

        GiteeClient client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No Gitee connection configured");
            return true;
        }

        String pullRequestTitle = title;
        if (addDatetime) {
            pullRequestTitle = LocalDateTime.now().toString() + title;
        }
        client.createPullRequest(owner, repo, pullRequestTitle, base, head);

        LOGGER.log(Level.INFO, "Pull request {0} generated, {1} -> {2}", LoggerUtil.toArray(title, head, base));
        return true;
    }

    @Override
    public MatrixAggregator createAggregator(MatrixBuild build, Launcher launcher, BuildListener listener) {
        return new MatrixAggregator(build, launcher, listener) {
            @Override
            public boolean endBuild() throws InterruptedException, IOException {
                perform(build, launcher, listener);
                return super.endBuild();
            }
        };
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            return Messages.GiteeCreatePullRequestPublisher_DisplayName();
        }
    }
}
