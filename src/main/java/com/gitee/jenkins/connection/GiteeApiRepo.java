package com.gitee.jenkins.connection;

import org.kohsuke.stapler.DataBoundConstructor;

public class GiteeApiRepo {
    private String repo;
    private String owner;

    @DataBoundConstructor
    public GiteeApiRepo(String repoOwnerString) {
        String[] arr = repoOwnerString.split(" ");
        repo = arr[0];
        owner = arr[1];
    }

    public String getRepo() {
        return repo;
    }

    public String getOwner() {
        return owner;
    }
}
