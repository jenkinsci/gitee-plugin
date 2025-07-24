package com.gitee.jenkins.webhook.build;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static com.gitee.jenkins.cause.CauseData.ActionType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.Queue;
import hudson.model.StringParameterValue;
import hudson.model.queue.QueueListener;
import hudson.model.queue.QueueTaskFuture;
import hudson.plugins.git.GitSCM;
import jakarta.servlet.ServletException;
import java.io.File;
import java.io.PrintWriter;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.gitee.jenkins.cause.CauseData.ActionType;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.trigger.filter.BranchFilterType;

/**
 * @author Robin Müller
 */
@WithJenkins
@ExtendWith(MockitoExtension.class)
class PullRequestBuildActionTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    @Mock
    private StaplerResponse2 response;

    private static volatile boolean wouldFire = false;

    private GiteePushTrigger trigger = new GiteePushTrigger();

    private String gitRepoUrl;
    private String commitSha1;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
        QueueListener ql = new QueueListener() {
            @Override
            public void onEnterWaiting(Queue.WaitingItem wi) {
                System.out.println("Got " + wi + " : " + wi.getCausesDescription());
                wouldFire = true;
            }

            @Override
            public void onEnterBuildable(Queue.BuildableItem bi) {
                System.out.println("Is buildable: " + bi.getCausesDescription());
            }
        };
        jenkins.getInstance().getExtensionList(QueueListener.class).add(ql);
    }

    @BeforeEach
    void setUp() throws Exception {
        when(response.getWriter()).thenAnswer(i -> new PrintWriter(System.out));
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        commitSha1 = commit.getId().getName();
        gitRepoUrl = tmp.toURI().toString();

        // some defaults of the trigger
        trigger.setBranchFilterType(BranchFilterType.All);
    }

    @Test
    void build() throws Exception {
        GiteePushTrigger mockTrigger = mock(GiteePushTrigger.class);
        try {
            FreeStyleProject testProject = jenkins.createFreeStyleProject();
            testProject.addTrigger(mockTrigger);
            executePullRequestAction(testProject, getJson("PullRequestEvent.json"));
        } finally {
            ArgumentCaptor<PullRequestHook> pushHookArgumentCaptor = ArgumentCaptor.forClass(PullRequestHook.class);
            verify(mockTrigger).onPost(pushHookArgumentCaptor.capture());
            assertThat(pushHookArgumentCaptor.getValue().getRepo(), is(notNullValue()));
            assertThat(pushHookArgumentCaptor.getValue().getRepo().getWebUrl(), is(notNullValue()));
        }
    }

    private void executePullRequestAction(FreeStyleProject testProject, String json) throws Exception {
        try {
            wouldFire = false;

            trigger.start(testProject, false);

            new PullRequestBuildAction(testProject, json, null).execute(response);
        } catch (HttpResponses.HttpResponseException hre) {
            
            // Test for OK status of a response.
            try {
                hre.generateResponse(null, response, null);
                verify(response, atLeastOnce()).getWriter();
                verify(response, atLeastOnce()).setContentType("text/plain;charset=UTF-8");
                // assertThat(response.getStatus(), is(200));
            } catch (ServletException e) {
                throw new Exception(e);
            }
        }
        // The assumption is, that queue listener have already been invoked when we got back a response.
    }

    @Test
    void skip_closedPR() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);

        executePullRequestAction(testProject, getJson("PullRequestEvent_closedPR.json"));
        assertFalse(wouldFire);
    }

    @Test
    void skip_approvedPR() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));

        executePullRequestAction(testProject, getJson("PullRequestEvent_approvedPR.json"));

        assertFalse(wouldFire);
    }

    @Test
    void skip_alreadyBuiltPR() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        executePullRequestAction(testProject, getJson("PullRequestEvent_alreadyBuiltPR_initialBuild.json"));
        jenkins.waitUntilNoActivity();
        executePullRequestAction(testProject, getJson("PullRequestEvent_alreadyBuiltPR.json"));
        assertFalse(wouldFire);
    }

    @Test
    void build_acceptedPR() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        trigger.setTriggerOnAcceptedPullRequest(true);
        // trigger.setTriggerOnPullRequest(false);
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
                0, new ParametersAction(new StringParameterValue("giteeTargetBranch", "master")));
        future.get();

        executePullRequestAction(testProject, getJson("PullRequestEvent_merged.json"));
        assertTrue(wouldFire);
    }

    @Test
    void build_alreadyBuiltPR_differentTargetBranch() throws Exception {
        FreeStyleProject testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
        testProject.setScm(new GitSCM(gitRepoUrl));
        QueueTaskFuture<?> future = testProject.scheduleBuild2(
                0,
                new GiteeWebHookCause(causeData()
                        .withActionType(ActionType.MERGE)
                        .withSourceProjectId(1)
                        .withTargetProjectId(1)
                        .withBranch("feature")
                        .withSourceBranch("feature")
                        .withUserName("")
                        .withSourceRepoHomepage("https://gitee.org/test")
                        .withSourceRepoName("test")
                        .withSourceNamespace("test-namespace")
                        .withSourceRepoUrl("git@gitee.org:test.git")
                        .withSourceRepoSshUrl("git@gitee.org:test.git")
                        .withSourceRepoHttpUrl("https://gitee.org/test.git")
                        .withPullRequestTitle("Test")
                        .withPullRequestId(1)
                        .withPullRequestIid(1)
                        .withTargetBranch("master")
                        .withTargetRepoName("test")
                        .withTargetNamespace("test-namespace")
                        .withTargetRepoSshUrl("git@gitee.org:test.git")
                        .withTargetRepoHttpUrl("https://gitee.org/test.git")
                        .withTriggeredByUser("test")
                        .withLastCommit("123")
                        .withTargetProjectUrl("https://gitee.org/test")
                        .build()));
        future.get();

        executePullRequestAction(testProject, getJson("PullRequestEvent_alreadyBuiltPR_differentTargetBranch.json"));

        assertTrue(wouldFire);
    }

    private String getJson(String name) throws Exception {
        return IOUtils.toString(getClass().getResourceAsStream(name), StandardCharsets.UTF_8)
                .replace("${commitSha1}", commitSha1);
    }
}
