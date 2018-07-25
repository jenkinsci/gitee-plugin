package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.model.*;



interface GiteeApiProxy {
    void createPullRequestNote(String owner, String repo, Integer pullRequestId, String body);
    void headCurrentUser();
    void acceptPullRequest(String owner, String repo, Integer pullRequestId);
    User getCurrentUser();
}
