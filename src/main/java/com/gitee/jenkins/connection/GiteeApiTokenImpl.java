package com.gitee.jenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.Secret;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * @author Robin Müller
 */
public final class GiteeApiTokenImpl extends BaseStandardCredentials implements GiteeApiToken {

    private Secret apiToken;

    @DataBoundConstructor
    public GiteeApiTokenImpl(CredentialsScope scope, String id, String description, Secret apiToken) {
        super(scope, id, description);
        this.apiToken = apiToken;
    }

    @Override
    public Secret getApiToken() {
        return apiToken;
    }

    @Extension
    public static class DescriptorImpl extends BaseStandardCredentialsDescriptor {
        @Override
        public String getDisplayName() {
            return Messages.GiteeApiToken_name();
        }
    }
}
