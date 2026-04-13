package com.gitee.jenkins.connection;

import org.kohsuke.stapler.DataBoundConstructor;

public class GiteeApiRepo {
    private String repoName;
    private String owner;

    @DataBoundConstructor
    public GiteeApiRepo(String repoOwnerString) throws IllegalStateException {
        if (repoOwnerString != null && repoOwnerString.length() > 1) {
            String[] arr = repoOwnerString.split(" ");
            repoName = arr[0];
            owner = arr[1];
        } else {
            throw new IllegalStateException("repoOwnerString is not allow to be empty or null.");
        }
    }

    public String getRepoName() {
        return repoName;
    }

    public String getOwner() {
        return owner;
    }
}
