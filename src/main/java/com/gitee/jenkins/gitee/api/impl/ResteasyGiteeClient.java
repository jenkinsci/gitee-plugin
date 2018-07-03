package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.*;
import com.google.common.base.Function;


final class ResteasyGiteeClient implements GiteeClient {
    private final String hostUrl;
    private final GiteeApiProxy api;
    private final Function<MergeRequest, Integer> mergeRequestIdProvider;

    ResteasyGiteeClient(String hostUrl, GiteeApiProxy api, Function<MergeRequest, Integer> mergeRequestIdProvider) {
        this.hostUrl = hostUrl;
        this.api = api;
        this.mergeRequestIdProvider = mergeRequestIdProvider;
    }

    @Override
    public final String getHostUrl() {
        return hostUrl;
    }

    // Gitee v5 don't support commit message and remove source branch
    @Override
    public void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {
        api.acceptMergeRequest(mr.getRepoOwner(), mr.getRepoPath(), mergeRequestIdProvider.apply(mr));
    }

    @Override
    public void createMergeRequestNote(MergeRequest mr, String body) {
        api.createMergeRequestNote(mr.getRepoOwner(), mr.getRepoPath(), mergeRequestIdProvider.apply(mr), body);
    }

    @Override
    public User getCurrentUser() {
        return api.getCurrentUser();
    }
}
