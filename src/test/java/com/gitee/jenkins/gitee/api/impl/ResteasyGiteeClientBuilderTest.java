/**
 * Adapted from GitLab automated tests
 * https://github.com/jenkinsci/gitlab-plugin/tree/master/src/test/java/com/dabsquared/gitlabjenkins
 */

package com.gitee.jenkins.gitee.api.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.instanceOf;
import java.lang.reflect.Field;

import hudson.ProxyConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.GiteeClientBuilder;

@WithJenkins
class ResteasyGiteeClientBuilderTest {
    static final String API_TOKEN = "secret";
    static final String API_TOKEN_ID = "apiTokenId";
    private static final boolean IGNORE_CERTIFICATE_ERRORS = true;
    private static final int CONNECTION_TIMEOUT = 10;
    private static final int READ_TIMEOUT = 10;

    private JenkinsRule jenkins;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @Test
    void buildClient() throws Exception {
        GiteeClientBuilder clientBuilder = new ResteasyGiteeClientBuilder("test", 0, GiteeV5ApiProxy.class, null);
        assertApiImpl(buildClientWithDefaults(clientBuilder, "http://localhost/"), GiteeV5ApiProxy.class);
    }

    @Test
    void buildClientWithProxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("example.com", 8080, "test", "test", "*localhost*");
        GiteeClientBuilder clientBuilder = new ResteasyGiteeClientBuilder("test", 0, GiteeV5ApiProxy.class, null);
        assertNotNull(buildClientWithDefaults(clientBuilder, "http://localhost"));
    }

    static void addGiteeApiToken() throws Exception {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
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

    static GiteeClient buildClientWithDefaults(GiteeClientBuilder clientBuilder, String url) {
        return clientBuilder.buildClient(
                url,
                API_TOKEN_ID,
                IGNORE_CERTIFICATE_ERRORS,
                CONNECTION_TIMEOUT,
                READ_TIMEOUT);
    }

    static void assertApiImpl(GiteeClient client, Class<? extends GiteeApiProxy> apiImplClass) throws Exception {
        Field apiField = ((ResteasyGiteeClient) client).getClass().getDeclaredField("api");
        apiField.setAccessible(true);
        assertThat(apiField.get(client), instanceOf(apiImplClass));
    }
}
