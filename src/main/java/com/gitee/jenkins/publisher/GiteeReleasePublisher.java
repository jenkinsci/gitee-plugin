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
import hudson.model.Run;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.util.BuildData;
import hudson.scm.SCM;
import hudson.tasks.ArtifactArchiver;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.triggers.SCMTriggerItem;
import jenkins.util.VirtualFile;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gitee.jenkins.connection.GiteeConnectionProperty.getClient;

public class GiteeReleasePublisher extends Notifier implements MatrixAggregatable {

    private String owner;
    private String repo;
    private String tagName;
    private String name;
    private String body;
    private String targetCommit;
    private boolean prerelease;
    private boolean artifacts;
    private boolean increment;

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

    public boolean isArtifacts() {
        return artifacts;
    }

    public boolean isIncrement() {
        return increment;
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
    public void setArtifacts(boolean artifacts) {
        this.artifacts = artifacts;
    }

    @DataBoundSetter
    public void setTargetCommit(String targetCommit) {
        this.targetCommit = targetCommit;
    }

    @DataBoundSetter
    public void setBody(String body) {
        this.body = body;
    }

    @DataBoundSetter
    public void setIncrement(boolean increment) {
        this.increment = increment;
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

    private String createIncrementVersionString(GiteeClient client) {
        Pattern incrementRegex = Pattern.compile("(=+|0+|\\++)");
        Matcher incrementMatcher = incrementRegex.matcher(tagName);

        Release latestRelease = client.getLatestRelease(owner, repo);
        String latestTag = latestRelease.getTagName();
        Pattern regex = Pattern.compile("(\\d+)");
        Matcher latestMatcher = regex.matcher(latestTag);
        ArrayList<String> newVersion = new ArrayList<String>();

        while (latestMatcher.find() && incrementMatcher.find()) {
            if (incrementMatcher.group().equals("+")) {
                int versionNumberString = Integer.parseInt(latestMatcher.group()) + 1;
                newVersion.add("" + versionNumberString);
            } else if (incrementMatcher.group().equals("=")) {
                newVersion.add(latestMatcher.group());
            } else if (incrementMatcher.group().equals("0")) {
                newVersion.add("0");
            }

        }

        if (!latestMatcher.find() && !incrementMatcher.find()) {
            return String.join(".", newVersion);
        } else {
            return null;
        }
    }

    private void attachFileArtifacts(Integer releaseId, GiteeClient client, AbstractBuild<?,?> build) {
        ArtifactArchiver archiver = build.getProject().getPublishersList().get(ArtifactArchiver.class);
        for (Run<?,?>.Artifact artifact: build.getArtifacts()) {
            VirtualFile file = build.getArtifactManager().root().child(artifact.toString());
            if (archiver.getExcludes() == null || !archiver.getExcludes().contains(file.getName())) {
                client.attachFileToRelease(owner, repo, releaseId, artifact.getFileName(), file);
            }
        }
    }

    private String getCommitHash(AbstractBuild<?, ?> build) {
        SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(build.getProject());
        String commitHash = null;
        for (SCM scm : item.getSCMs()) {
            if (scm instanceof GitSCM gitSCM) {
                // Get first Git repo and use it for commit
                BuildData data = gitSCM.getBuildData(build);
                if (data != null && data.getLastBuiltRevision() != null) {
                    commitHash = data.getLastBuiltRevision().getSha1String();
                }
           }
        }
        return commitHash;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        GiteeClient client = getClient(build);
        
        String incrementedTagName = null;
        String commitHash = getCommitHash(build);
        
        if (commitHash == null) {
            launcher.getListener().getLogger().print("Failed to find commit hash. Check that first repository is configured and is Gitee repo.");
            return false;
        }
        
        if (increment) {
            incrementedTagName = createIncrementVersionString(client);
            if (incrementedTagName == null) {
                launcher.getListener().getLogger().print("Failed to increment. Check version string format.");
                return false;
            }
        }

        Release release = new ReleaseBuilder()
                .withTagName(increment ? incrementedTagName : tagName)
                .withName(name)
                .withBody(body)
                .withPrerelease(prerelease)
                .withTargetCommitish(commitHash)
                .build();
        
        Release releaseResponse = client.createRelease(owner, repo, release);
        
        if (artifacts) {
            attachFileArtifacts(releaseResponse.getId(), client, build);
        }
        
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
