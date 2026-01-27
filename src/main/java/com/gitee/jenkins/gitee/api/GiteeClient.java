package com.gitee.jenkins.gitee.api;

import java.util.List;

import com.gitee.jenkins.gitee.api.model.*;

import jenkins.util.VirtualFile;

public interface GiteeClient {
    String getHostUrl();

    void acceptPullRequest(PullRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createPullRequestNote(PullRequest mr, String body);

    User getCurrentUser();

    void createPullRequest(PullRequest pr, boolean pruneSourceBranch, boolean draft, boolean squash);
    
    WebHook createWebHook(String owner, String repo, WebHook hook);

    List<WebHook> getWebHooks(String owner, String repo);

    List<Label> getLabels(String owner, String repo);

    List<PullRequest> getPullRequest(PullRequest pr);

    Release getLatestRelease(String owner, String repo);

    Release createRelease(String owner, String repo, Release release);

    void attachFileToRelease(String owner, String repo, Integer releaseId, String filename, VirtualFile file);

}
