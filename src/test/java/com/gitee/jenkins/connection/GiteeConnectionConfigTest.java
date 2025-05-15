/**
 * Adapted from GitLab automated tests
 * https://github.com/jenkinsci/gitlab-plugin/tree/master/src/test/java/com/dabsquared/gitlabjenkins
 */

package com.gitee.jenkins.connection;

import static com.gitee.jenkins.connection.Messages.connection_error;
import static com.gitee.jenkins.connection.Messages.connection_success;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;

import hudson.ProxyConfiguration;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jakarta.ws.rs.core.Response;
import jenkins.model.Jenkins;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;

@WithJenkins
@ExtendWith(MockServerExtension.class)
public class GiteeConnectionConfigTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    
    private JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private String giteeUrl;

    @BeforeAll
    static void setUp(MockServerClient client) {
        mockServerClient = client;
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        giteeUrl = "http://localhost:" + mockServerClient.getPort();
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new GiteeApiTokenImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "Gitee API Token",
                                Secret.fromString(API_TOKEN)));
            }
        }
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    public void doCheckConnection_success() {
        String expected = connection_success();
        assertThat(doCheckConnection("v5", Response.Status.OK), is(expected));
    }

    @Test
    void doCheckConnection_forbidden() {
        String expected = connection_error("HTTP 403 Forbidden");
        assertThat(doCheckConnection("v5", Response.Status.FORBIDDEN), is(expected));
    }

    @Test
    void doCheckConnection_proxyUserPass() {
        jenkins.getInstance().proxy = new ProxyConfiguration("0.0.0.0", 80, "testName", "testPass");
        GiteeConnectionConfig descriptor = (GiteeConnectionConfig) jenkins.jenkins.getDescriptor(GiteeConnectionConfig.class);
        FormValidation result =
                descriptor.doTestConnection(giteeUrl, API_TOKEN_ID, "v5", false, 10, 10);
        assertThat(result.getMessage(), containsString("Connection refused"));
    }

    private String doCheckConnection(String clientBuilderId, Response.Status status) {
        HttpRequest request =
            request().withPath("/api/" + clientBuilderId + "/user").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(status.getStatusCode()).withContentType(MediaType.APPLICATION_JSON));

        GiteeConnectionConfig descriptor = (GiteeConnectionConfig) jenkins.jenkins.getDescriptor(GiteeConnectionConfig.class);
        FormValidation formValidation =
                descriptor.doTestConnection(giteeUrl, API_TOKEN_ID, clientBuilderId, false, 10, 10);
        mockServerClient.verify(request);
        return formValidation.getMessage();
    }
}
