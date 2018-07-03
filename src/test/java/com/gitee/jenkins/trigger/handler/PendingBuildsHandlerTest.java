package com.gitee.jenkins.trigger.handler;

import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.BuildState;
import com.gitee.jenkins.gitee.hook.model.*;
import com.gitee.jenkins.gitee.hook.model.builder.generated.*;
import com.gitee.jenkins.trigger.filter.BranchFilterType;
import hudson.model.FreeStyleProject;
import hudson.model.ItemGroup;
import hudson.model.Project;
import hudson.model.Queue;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.gitee.jenkins.gitee.hook.model.builder.generated.CommitBuilder.commit;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.MergeRequestObjectAttributesBuilder.mergeRequestObjectAttributes;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.UserBuilder.user;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PendingBuildsHandlerTest {

    private static final String GITEE_BUILD_NAME = "Jenkins";

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Mock
    private GiteeClient giteeClient;

    @Mock
    private GiteeConnectionProperty giteeConnectionProperty;

    @Before
    public void init() {
        when(giteeConnectionProperty.getClient()).thenReturn(giteeClient);
    }

    @After
    public void clearQueue() {
        Queue queue = jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            queue.cancel(item);
        }
    }

    @Test
    public void projectCanBeConfiguredToSendPendingBuildStatusWhenTriggered() throws IOException {
        Project project = freestyleProject("freestyleProject1", new GiteeCommitStatusPublisher(GITEE_BUILD_NAME, false));

        GiteePushTrigger giteePushTrigger = giteePushTrigger(project);

        giteePushTrigger.onPost(pushHook(1, "branch1", "commit1"));

        verify(giteeClient).changeBuildStatus(eq(1), eq("commit1"), eq(BuildState.pending), eq("branch1"), eq(GITEE_BUILD_NAME),
            contains("/freestyleProject1/"), eq(BuildState.pending.name()));
        verifyNoMoreInteractions(giteeClient);
    }

    @Test
    public void workflowJobCanConfiguredToSendToPendingBuildStatusWhenTriggered() throws IOException {
        WorkflowJob workflowJob = workflowJob();

        GiteePushTrigger giteePushTrigger = giteePushTrigger(workflowJob);
        giteePushTrigger.setPendingBuildName(GITEE_BUILD_NAME);

        giteePushTrigger.onPost(mergeRequestHook(1, "branch1", "commit1"));

        verify(giteeClient).changeBuildStatus(eq(1), eq("commit1"), eq(BuildState.pending), eq("branch1"), eq(GITEE_BUILD_NAME),
            contains("/workflowJob/"), eq(BuildState.pending.name()));
        verifyNoMoreInteractions(giteeClient);
    }

    @Test
    public void queuedMergeRequestBuildsCanBeCancelledOnMergeRequestUpdate() throws IOException {
        Project project = freestyleProject("project1", new GiteeCommitStatusPublisher(GITEE_BUILD_NAME, false));

        GiteePushTrigger giteePushTrigger = giteePushTrigger(project);
        giteePushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        giteePushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit1")); // Will be cancelled
        giteePushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit2")); // Will be cancelled
        giteePushTrigger.onPost(mergeRequestHook(1, "sourceBranch", "commit3"));
        giteePushTrigger.onPost(mergeRequestHook(1, "anotherBranch", "commit4"));
        giteePushTrigger.onPost(mergeRequestHook(2, "sourceBranch", "commit5"));

        verify(giteeClient).changeBuildStatus(eq(1), eq("commit1"), eq(BuildState.canceled), eq("sourceBranch"),
            eq("Jenkins"), contains("project1"), eq(BuildState.canceled.name()));
        verify(giteeClient).changeBuildStatus(eq(1), eq("commit2"), eq(BuildState.canceled), eq("sourceBranch"),
            eq("Jenkins"), contains("project1"), eq(BuildState.canceled.name()));

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(3));
    }

    private GiteePushTrigger giteePushTrigger(Project project) throws IOException {
        GiteePushTrigger giteePushTrigger = giteePushTrigger();
        project.addTrigger(giteePushTrigger);
        giteePushTrigger.start(project,true);
        return giteePushTrigger;
    }

    private GiteePushTrigger giteePushTrigger(WorkflowJob workflowJob) {
        GiteePushTrigger giteePushTrigger = giteePushTrigger();
        workflowJob.addTrigger(giteePushTrigger);
        giteePushTrigger.start(workflowJob,true);
        return giteePushTrigger;
    }

    private GiteePushTrigger giteePushTrigger() {
        GiteePushTrigger giteePushTrigger = new GiteePushTrigger();
        giteePushTrigger.setTriggerOnPush(true);
        giteePushTrigger.setTriggerOnMergeRequest(true);
        giteePushTrigger.setPendingBuildName(GITEE_BUILD_NAME);
        giteePushTrigger.setBranchFilterType(BranchFilterType.NameBasedFilter);
        giteePushTrigger.setBranchFilterName("");
        return giteePushTrigger;
    }

    private MergeRequestHook mergeRequestHook(int projectId, String branch, String commitId) {
        return MergeRequestHookBuilder.mergeRequestHook()
            .withObjectAttributes(mergeRequestObjectAttributes()
                .withAction(Action.update)
                .withState(State.updated)
                .withIid(1)
                .withTitle("test")
                .withTargetProjectId(1)
                .withTargetBranch("targetBranch")
                .withSourceBranch(branch)
                .withSourceProjectId(projectId)
                .withLastCommit(commit().withAuthor(user().withName("author").build()).withId(commitId).build())
                .withSource(ProjectBuilder.project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitee.org/test")
                    .withUrl("git@gitee.org:test.git")
                    .withSshUrl("git@gitee.org:test.git")
                    .withHttpUrl("https://gitee.org/test.git")
                    .build())
                .withTarget(ProjectBuilder.project()
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitee.org/test")
                    .withUrl("git@gitee.org:test.git")
                    .withSshUrl("git@gitee.org:test.git")
                    .withHttpUrl("https://gitee.org/test.git")
                    .build())
                .build())
            .withProject(ProjectBuilder.project()
                .withWebUrl("https://gitee.org/test.git")
                .build()
            )
            .build();
    }

    private PushHook pushHook(int projectId, String branch, String commitId) {
        User user = new UserBuilder()
            .withName("username")
            .build();

        Repository repository = new RepositoryBuilder()
            .withName("repository")
            .withGitSshUrl("sshUrl")
            .withGitHttpUrl("httpUrl")
            .build();

        return new PushHookBuilder()
            .withProjectId(projectId)
            .withRef(branch)
            .withAfter(commitId)
            .withRepository(new Repository())
            .withProject(ProjectBuilder.project().withNamespace("namespace").build())
            .withCommits(Arrays.asList(CommitBuilder.commit().withId(commitId).withAuthor(user).build()))
            .withRepository(repository)
            .withObjectKind("push")
            .withUserName("username")
            .build();
    }

    private Project freestyleProject(String name, GiteeCommitStatusPublisher gitLabCommitStatusPublisher) throws IOException {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.setQuietPeriod(5000);
        project.getPublishersList().add(gitLabCommitStatusPublisher);
        project.addProperty(giteeConnectionProperty);
        return project;
    }

    private WorkflowJob workflowJob() throws IOException {
        ItemGroup itemGroup = mock(ItemGroup.class);
        when(itemGroup.getFullName()).thenReturn("parent");
        when(itemGroup.getUrlChildPrefix()).thenReturn("prefix");

        WorkflowJob workflowJob = new WorkflowJob(itemGroup, "workflowJob");
        when(itemGroup.getRootDirFor(workflowJob)).thenReturn(new File("work"));

        workflowJob.addProperty(giteeConnectionProperty);
        workflowJob.setQuietPeriod(5000);
        workflowJob.onCreatedFromScratch();
        return workflowJob;
    }
}
