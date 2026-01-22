package com.gitee.jenkins.gitee.api.impl;

import java.util.List;

import com.gitee.jenkins.gitee.api.model.*;

interface GiteeApiProxy {
    void createPullRequestNote(String owner, String repo, Integer pullRequestId, String body);

    void headCurrentUser();

    void acceptPullRequest(String owner, String repo, Integer pullRequestId);

    void createPullRequest(String owner, String repo, String title, String base, String head, String body,
            Boolean pruneSourceBranch, Boolean draft, Boolean squash);

    WebHook createWebHook(String owner, String repo, String url, String title, Integer encryptionType,
            Boolean pushEvents, Boolean tagPushEvents, Boolean issuesEvents, Boolean noteEvents,
            Boolean mergeRequestEvents);

    List<WebHook> getWebHooks(String owner, String repo);

    List<PullRequest> getPullRequest(String owner, String repo, String base, String head);

    User getCurrentUser();

    List<Label> getLabels(String owner, String repo);

    Release createRelease(String owner, String repo, String tagName, String name, String body, Boolean prerelease,
            String targetCommit);
}
