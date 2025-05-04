package com.gitee.jenkins.gitee.api.impl;


import java.util.List;

import com.gitee.jenkins.gitee.api.model.*;



interface GiteeApiProxy {
    void createPullRequestNote(String owner, String repo, Integer pullRequestId, String body);
    void headCurrentUser();
    void acceptPullRequest(String owner, String repo, Integer pullRequestId);
    User getCurrentUser();
    List<RepoUser> getRepositoryUsers(String owner, String repo, String type);
}
