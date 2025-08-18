/**
 * Adapted from GitLab automated tests
 * https://github.com/jenkinsci/gitlab-plugin/tree/master/src/test/java/com/dabsquared/gitlabjenkins
 */

package com.gitee.jenkins.connection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.List;

import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.impl.GiteeV5ClientBuilder;

import hudson.util.Secret;
import jenkins.model.Jenkins;

@WithJenkins
class GiteeConnectionTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final String API_TOKEN_ID_NOT_STORED = "apiTokenNotStored";

    private static JenkinsRule jenkins;
    
    @BeforeAll
    static void setUp(JenkinsRule rule) throws IOException {
        jenkins = rule;
        for (final CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.get())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                final List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "Gitee API Token",
                                Secret.fromString(API_TOKEN)));
            }
        }
    }

    @Test
    void doGetClientSuccessTest() {
        GiteeConnection connection = new GiteeConnection(
                "test", "http://localhost", API_TOKEN_ID, new GiteeV5ClientBuilder(), false, 10, 10); 

        assertThat(API_TOKEN_ID, is(equalTo(connection.getApiTokenId())));
        GiteeClient client = connection.getClient();
        assertThat(client, notNullValue());
    }

    @Test
    void doGetClientIdNotStoredTest() {
        GiteeConnection connection = new GiteeConnection(
                "test", "http://localhost", API_TOKEN_ID_NOT_STORED, new GiteeV5ClientBuilder(), false, 10, 10);
        
        assertThat(connection.getApiTokenId(), equalTo(API_TOKEN_ID_NOT_STORED));
        assertThrows(IllegalStateException.class, connection::getClient);
    }
}
