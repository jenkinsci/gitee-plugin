package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.GitLabClient;
import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;
import com.google.common.base.Function;

import java.util.List;


final class ResteasyGiteeClient implements GitLabClient {
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



    @Override
    public void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {
        api.acceptMergeRequest(mr.getProjectId(), mergeRequestIdProvider.apply(mr), mergeCommitMessage, shouldRemoveSourceBranch);
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
