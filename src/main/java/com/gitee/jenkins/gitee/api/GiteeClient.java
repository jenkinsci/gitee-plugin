package com.gitee.jenkins.gitee.api;

import java.util.List;

import com.gitee.jenkins.gitee.api.model.*;

public interface GiteeClient {
    String getHostUrl();

    void acceptPullRequest(PullRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createPullRequestNote(PullRequest mr, String body);

    User getCurrentUser();

    List<RepoUser> getRepositoryUsers(Repo repo);

}
