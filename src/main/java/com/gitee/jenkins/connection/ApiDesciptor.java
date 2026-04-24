package com.gitee.jenkins.connection;

import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public interface ApiDesciptor {

    default ListBoxModel doFillGiteeApiRepoItems() {
        GiteeApiRepoProperty.DescriptorImpl descriptor = (com.gitee.jenkins.connection.GiteeApiRepoProperty.DescriptorImpl) Jenkins
            .get().getJobProperty("GiteeApiRepoProperty");

        return descriptor.doFillGiteeApiRepoItems();
    }
    
}
