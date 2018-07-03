package com.gitee.jenkins.gitee.api;

import com.gitee.jenkins.gitee.api.model.*;

public interface GiteeClient {
    String getHostUrl();

    void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createMergeRequestNote(MergeRequest mr, String body);

    User getCurrentUser();


}
