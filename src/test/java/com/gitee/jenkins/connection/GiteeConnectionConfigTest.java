package com.gitee.jenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.api.GiteeClient;
import hudson.ProxyConfiguration;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.gitee.jenkins.connection.Messages.connection_error;
import static com.gitee.jenkins.connection.Messages.connection_success;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Robin Müller
 */
public class GiteeConnectionConfigTest {

    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";

    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private MockServerClient mockServerClient;
    private String gitLabUrl;

    @Before
    public void setup() throws IOException {
        gitLabUrl = "http://localhost:" + mockServer.getPort() + "/gitee";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "Gitee API Token", Secret.fromString(API_TOKEN)));
            }
        }
    }

    @Test
    public void doCheckConnection_success() {
        String expected = connection_success();
        assertThat(doCheckConnection("v3", Response.Status.OK), is(expected));
        assertThat(doCheckConnection("v4", Response.Status.OK), is(expected));
    }

    @Test
    public void doCheckConnection_forbidden() {
        String expected = connection_error("HTTP 403 Forbidden");
        assertThat(doCheckConnection("v3", Response.Status.FORBIDDEN), is(expected));
        assertThat(doCheckConnection("v4", Response.Status.FORBIDDEN), is(expected));
    }

    @Test
    public void doCheckConnection_proxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("0.0.0.0", 80);
        GiteeConnectionConfig connectionConfig = jenkins.get(GiteeConnectionConfig.class);
        FormValidation result = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, "v3", false, 10, 10);
        assertThat(result.getMessage(), containsString("Connection refused"));
    }

    @Test
    public void doCheckConnection_noProxy() {
        jenkins.getInstance().proxy = new ProxyConfiguration("0.0.0.0", 80, "", "", "localhost");
        assertThat(doCheckConnection("v3", Response.Status.OK), is(connection_success()));
    }


    private String doCheckConnection(String clientBuilderId, Response.Status status) {
        HttpRequest request = request().withPath("/gitee/api/" + clientBuilderId + "/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(status.getStatusCode()));

        GiteeConnectionConfig connectionConfig = jenkins.get(GiteeConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, clientBuilderId, false, 10, 10);
        mockServerClient.verify(request);
        return formValidation.getMessage();
    }


    @Test
    public void authenticationEnabled_anonymous_forbidden() throws IOException {
        Boolean defaultValue = jenkins.get(GiteeConnectionConfig.class).isUseAuthenticatedEndpoint();
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
    public void authenticationEnabled_registered_success() throws Exception {
        String username = "test-user";
        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Item.BUILD, username);
        jenkins.getInstance().setAuthorizationStrategy(authorizationStrategy);
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitee-Event", "Push Hook");
        String auth = username + ":" + username;
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")))));
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void authenticationDisabled_anonymous_success() throws IOException, URISyntaxException {
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

    @Test
    public void setConnectionsTest() {
        GiteeConnection connection1 = new GiteeConnection("1", "http://localhost", null, new V3GiteeClientBuilder(), false, 10, 10);
        GiteeConnection connection2 = new GiteeConnection("2", "http://localhost", null, new V3GiteeClientBuilder(), false, 10, 10);
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
    public void getClient_is_cached() {
        GiteeConnection connection = new GiteeConnection("test", "http://localhost", API_TOKEN_ID, new V3GiteeClientBuilder(), false, 10, 10);
        GiteeConnectionConfig config = jenkins.get(GiteeConnectionConfig.class);
        List<GiteeConnection> connectionList1 = new ArrayList<>();
        connectionList1.add(connection);
        config.setConnections(connectionList1);

        GiteeClient client = config.getClient(connection.getName());
        assertNotNull(client);
        assertSame(client, config.getClient(connection.getName()));
    }
}
