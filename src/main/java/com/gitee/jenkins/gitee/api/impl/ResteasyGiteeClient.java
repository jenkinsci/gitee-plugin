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
    public void createPullRequest(PullRequest pr, boolean pruneSourceBranch, boolean isDraft, boolean isSquashMerge) {
        api.createPullRequest(pr.getRepoOwner(), pr.getRepoPath(), pr.getTitle(), pr.getTargetBranch(),
                pr.getSourceBranch(), pr.getDescription(), pruneSourceBranch, isDraft, isSquashMerge);
    }

    @Override
    public List<PullRequest> getPullRequest(PullRequest pr) {
        return api.getPullRequest(pr.getRepoOwner(), pr.getRepoPath(), pr.getTargetBranch(), pr.getSourceBranch());
    }

    @Override
    public User getCurrentUser() {
        return api.getCurrentUser();
    }

    @Override
    public List<Label> getLabels(String owner, String repo) {
        return api.getLabels(owner, repo);
    }

    @Override
    public WebHook createWebHook(String owner, String repo, WebHook hook) {
        return api.createWebHook(owner, repo, hook.getUrl(), hook.getTitle(), hook.getEncryptionType(),
                hook.getPushEvents(), hook.getTagPushEvents(), hook.getIssuesEvents(), hook.getNoteEvents(),
                hook.getMergeRequestsEvents());
    }

    @Override
    public List<WebHook> getWebHooks(String owner, String repo) {
        return api.getWebHooks(owner, repo);
    }
}
