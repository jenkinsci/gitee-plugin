package com.dabsquared.gitlabjenkins.gitlab.api;

import com.dabsquared.gitlabjenkins.gitlab.api.model.*;
import com.dabsquared.gitlabjenkins.gitlab.hook.model.State;

import java.util.List;

public interface GitLabClient {
    String getHostUrl();

    void acceptMergeRequest(MergeRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch);

    void createMergeRequestNote(MergeRequest mr, String body);

    User getCurrentUser();


}
