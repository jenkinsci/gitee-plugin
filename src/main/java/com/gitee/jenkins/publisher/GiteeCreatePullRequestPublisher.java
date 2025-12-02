package com.gitee.jenkins.publisher;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.Label;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.gitee.api.model.builder.generated.PullRequestBuilder;
import com.gitee.jenkins.util.LoggerUtil;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
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
    private String body;
    private List<LabelNameEntry> labelNames = Collections.<LabelNameEntry>emptyList();
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

    public String getBody() {
        return body;
    }

    public List<LabelNameEntry> getLabelNames() {
        return labelNames;
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

    @DataBoundSetter
    public void setBody(String body) {
        this.body = body;
    }

    @DataBoundSetter
    public void setLabelNames(List<LabelNameEntry> labelNames) {
        this.labelNames = labelNames;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
            throws InterruptedException, IOException {
        
        ArrayList<String> labels = new ArrayList<String>();
        for(LabelNameEntry entry : labelNames) {
            if (!labels.contains(entry.toString())) {
                labels.add(entry.toString());
            }
        }

        GiteeClient client = getClient(build);
        if (client == null) {
            listener.getLogger().println("No Gitee connection configured");
            return true;
        }

        String pullRequestTitle = title;
        if (addDatetime) {
            StringBuilder newTitle = new StringBuilder();
            newTitle.append(LocalDateTime.now().toString());
            newTitle.append(" ");
            newTitle.append(title);
            pullRequestTitle = newTitle.toString();
        }

        if (build.getResult() == Result.SUCCESS) {
            PullRequest pr = PullRequestBuilder.pullRequest()
                    .withRepoOwner(owner)
                    .withRepoPath(repo)
                    .withTitle(pullRequestTitle)
                    .withSourceBranch(head)
                    .withTargetBranch(base)
                    .withDescription(body)
                    .withLabels(labels)
                    .build();
            
            if (!client.getPullRequest(pr).isEmpty()) {
                LOGGER.log(Level.INFO, "Pull request {0} -> {1} already exists", LoggerUtil.toArray(head, base));
                if (launcher != null) {
                    launcher.getListener().getLogger().println("Pull request {0} -> {1} already exists");
                }
                
                return true;
            }

            client.createPullRequest(pr);
            LOGGER.log(Level.INFO, "Pull request {0} generated, {1} -> {2}", LoggerUtil.toArray(title, head, base));
        }
        
        if (build.getResult() == Result.SUCCESS) {
            PullRequest pr = PullRequestBuilder.pullRequest()
                    .withRepoOwner(owner)
                    .withRepoPath(repo)
                    .withTitle(pullRequestTitle)
                    .withSourceBranch(head)
                    .withTargetBranch(base)
                    .withDescription(body)
                    .build();
            
            if (!client.getPullRequest(pr).isEmpty()) {
                LOGGER.log(Level.INFO, "Pull request {0} -> {1} already exists", LoggerUtil.toArray(head, base));
                if (launcher != null) {
                    launcher.getListener().getLogger().println("Pull request {0} -> {1} already exists");
                }
                
                return true;
            }

            client.createPullRequest(pr);
            LOGGER.log(Level.INFO, "Pull request {0} generated, {1} -> {2}", LoggerUtil.toArray(title, head, base));
        }

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

    public static final class LabelNameEntry extends AbstractDescribableImpl<LabelNameEntry> {
        private final String text;

        @DataBoundConstructor public LabelNameEntry(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return text;
        }

        @Extension 
        public static class DescriptorImpl extends Descriptor<LabelNameEntry> {
            @Override 
            public String getDisplayName() {
                return "Label";
            }
        }
    }
}
