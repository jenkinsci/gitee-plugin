package com.gitee.jenkins.trigger.handler;

import static com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestObjectAttributesBuilder.pullRequestObjectAttributes;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.ActionDesc;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.gitee.hook.model.State;
import com.gitee.jenkins.gitee.hook.model.builder.generated.BranchDataBuilder;
import com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestHookBuilder;
import com.gitee.jenkins.gitee.hook.model.builder.generated.ProjectBuilder;
import com.gitee.jenkins.gitee.hook.model.builder.generated.UserBuilder;
import com.gitee.jenkins.trigger.filter.BranchFilterType;

import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.Project;
import hudson.model.Queue;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@WithJenkins
@ExtendWith(MockitoExtension.class)
class PendingBuildsHandlerTest {

    private static final String GITEE_BUILD_NAME = "Jenkins";

    private static JenkinsRule jenkins;

    @Mock
    private GiteeConnectionProperty giteeConnectionProperty;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }


    @AfterEach
    void tearDown() {
        Queue queue = jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            queue.cancel(item);
        }
    }

    @Test
    void queuedMergeRequestBuildsCanBeCancelledOnMergeRequestUpdate() throws Exception {
        Project project = freestyleProject("project1");

        GiteePushTrigger giteePushTrigger = giteePushTrigger(project);
        giteePushTrigger.setCancelPendingBuildsOnUpdate(true);

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(0));

        List<String> queuedCommits = List.of("commit3", "commit4", "commit5");

        giteePushTrigger.onPost(pullRequestHook(1, "sourceBranch", "commit1")); // Will be cancelled
        giteePushTrigger.onPost(pullRequestHook(1, "sourceBranch", "commit2")); // Will be cancelled
        giteePushTrigger.onPost(pullRequestHook(1, "sourceBranch", "commit3"));
        giteePushTrigger.onPost(pullRequestHook(1, "anotherBranch", "commit4"));
        giteePushTrigger.onPost(pullRequestHook(2, "sourceBranch", "commit5"));

        
        for (Queue.Item i: jenkins.getInstance().getQueue().getItems()) {
            for (Cause c : i.getCauses()) {
                GiteeWebHookCause w = (GiteeWebHookCause) c;
                assertThat(queuedCommits.contains(w.getData().getLastCommit()), is(true));
            }
        }

        assertThat(jenkins.getInstance().getQueue().getItems().length, is(3));
    }

    private GiteePushTrigger giteePushTrigger(Project project) throws Exception {
        GiteePushTrigger giteePushTrigger = giteePushTrigger();
        project.addTrigger(giteePushTrigger);
        giteePushTrigger.start(project, true);
        return giteePushTrigger;
    }

    private GiteePushTrigger giteePushTrigger() {
        GiteePushTrigger giteePushTrigger = new GiteePushTrigger();
        giteePushTrigger.setTriggerOnPush(true);
        giteePushTrigger.setTriggerOnAcceptedPullRequest(true);
        giteePushTrigger.setTriggerOnApprovedPullRequest(true);
        giteePushTrigger.setPendingBuildName(GITEE_BUILD_NAME);
        giteePushTrigger.setBranchFilterType(BranchFilterType.NameBasedFilter);
        giteePushTrigger.setBranchFilterName("");
        return giteePushTrigger;
    }

    private PullRequestHook pullRequestHook(int projectId, String branch, String commitId) {
        return PullRequestHookBuilder.pullRequestHook()
                .withSender(UserBuilder.user()
                    .withId(1)
                    .withName("test-user")
                    .withUsername("test")
                    .build())
                .withAction(Action.update)
                .withActionDesc(ActionDesc.target_branch_changed)
                .withState(State.updated)
                .withRepo(ProjectBuilder.project()
                    .withId(1)
                    .withName("test")
                    .withNamespace("test-namespace")
                    .withHomepage("https://gitee.com/test")
                    .withUrl("git@gitee.com:test.git")
                    .withSshUrl("git@gitee.com:test.git")
                    .withGitHttpUrl("https://gitee.com/test.git")
                    .withWebUrl("https://gitee.com/test.git")
                    .build())
                .withPullRequest(pullRequestObjectAttributes()
                        .withMergeCommitSha(commitId)
                        .withId(projectId)
                        .withMergeReferenceName("testRefName")
                        .withTitle("test")
                        .withMergeable(true)
                        .withBase(BranchDataBuilder.branchData()
                            .withLabel(branch)
                            .withRepo(ProjectBuilder.project()
                                .withId(projectId)
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitee.com/test")
                                .withUrl("git@gitee.com:test.git")
                                .withSshUrl("git@gitee.com:test.git")
                                .withGitHttpUrl("https://gitee.com/test.git")
                                .withWebUrl("https://gitee.com/test.git")
                                .build())
                            .withRef("")
                            .withUser(UserBuilder.user()
                                .withId(1)
                                .withName("test-user")
                                .withUsername("test")
                                .build())
                            .build())
                        .withHead(BranchDataBuilder.branchData()
                            .withLabel(branch)
                            .withRepo(ProjectBuilder.project()
                                .withId(projectId)
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitee.com/test")
                                .withUrl("git@gitee.com:test.git")
                                .withSshUrl("git@gitee.com:test.git")
                                .withGitHttpUrl("https://gitee.com/test.git")
                                .withWebUrl("https://gitee.com/test.git")
                                .build())
                            .withRef(branch)
                            .withUser(UserBuilder.user()
                                .withId(1)
                                .withName("test-user")
                                .withUsername("test")
                                .build())
                            .build())
                        .build())
                .build();
    }

    private Project freestyleProject(String name)
            throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject(name);
        project.setQuietPeriod(5000);
        // project.getPublishersList().add(giteeCommitStatusPublisher);
        project.addProperty(giteeConnectionProperty);
        return project;
    }
}

