package com.gitee.jenkins.publisher;

import static com.gitee.jenkins.publisher.TestUtility.GITEE_CONNECTION_V5;
import static com.gitee.jenkins.publisher.TestUtility.OWNER_PATH;
import static com.gitee.jenkins.publisher.TestUtility.REPO_PATH;
import static com.gitee.jenkins.publisher.TestUtility.setupGiteeConnections;
import static com.gitee.jenkins.publisher.TestUtility.verifyMatrixAggregatable;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static com.gitee.jenkins.publisher.TestUtility.mockSimpleBuild;
import java.io.IOException;
import java.nio.charset.Charset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.MediaType;
import org.mockserver.verify.VerificationTimes;

import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;

@WithJenkins
@ExtendWith(MockServerExtension.class)
public class GiteeCreatePullRequestPublisherTest {
    private static JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private BuildListener listener;

    @BeforeAll
    static void setUp(JenkinsRule rule, MockServerClient client) throws Exception {
        jenkins = rule;
        mockServerClient = client;
        setupGiteeConnections(jenkins, client);
    }

    @BeforeEach
    void setUp() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void matrixAggregatable() throws Exception {
        verifyMatrixAggregatable(GiteeMessagePublisher.class, listener);
    }

    @Test
    void success() throws InterruptedException, IOException {
        HttpRequest createPullRequestStatus = prepareCreatePullRequest("v5");
        HttpRequest getPullRequest = prepareGetPullRequest("v5");
        mockServerClient.when(getPullRequest)
                .respond(response().withStatusCode(200).withBody("[]").withContentType(MediaType.APPLICATION_JSON));
        mockServerClient.when(createPullRequestStatus).respond(response().withStatusCode(200));

        GiteeCreatePullRequestPublisher p = new GiteeCreatePullRequestPublisher();
        p.setRepo(REPO_PATH);
        p.setOwner(OWNER_PATH);
        p.setBase("base");
        p.setHead("head");
        p.setTitle("title");

        p.perform(mockSimpleBuild(GITEE_CONNECTION_V5, Result.SUCCESS), null, listener);
        mockServerClient.verify(createPullRequestStatus);
    }

    @Test
    void pullRequestExists() throws InterruptedException, IOException {
        HttpRequest createPullRequestStatus = prepareCreatePullRequest("v5");
        mockServerClient.when(createPullRequestStatus).respond(response().withStatusCode(200));

        HttpRequest getPullRequest = prepareGetPullRequest("v5");

        GiteeCreatePullRequestPublisher p = new GiteeCreatePullRequestPublisher();
        p.setRepo(REPO_PATH);
        p.setOwner(OWNER_PATH);
        p.setBase("base");
        p.setHead("head");
        p.setTitle("title");

        mockServerClient.when(getPullRequest)
                .respond(response().withStatusCode(200).withBody(
                        String.format("[{ \"base\":\"%s\", \"head\":\"%s\" }]", p.getBase(), p.getHead()))
                        .withContentType(MediaType.APPLICATION_JSON));
        
        p.perform(mockSimpleBuild(GITEE_CONNECTION_V5, Result.SUCCESS), null, listener);
        mockServerClient.verify(createPullRequestStatus, VerificationTimes.never());
    }

    @Test
    void failed() throws InterruptedException, IOException {
        GiteeCreatePullRequestPublisher p = new GiteeCreatePullRequestPublisher();
        p.setRepo(REPO_PATH);
        p.setOwner(OWNER_PATH);
        p.setBase("base");
        p.setHead("head");
        p.setTitle("title");

        p.perform(mockSimpleBuild(GITEE_CONNECTION_V5, Result.FAILURE), null, listener);
        mockServerClient.verifyZeroInteractions();
    }

    private HttpRequest prepareCreatePullRequest(String apiLevel) {
        return request()
                .withPath(String.format("/gitee/api/%s/repos/%s/%s/pulls", apiLevel, OWNER_PATH, REPO_PATH))
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest prepareGetPullRequest(String apiLevel) {
        return request()
                .withPath(String.format("/gitee/api/%s/repos/%s/%s/pulls", apiLevel, OWNER_PATH, REPO_PATH))
                .withMethod("GET")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

}
