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
import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.impl.GiteeV5ClientBuilder;
import com.gitee.jenkins.trigger.GiteePushTrigger;

import hudson.ProxyConfiguration;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jakarta.ws.rs.core.Response;
import jenkins.model.Jenkins;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.matrixauth.AuthorizationType;
import org.jenkinsci.plugins.matrixauth.PermissionEntry;
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

    @Test
    void doCheckConnection_noProxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("0.0.0.0", 80, "", "", "localhost");
        assertThat(doCheckConnection("v5", Response.Status.OK), is(connection_success()));
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
    
    @Test
    void setConnectionsTest() {
        GiteeConnection connection1 =
                new GiteeConnection("1", "http://localhost", null, new GiteeV5ClientBuilder(), false, 10, 10);
        GiteeConnection connection2 =
                new GiteeConnection("2", "http://localhost", null, new GiteeV5ClientBuilder(), false, 10, 10);
        GiteeConnectionConfig config = jenkins.get(GiteeConnectionConfig.class);
        List<GiteeConnection> connectionList1 = new ArrayList<>();
        connectionList1.add(connection1);

        config.setConnections(connectionList1);
        assertThat(config.getConnections(), is(connectionList1));

        List<GiteeConnection> connectionList2 = new ArrayList<>();
        connectionList2.add(connection1);
        connectionList2.add(connection2);

        config.setConnections(connectionList2);
        assertThat(config.getConnections(), is(connectionList2));

        config.setConnections(connectionList1);
        assertThat(config.getConnections(), is(connectionList1));
    }

    @Test
    void getClient_isCached() {
        GiteeConnection connection = new GiteeConnection(
                "test", "http://localhost", API_TOKEN_ID, new GiteeV5ClientBuilder(), false, 10, 10);
        GiteeConnectionConfig config = jenkins.get(GiteeConnectionConfig.class);
        List<GiteeConnection> connectionList1 = new ArrayList<>();
        connectionList1.add(connection);
        config.setConnections(connectionList1);

        GiteeClient client = config.getClient(connection.getName());
        assertNotNull(client);
        assertSame(client, config.getClient(connection.getName()));
    }

    @Test
    void authenticationEnabled_registered_success() throws Exception {
        String username = "test-user";
        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Item.BUILD, new PermissionEntry(AuthorizationType.EITHER, username));
        jenkins.getInstance().setAuthorizationStrategy(authorizationStrategy);
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitee-Event", "Push Hook");
        String auth = username + ":" + username;
        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.ISO_8859_1)));
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    void authenticationEnabled_anonymous_forbidden() throws Exception {
        boolean defaultValue = jenkins.get(GiteeConnectionConfig.class).isUseAuthenticatedEndpoint();
        assertTrue(defaultValue);
        jenkins.getInstance().setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy());
        URL jenkinsURL = jenkins.getURL();
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        GiteePushTrigger trigger = mock(GiteePushTrigger.class);
        project.addTrigger(trigger);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitee-Event", "Push Hook");
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(403));
    }

    @Test
    void authenticationDisabled_anonymous_success() throws Exception {
        jenkins.get(GiteeConnectionConfig.class).setUseAuthenticatedEndpoint(false);
        jenkins.getInstance().setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy());
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitee-Event", "Push Hook");
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }
}
