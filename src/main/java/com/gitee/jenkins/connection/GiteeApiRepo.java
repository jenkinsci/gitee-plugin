package com.gitee.jenkins.connection;

import org.kohsuke.stapler.DataBoundConstructor;

public class GiteeApiRepo {
    private String repo;
    private String owner;

    @DataBoundConstructor
    public GiteeApiRepo(String repoOwnerString) throws IllegalStateException {
        if (repoOwnerString != null && repoOwnerString.length() > 1) {
            String[] arr = repoOwnerString.split(" ");
            repo = arr[0];
            owner = arr[1];
        } else {
            throw new IllegalStateException("repoOwnerString is not allow to be empty or null.");
        }
    }

    public String getRepo() {
        return repo;
    }

    public String getOwner() {
        return owner;
    }
}
