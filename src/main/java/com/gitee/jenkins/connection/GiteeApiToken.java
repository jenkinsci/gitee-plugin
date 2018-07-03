package com.gitee.jenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import hudson.Util;
import hudson.util.Secret;

/**
 * @author Robin Müller
 */
@NameWith(GiteeApiToken.NameProvider.class)
public interface GiteeApiToken extends StandardCredentials {

    Secret getApiToken();

    class NameProvider extends CredentialsNameProvider<GiteeApiToken> {
        @Override
        public String getName(GiteeApiToken c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return Messages.GiteeApiToken_name() + (description != null ? " (" + description + ")" : "");
        }
    }
}
