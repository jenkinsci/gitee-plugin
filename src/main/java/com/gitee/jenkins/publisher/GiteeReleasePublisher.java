package com.gitee.jenkins.publisher;

import java.io.IOException;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.Release;
import com.gitee.jenkins.gitee.api.model.builder.generated.ReleaseBuilder;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.triggers.SCMTriggerItem;

import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;

public class GiteeReleasePublisher extends Notifier implements MatrixAggregatable {

    private String owner;
    private String repo;
    private String tagName;
    private String name;
    private String body;
    private boolean prerelease;
    private String targetCommit;

    @DataBoundConstructor
    public GiteeReleasePublisher() {
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public String getTagName() {
        return tagName;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public boolean isPrerelease() {
        return prerelease;
    }

    public String getTargetCommit() {
        return targetCommit;
    }

    @DataBoundSetter
    public void setOwner(String owner) {
        this.owner = owner;
    }

    @DataBoundSetter
    public void setRepo(String repo) {
        this.repo = repo;
    }

    @DataBoundSetter
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    @DataBoundSetter
    public void setName(String name) {
        this.name = name;
    }

    @DataBoundSetter
    public void setPrerelease(boolean prerelease) {
        this.prerelease = prerelease;
    }

    @DataBoundSetter
    public void setTargetCommit(String targetCommit) {
        this.targetCommit = targetCommit;
    }

    @DataBoundSetter
    public void setBody(String body) {
        this.body = body;
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

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        GiteeClient client = getClient(build);
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(build.getProject());
        String commitHash = null;
        for (SCM scm : item.getSCMs()) {
            if (scm instanceof GitSCM gitSCM) {
                BuildData data = gitSCM.getBuildData(build);
                if (data != null && data.getLastBuiltRevision() != null) {
                    commitHash = data.getLastBuiltRevision().getSha1String();
                }

            }
        }
        Release release = new ReleaseBuilder()
                .withTagName(tagName)
                .withName(name)
                .withBody(body)
                .withPrerelease(prerelease)
                .withTargetCommitish(commitHash)
                .build();

        Release releaseResponse = client.createRelease(owner, repo, release);

        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GiteeReleasePublisher_DisplayName();
        }
    }
}
