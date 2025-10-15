package com.gitee.jenkins.gitee.api;

import com.gitee.jenkins.gitee.api.model.*;

public interface GiteeClient {
    String getHostUrl();

    void acceptPullRequest(PullRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createPullRequestNote(PullRequest mr, String body);

    User getCurrentUser();

    void createPullRequest(String owner, String repo, String title, String base, String head);

}
