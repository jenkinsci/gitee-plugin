package com.gitee.jenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsNameProvider;
import com.cloudbees.plugins.credentials.NameWith;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Util;
import hudson.util.Secret;

/**
 * @author Robin Müller
 */
@NameWith(GiteeApiToken.NameProvider.class)
public interface GiteeApiToken extends StandardCredentials {

    Secret getApiToken();

    class NameProvider extends CredentialsNameProvider<GiteeApiToken> {

        @NonNull
        @Override
        public String getName(@NonNull GiteeApiToken c) {
            String description = Util.fixEmptyAndTrim(c.getDescription());
            return Messages.GiteeApiToken_name() + (description != null ? " (" + description + ")" : "");
        }
    }
}
