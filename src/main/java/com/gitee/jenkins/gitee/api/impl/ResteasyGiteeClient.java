package com.gitee.jenkins.gitee.api.impl;


import java.util.List;

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
    public String getHostUrl() {
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
    public void createPullRequest(PullRequest pr) {
        api.createPullRequest(pr.getRepoOwner(), pr.getRepoPath(), pr.getTitle(), pr.getTargetBranch(), pr.getSourceBranch(), pr.getDescription());
    }

    @Override
    public List<PullRequest> getPullRequest(PullRequest pr) {
        return api.getPullRequest(pr.getRepoOwner(), pr.getRepoPath(), pr.getTargetBranch(), pr.getSourceBranch());
    }

    @Override
    public User getCurrentUser() {
        return api.getCurrentUser();
    }
}
