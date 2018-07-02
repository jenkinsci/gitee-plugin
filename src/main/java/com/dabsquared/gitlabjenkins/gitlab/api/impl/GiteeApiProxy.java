package com.dabsquared.gitlabjenkins.gitlab.api.impl;


import com.dabsquared.gitlabjenkins.gitlab.api.model.*;



interface GiteeApiProxy {
    void createMergeRequestNote(String owner, String repo, Integer mergeRequestId, String body);
    void headCurrentUser();
    void acceptMergeRequest(Integer projectId, Integer mergeRequestId, String mergeCommitMessage, boolean shouldRemoveSourceBranch);
    User getCurrentUser();
}
