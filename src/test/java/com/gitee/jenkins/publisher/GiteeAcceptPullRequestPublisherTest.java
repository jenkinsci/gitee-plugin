package com.gitee.jenkins.publisher;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import static com.gitee.jenkins.publisher.TestUtility.GITEE_CONNECTION_V5;
import static com.gitee.jenkins.publisher.TestUtility.PULL_REQUEST_ID;
import static com.gitee.jenkins.publisher.TestUtility.PULL_REQUEST_IID;
import static com.gitee.jenkins.publisher.TestUtility.REPO_PATH;
import static com.gitee.jenkins.publisher.TestUtility.PROJECT_ID;
import static com.gitee.jenkins.publisher.TestUtility.mockSimpleBuild;
import static com.gitee.jenkins.publisher.TestUtility.preparePublisher;
import static com.gitee.jenkins.publisher.TestUtility.setupGiteeConnections;
import static com.gitee.jenkins.publisher.TestUtility.verifyMatrixAggregatable;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
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

/**
 * @author Nikolay Ustinov
 */
@WithJenkins
@ExtendWith(MockServerExtension.class)
class GiteeAcceptPullRequestPublisherTest {

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
        verifyMatrixAggregatable(GiteeAcceptPullRequestPublisher.class, listener);
    }

    @Test
    void success() throws Exception {
        publish(mockSimpleBuild(GITEE_CONNECTION_V5, Result.SUCCESS));

        mockServerClient.verify(
                prepareAcceptPullRequestWithSuccessResponse("v5", PULL_REQUEST_IID, null));
    }

    @Test
    void failed() throws Exception {
        publish(mockSimpleBuild(GITEE_CONNECTION_V5, Result.FAILURE));

        mockServerClient.verifyZeroInteractions();
    }

    private void publish(AbstractBuild build) throws Exception {
        GiteeAcceptPullRequestPublisher publisher = preparePublisher(new GiteeAcceptPullRequestPublisher(), build);
        publisher.perform(build, null, listener);
    }

    private HttpRequest prepareAcceptPullRequestWithSuccessResponse(
            String apiLevel, int pullRequestId, Boolean shouldRemoveSourceBranch) {
        HttpRequest updateCommitStatus = prepareAcceptPullRequest(apiLevel, pullRequestId, shouldRemoveSourceBranch);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareAcceptPullRequest(String apiLevel, int pullRequestId, Boolean removeSourceBranch) {
        return request()
                .withPath(String.format("/gitee/api/%s/repos/%d/%s/pulls/%d/merge", apiLevel, PROJECT_ID, REPO_PATH, pullRequestId))
                .withMethod("PUT")
                .withHeader("PRIVATE-TOKEN", "secret");
    }
}
