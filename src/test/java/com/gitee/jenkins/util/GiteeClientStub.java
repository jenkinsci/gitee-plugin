package com.gitee.jenkins.util;

import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.gitee.api.model.User;

class GiteeClientStub implements GiteeClient {
    private final String url;

    GiteeClientStub(String url) {
        this.url = url;
    }

    @Override
    public String getHostUrl() {
        return url;
    }

    @Override
    public void acceptPullRequest(PullRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {}

    @Override
    public void createPullRequestNote(PullRequest mr, String body) {}

    @Override
    public User getCurrentUser() {
        return null;
    }
}

