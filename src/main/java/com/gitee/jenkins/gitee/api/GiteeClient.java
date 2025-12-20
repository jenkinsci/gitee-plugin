package com.gitee.jenkins.gitee.api;

import java.util.List;

import com.gitee.jenkins.gitee.api.model.*;

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

}
