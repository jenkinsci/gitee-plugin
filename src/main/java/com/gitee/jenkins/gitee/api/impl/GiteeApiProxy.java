package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.model.*;



interface GiteeApiProxy {
    void createMergeRequestNote(String owner, String repo, Integer mergeRequestId, String body);
    void headCurrentUser();
    void acceptMergeRequest(String owner, String repo, Integer mergeRequestId);
    User getCurrentUser();
}
