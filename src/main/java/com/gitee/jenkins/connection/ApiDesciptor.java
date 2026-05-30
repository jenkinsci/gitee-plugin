package com.gitee.jenkins.connection;

import org.kohsuke.stapler.interceptor.RequirePOST;

import hudson.security.Permission;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public interface ApiDesciptor {

    @RequirePOST
    default ListBoxModel doFillGiteeApiRepoItems() {
        if (Jenkins.get().hasPermission(Permission.CONFIGURE)) {
            GiteeApiRepoProperty.DescriptorImpl descriptor = (com.gitee.jenkins.connection.GiteeApiRepoProperty.DescriptorImpl) Jenkins
                    .get().getJobProperty("GiteeApiRepoProperty");

            return descriptor.doFillGiteeApiRepoItems();
        } else {
            return null;
        }
    }

}
