package com.gitee.jenkins.publisher;

import static com.gitee.jenkins.publisher.TestUtility.BUILD_NUMBER;
import static com.gitee.jenkins.publisher.TestUtility.BUILD_URL;
import static com.gitee.jenkins.publisher.TestUtility.GITEE_CONNECTION_V5;
import static com.gitee.jenkins.publisher.TestUtility.PROJECT_ID;
import static com.gitee.jenkins.publisher.TestUtility.PULL_REQUEST_IID;
import static com.gitee.jenkins.publisher.TestUtility.REPO_PATH;
import static com.gitee.jenkins.publisher.TestUtility.formatNote;
import static com.gitee.jenkins.publisher.TestUtility.preparePublisher;
import static com.gitee.jenkins.publisher.TestUtility.setupGiteeConnections;
import static com.gitee.jenkins.publisher.TestUtility.verifyMatrixAggregatable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.stubbing.Answer;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

import com.gitee.jenkins.connection.GiteeConnectionProperty;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.util.BuildData;

/**
 * @author Nikolay Ustinov
 */
@WithJenkins
@ExtendWith(MockServerExtension.class)
class GiteeMessagePublisherTest {

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
    void canceled_v5() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.ABORTED);
        String defaultNote = formatNote(build,
                ":point_up: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} # {2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void success_v5() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.SUCCESS);
        String defaultNote = formatNote(
                build, ":white_check_mark: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} # {2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void success_withOnlyForFailure() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.SUCCESS);

        performAndVerify(build, "test", true, false, false, false, false);
    }

    @Test
    void failed_v5() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.FAILURE);
        String defaultNote = formatNote(build,
                ":x: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} # {2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void failed_withOnlyForFailed() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.FAILURE);
        String defaultNote = formatNote(build,
                ":x: Jenkins Build {0}\n\nResults available at: [Jenkins [{1} # {2}]]({3})");

        performAndVerify(
                build,
                defaultNote,
                true,
                false,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void canceledWithCustomNote() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.ABORTED);
        String defaultNote = "abort";

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                true,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void successWithCustomNote() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.SUCCESS);
        String defaultNote = "success";

        performAndVerify(
                build,
                defaultNote,
                false,
                true,
                false,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void failedWithCustomNote() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.FAILURE);
        String defaultNote = "failure";

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                true,
                false,
                false,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    @Test
    void unstableWithCustomNote() throws Exception {
        @SuppressWarnings("rawtypes")
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.UNSTABLE);
        String defaultNote = "unstable";

        performAndVerify(
                build,
                defaultNote,
                false,
                false,
                false,
                false,
                true,
                prepareSendMessageWithSuccessResponse("v5", PULL_REQUEST_IID, defaultNote));
    }

    private void performAndVerify(
            @SuppressWarnings("rawtypes") AbstractBuild build,
            String note,
            boolean onlyForFailure,
            boolean replaceSuccessNote,
            boolean replaceFailureNote,
            boolean replaceAbortNote,
            boolean replaceUnstableNote,
            HttpRequest... requests)
            throws Exception {
        String successNoteText = replaceSuccessNote ? note : null;
        String failureNoteText = replaceFailureNote ? note : null;
        String abortNoteText = replaceAbortNote ? note : null;
        String unstableNoteText = replaceUnstableNote ? note : null;
        GiteeMessagePublisher publisher = preparePublisher(
                new GiteeMessagePublisher(
                        onlyForFailure,
                        replaceSuccessNote,
                        replaceFailureNote,
                        replaceAbortNote,
                        replaceUnstableNote,
                        successNoteText,
                        failureNoteText,
                        abortNoteText,
                        unstableNoteText),
                build);
        publisher.perform(build, null, listener);

        if (requests.length > 0) {
            mockServerClient.verify(requests);
        } else {
            mockServerClient.verifyZeroInteractions();
        }
    }

    private HttpRequest prepareSendMessageWithSuccessResponse(String apiLevel, int pullRequestId, String body) {
        HttpRequest updateCommitStatus = prepareSendMessageStatus(apiLevel, pullRequestId, body);
        mockServerClient.when(updateCommitStatus).respond(response().withStatusCode(200));
        return updateCommitStatus;
    }

    private HttpRequest prepareSendMessageStatus(final String apiLevel, int pullRequestId, String body) {
        JsonBody json = new JsonBody(String.format(
                "{\"type\": \"STRING\", \"string\":\"body=%s\", \"contentType\":\"application/x-www-form-urlencoded\"}",
                URLEncoder.encode(body, StandardCharsets.UTF_8)));

        return request()
                .withPath(String.format("/gitee/api/%s/repos/%d/%s/pulls/%d/comments", apiLevel, PROJECT_ID, REPO_PATH,
                        pullRequestId))
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody(json);
    }

    @SuppressWarnings("rawtypes")
    private AbstractBuild mockBuild(String giteeConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        BuildData buildData = mock(BuildData.class);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
        when(build.getAction(BuildData.class)).thenReturn(buildData);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(BUILD_URL);
        when(build.getResult()).thenReturn(result);
        when(build.getNumber()).thenReturn(BUILD_NUMBER);

        AbstractProject<?, ?> project = mock(AbstractProject.class);
        when(project.getProperty(GiteeConnectionProperty.class))
                .thenReturn(new GiteeConnectionProperty(giteeConnection));
        doReturn(project).when(build).getParent();
        doReturn(project).when(build).getProject();
        EnvVars environment = mock(EnvVars.class);
        when(environment.expand(anyString()))
                .thenAnswer((Answer<String>) invocation -> (String) invocation.getArguments()[0]);
        try {
            when(build.getEnvironment(any(TaskListener.class))).thenReturn(environment);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return build;
    }
}
