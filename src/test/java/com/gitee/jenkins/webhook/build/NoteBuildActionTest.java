package com.gitee.jenkins.webhook.build;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.CauseDataBuilder;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.trigger.GiteePushTrigger;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse2;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Nikolay Ustinov
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class NoteBuildActionTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    @Mock
    private StaplerResponse2 response;

    @Mock
    private GiteePushTrigger trigger;

    private String gitRepoUrl;
    private String commitSha1;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() throws Exception {
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        commitSha1 = commit.getId().getName();
        gitRepoUrl = tmp.toURI().toString();
    }

    @Test
    void build() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        assertThrows(HttpResponses.HttpResponseException.class, () ->
            new NoteBuildAction(testProject, getJson("NoteEvent.json"), null).execute(response));
        verify(trigger).onPost(any(NoteHook.class));
    }

    @Test
    void build_alreadyBuiltPR_alreadyBuiltPR() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
            0, new ParametersAction(new StringParameterValue("gitlabTargetBranch", "master")));
        future.get();
        assertThrows(HttpResponses.HttpResponseException.class, () ->
            new NoteBuildAction(testProject, getJson("NoteEvent_alreadyBuiltMR.json"), null).execute(response));
        verify(trigger).onPost(any(NoteHook.class));
    }

    @Test
    void build_alreadyBuiltPR_differentTargetBranch() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
                    0,
                    new GiteeWebHookCause(CauseDataBuilder.causeData()
                            .withActionType(CauseData.ActionType.NOTE)
                            .withSourceProjectId(1)
                            .withTargetProjectId(1)
                            .withBranch("feature")
                            .withSourceBranch("feature")
                            .withUserName("")
                            .withSourceRepoHomepage("https://gitee.com/test")
                            .withSourceRepoName("test")
                            .withSourceNamespace("test-namespace")
                            .withSourceRepoUrl("git@gitee.com:test.git")
                            .withSourceRepoSshUrl("git@gitee.com:test.git")
                            .withSourceRepoHttpUrl("https://gitee.com/test.git")
                            .withPullRequestTitle("Test")
                            .withPullRequestId(1)
                            .withPullRequestIid(1)
                            .withTargetBranch("master")
                            .withTargetRepoName("test")
                            .withTargetNamespace("test-namespace")
                            .withTargetRepoSshUrl("git@gitee.com:test.git")
                            .withTargetRepoHttpUrl("https://gitee.com/test.git")
                            .withTriggeredByUser("test")
                            .withLastCommit("123")
                            .withTargetProjectUrl("https://gitee.com/test")
                            .build()));
        future.get();
        assertThrows(HttpResponses.HttpResponseException.class, () ->
            new NoteBuildAction(testProject, getJson("NoteEvent_alreadyBuiltMR.json"), null).execute(response));
        verify(trigger).onPost(any(NoteHook.class));
    }

    private String getJson(String name) throws Exception {
        return IOUtils.toString(getClass().getResourceAsStream(name), StandardCharsets.UTF_8)
                .replace("${commitSha1}", commitSha1);
    }
}
