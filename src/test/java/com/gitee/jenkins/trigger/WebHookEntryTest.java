package com.gitee.jenkins.trigger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.connection.GiteeApiTokenImpl;
import com.gitee.jenkins.connection.GiteeConnection;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.impl.GiteeV5ClientBuilder;
import com.gitee.jenkins.gitee.api.model.WebHook;
import com.gitee.jenkins.gitee.api.model.builder.generated.WebHookBuilder;
import com.gitee.jenkins.trigger.GiteePushTrigger.WebhookEntry;

import hudson.model.Job;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jakarta.ws.rs.core.Response;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;

@WithJenkins
@ExtendWith(MockServerExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class WebHookEntryTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    private JenkinsRule jenkins;
    private JenkinsLocationConfiguration locationConfig;

    private static MockServerClient mockServerClient;
    private String giteeUrl;

    @Mock
    private Job<?, ?> job;

    private GiteeConnection connection;
    private GiteeConnectionProperty prop;

    private WebhookEntry.DescriptorImpl descriptor;

    @BeforeAll
    static void setUp(MockServerClient mockServerClient) {
        WebHookEntryTest.mockServerClient = mockServerClient;
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        descriptor = (WebhookEntry.DescriptorImpl) jenkins.jenkins
                .getDescriptor(WebhookEntry.class);

        locationConfig = JenkinsLocationConfiguration.get();
        locationConfig.setUrl("http://testingip.com");
        locationConfig.save();

        giteeUrl = "http://localhost:" + mockServerClient.getPort();
        connection = new GiteeConnection(
                "test", giteeUrl, API_TOKEN_ID, new GiteeV5ClientBuilder(),
                false, 10, 10);

        GiteeConnectionConfig config = jenkins.get(GiteeConnectionConfig.class);
        List<GiteeConnection> connectionList = new ArrayList<>();
        connectionList.add(connection);
        config.setConnections(connectionList);
        prop = new GiteeConnectionProperty("test");

        Mockito.when(job.getProperty(GiteeConnectionProperty.class)).thenReturn(prop);

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
    void testAddWebhookExisting() throws IOException {
        WebhookEntry entry = new WebhookEntry("test", "test", "test", true, false,
                false, false, false);

        String form = doGetWebhook(entry);

        assertTrue(form.contains(Messages.webhook_exist()));
    }

    @Test
    void testAddNewWebhook() throws IOException {
        WebhookEntry entry = new WebhookEntry("test", "test", "test",
                true, false, false, false, false);
        String form = doAddWebhook(entry);

        assertTrue(form.contains(Messages.connection_success(entry.getName())));
    }

    @Test
    void testLocalhostNotAllowed() {
        locationConfig.setUrl("http://localhost.com");
        locationConfig.save();

        WebhookEntry entry = new WebhookEntry("test", "test", "test",
                true, false, false, false, false);
        WebhookEntry.DescriptorImpl descriptor = (WebhookEntry.DescriptorImpl) jenkins.jenkins
                .getDescriptor(WebhookEntry.class);

        FormValidation formValidation = descriptor.doAddWebhook(entry.getRepo(), entry.getOwner(), entry.getName(),
                entry.isPush(), entry.isTagPush(),
                entry.isIssue(), entry.isNote(), entry.isPullRequest(), job);

        assertTrue(formValidation.getMessage().equals(Messages.localhost_error()));
    }

    private String doAddWebhook(WebhookEntry entry) throws JsonProcessingException {
        HttpRequest getWebhooksRequest = request()
                .withPath("/api/v5/repos/%s/%s/hooks".formatted(entry.getOwner(), entry.getRepo()))
                .withHeader("PRIVATE-TOKEN", API_TOKEN)
                .withMethod("GET");
        HttpRequest addWebHookRequest = request()
                .withPath("/api/v5/repos/%s/%s/hooks".formatted(entry.getOwner(), entry.getRepo()))
                .withHeader("PRIVATE-TOKEN", API_TOKEN)
                .withMethod("POST");

        WebHook hook = new WebHookBuilder()
                .withUrl(Jenkins.get().getRootUrl())
                .withTitle(entry.getName())
                .withPushEvents(entry.isPush())
                .withTagPushEvents(entry.isTagPush())
                .withNoteEvents(entry.isNote())
                .withIssuesEvents(entry.isIssue())
                .withMergeRequestsEvents(entry.isPullRequest())
                .withEncryptionType(0)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        String hookJson = mapper.writeValueAsString(hook);

        mockServerClient.when(getWebhooksRequest)
                .respond(
                        response()
                                .withStatusCode(Response.Status.OK.getStatusCode())
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody("[]"));

        mockServerClient.when(addWebHookRequest)
                .respond(
                        response()
                                .withStatusCode(Response.Status.OK.getStatusCode())
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(hookJson));

        descriptor = (WebhookEntry.DescriptorImpl) jenkins.jenkins
                .getDescriptor(WebhookEntry.class);

        FormValidation formValidation = descriptor.doAddWebhook(entry.getRepo(), entry.getOwner(), entry.getName(),
                entry.isPush(), entry.isTagPush(),
                entry.isIssue(), entry.isNote(), entry.isPullRequest(), job);

        mockServerClient.verify(getWebhooksRequest);
        return formValidation.getMessage();
    }

    private String doGetWebhook(WebhookEntry entry) throws JsonProcessingException {
        HttpRequest getWebhooksRequest = request()
                .withPath("/api/v5/repos/%s/%s/hooks".formatted(entry.getOwner(), entry.getRepo()))
                .withHeader("PRIVATE-TOKEN", API_TOKEN)
                .withMethod("GET");

        WebHook hook = new WebHookBuilder()
                .withUrl(Jenkins.get().getRootUrl())
                .withTitle(entry.getName())
                .withPushEvents(entry.isPush())
                .withTagPushEvents(entry.isTagPush())
                .withNoteEvents(entry.isNote())
                .withIssuesEvents(entry.isIssue())
                .withMergeRequestsEvents(entry.isPullRequest())
                .withEncryptionType(0)
                .build();

        ObjectMapper mapper = new ObjectMapper();
        List<WebHook> entries = new ArrayList<WebHook>();
        entries.add(hook);
        String hookJson = mapper.writeValueAsString(entries);

        mockServerClient.when(getWebhooksRequest)
                .respond(
                        response()
                                .withStatusCode(Response.Status.OK.getStatusCode())
                                .withContentType(MediaType.APPLICATION_JSON)
                                .withBody(hookJson));

        descriptor = (WebhookEntry.DescriptorImpl) jenkins.jenkins
                .getDescriptor(WebhookEntry.class);

        FormValidation formValidation = descriptor.doAddWebhook(entry.getRepo(), entry.getOwner(), entry.getName(),
                entry.isPush(), entry.isTagPush(),
                entry.isIssue(), entry.isNote(), entry.isPullRequest(), job);

        return formValidation.getMessage();
    }
}
