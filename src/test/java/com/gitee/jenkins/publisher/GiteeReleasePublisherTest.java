package com.gitee.jenkins.publisher;

import static com.gitee.jenkins.publisher.TestUtility.BUILD_NUMBER;
import static com.gitee.jenkins.publisher.TestUtility.BUILD_URL;
import static com.gitee.jenkins.publisher.TestUtility.GITEE_CONNECTION_V5;
import static com.gitee.jenkins.publisher.TestUtility.OWNER_PATH;
import static com.gitee.jenkins.publisher.TestUtility.REPO_PATH;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static com.gitee.jenkins.publisher.TestUtility.setupGiteeConnections;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mockserver.model.HttpRequest;
import org.mockserver.model.JsonBody;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.stubbing.Answer;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;

import com.gitee.jenkins.connection.GiteeConnectionProperty;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.Revision;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.Publisher;
import hudson.util.DescribableList;

@WithJenkins
@ExtendWith(MockServerExtension.class)
public class GiteeReleasePublisherTest {

    private static JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private BuildListener listener;

    @TempDir
    Path tempDir;

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
    void createReleaseSuccessTest() throws IOException, InterruptedException {
        AbstractBuild<?, ?> build = mockBuild(GITEE_CONNECTION_V5, Result.SUCCESS, false);
        HttpRequest request = createRelease("v5", "1.0.0");
        mockServerClient.when(request).respond(response().withStatusCode(200)
                .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON).withBody("{\"test\": \"test\"}"));
        performAndVerify(build, false, "1.0.0", false, request);
    }

    @Test
    void incrementReleaseTest() throws IOException, InterruptedException {
        AbstractBuild<?, ?> build = mockBuild(GITEE_CONNECTION_V5, Result.SUCCESS, false);
        HttpRequest latestReleaseRequest = getLatestRelease("v5");
        HttpRequest createReleaseRequest = createRelease("v5", "1.1.0");

        JsonBody jsonLatestRelease = new JsonBody("{\"tag_name\": \"1.0.0\"}");
        JsonBody jsonCreateRelease = new JsonBody("{\"test\": \"test\"}");
        mockServerClient.when(latestReleaseRequest).respond(
                response()
                        .withStatusCode(200)
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(jsonLatestRelease));

        mockServerClient.when(createReleaseRequest).respond(
                response()
                        .withStatusCode(200)
                        .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                        .withBody(jsonCreateRelease));

        performAndVerify(build, true, "=.+.0", false, latestReleaseRequest);
    }

    @Disabled
    @Test
    void attachReleaseFileTest() throws IOException, InterruptedException {
        AbstractBuild build = mockBuild(GITEE_CONNECTION_V5, Result.SUCCESS, true);
        HttpRequest createReleaseRequest = createRelease("v5", "1.0.0");
        HttpRequest attachFileRequest = attachFile("v5");

        mockServerClient.when(createReleaseRequest).respond(response().withStatusCode(200)
                .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON).withBody("{\"test\": \"test\"}"));

        performAndVerify(build, false, "1.0.0", true, createReleaseRequest, attachFileRequest);

    }

    private void performAndVerify(
            AbstractBuild<?, ?> build,
            boolean increment,
            String tagName,
            boolean attachFiles,
            HttpRequest... requests) {
        GiteeReleasePublisher publisher = new GiteeReleasePublisher();
        publisher.setOwner(OWNER_PATH);
        publisher.setRepo(REPO_PATH);
        publisher.setTagName(tagName);
        publisher.setName("test");
        publisher.setIncrement(increment);
        publisher.setArtifacts(attachFiles);

        publisher.perform(build, null, listener);

        if (requests.length > 0) {
            mockServerClient.verify(requests);
        } else {
            mockServerClient.verifyZeroInteractions();
        }
    }

    @SuppressWarnings("rawtypes")
    private AbstractBuild mockBuild(String giteeConnection, Result result, boolean isArchiver, String... remoteUrls)
            throws IOException, InterruptedException {
        AbstractBuild<?, ?> build = mock(AbstractBuild.class);
        DescribableList<Publisher, Descriptor<Publisher>> describableList = mock(DescribableList.class);
        BuildData buildData = mock(BuildData.class);
        ObjectId objectId = new ObjectId(0, 0, 0, 0, 0);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
        when(build.getAction(BuildData.class)).thenReturn(buildData);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(BUILD_URL);
        when(build.getResult()).thenReturn(result);
        when(build.getNumber()).thenReturn(BUILD_NUMBER);
        when(buildData.getLastBuiltRevision()).thenReturn(new Revision(objectId));

        AbstractProject<?, ?> project = mock(AbstractProject.class);
        GitSCM scm = mock(GitSCM.class);
        when(scm.getBuildData(build)).thenReturn(buildData);
        when(project.getScm()).thenReturn(scm);

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

    private HttpRequest createRelease(String apiLevel, String tagName) {
        return request()
                .withPath(String.format("/gitee/api/%s/repos/%s/%s/releases", apiLevel, OWNER_PATH, REPO_PATH))
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody(String.format(
                        "tag_name=%s&name=test&prerelease=false&target_commitish=0000000000000000000000000000000000000000",
                        tagName));

    }

    private HttpRequest getLatestRelease(String apiLevel) {
        return request()
                .withPath(String.format("/gitee/api/%s/repos/%s/%s/releases/latest", apiLevel, OWNER_PATH, REPO_PATH))
                .withMethod("GET")
                .withHeader("PRIVATE-TOKEN", "secret");
    }

    private HttpRequest attachFile(String apiLevel) {
        return request()
                .withPath(String.format("/gitee/api/%s/repos/%s/%s/releases/attach_files", apiLevel, OWNER_PATH,
                        REPO_PATH))
                .withMethod("POST")
                .withHeader("PRIVATE-TOKEN", "secret")
                .withBody("");
    }
}
