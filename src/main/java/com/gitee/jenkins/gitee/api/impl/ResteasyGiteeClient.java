package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.*;
import com.google.common.base.Function;


final class ResteasyGiteeClient implements GiteeClient {
    private final String hostUrl;
    private final GiteeApiProxy api;
    private final Function<PullRequest, Integer> pullRequestIdProvider;


    ResteasyGiteeClient(String hostUrl, GiteeApiProxy api, Function<PullRequest, Integer> pullRequestIdProvider) {
        this.hostUrl = hostUrl;
        this.api = api;
        this.pullRequestIdProvider = pullRequestIdProvider;
    }

    @Override
    public final String getHostUrl() {
        return hostUrl;
    }

    // Gitee v5 don't support commit message and remove source branch
    @Override
    public void acceptPullRequest(PullRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {
        api.acceptPullRequest(mr.getRepoOwner(), mr.getRepoPath(), mr.getIid());
    }

    @Override
    public void createPullRequestNote(PullRequest mr, String body) {
        api.createPullRequestNote(mr.getRepoOwner(), mr.getRepoPath(), mr.getIid(), body);
    }

    @Override
    public User getCurrentUser() {
        return api.getCurrentUser();
    }
}
