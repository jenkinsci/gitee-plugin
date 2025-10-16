package com.gitee.jenkins.trigger.handler.pull;

import static com.gitee.jenkins.gitee.hook.model.builder.generated.BranchDataBuilder.branchData;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestHookBuilder.pullRequestHook;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestObjectAttributesBuilder.pullRequestObjectAttributes;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.ProjectBuilder.project;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.UserBuilder.user;
import static com.gitee.jenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.gitee.jenkins.trigger.filter.PullRequestLabelFilterFactory.newPullRequestLabelFilter;
import static com.gitee.jenkins.trigger.handler.pull.PullRequestHookTriggerHandlerFactory.newPullRequestHookTriggerHandler;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.gitee.api.model.User;
import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.ActionDesc;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.gitee.hook.model.State;
import com.gitee.jenkins.gitee.hook.model.builder.generated.BranchDataBuilder;
import com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestHookBuilder;
import com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestObjectAttributesBuilder;
import com.gitee.jenkins.publisher.GiteeMessagePublisher;
import com.gitee.jenkins.trigger.filter.BranchFilterFactory;
import com.gitee.jenkins.trigger.filter.BranchFilterType;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilterType;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/**
 * @author Robin Müller
 */
@WithJenkins
class PullRequestHookTriggerHandlerImplTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
        jenkins.timeout = 450;
    }

    @Test
    void pullRequest_ciSkip() throws Exception {
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        assertThat(ciSkipTestHelper("enable build", "enable build", buildHolder), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
        assertThat(ciSkipTestHelper("garbage [ci-skip] garbage", "enable build", buildHolder), is(false));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_opened_with_source() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                true,
                "1",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
                
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, Action.open, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_do_not_build_when_is_not_mergeable_client_response() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        
        MockedStatic<GiteeConnectionProperty> config = Mockito.mockStatic(GiteeConnectionProperty.class);
        MockedStatic<GiteeMessagePublisher> publisher = Mockito.mockStatic(GiteeMessagePublisher.class);
        TestGiteeClient client = new TestGiteeClient();

        config.when(() -> GiteeConnectionProperty.getClient(project)).thenReturn(client);
        publisher.when(() -> GiteeMessagePublisher.getFromJob(project)).thenReturn(new TestPublisher());
        
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                true,
                "1",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
                
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, Action.open, buildHolder, false, project);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertThat(client.getMessage(), is(":bangbang: This pull request can not be merge! The build will not be triggered. Please manual merge conflict."));
    }

    @Test
    void pullRequest_build_when_opened_with_both() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                true, 
                "true",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, Action.open, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_opened_with_never() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                true,
                "false",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.opened, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void pullRequest_build_when_reopened() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                true,
                "true",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.reopened, Action.update, ActionDesc.source_branch_changed, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_opened_with_approved_action_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_accepted() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.merged, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_accepted_with_approved_action_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.merged, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_closed() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_close() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                false, 
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.open, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_closed_with_actions_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_do_not_build_for_accepted_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.merged);
    }

    @Test
    void pullRequest_do_not_build_for_updated_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.updated);
    }

    @Test
    void pullRequest_do_not_build_for_reopened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.reopened);
    }

    @Test
    void pullRequest_do_not_build_for_opened_when_nothing_enabled() throws Exception {
        do_not_build_for_state_when_nothing_enabled(State.opened);
    }

    @Test
    void pullRequest_do_not_build_when_accepted_some_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.merged, Action.merge, ActionDesc.source_branch_changed, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void pullRequest_build_for_accepted_state_when_approved_action_triggered() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "false",
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.merged, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_do_not_build_when_closed() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.closed, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void pullRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "false",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
                
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void pullRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not()
            throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void pullRequest_build_for_update_state_when_updated_state_and_approved_action_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled()
            throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.update, ActionDesc.source_branch_changed, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled()
            throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    @Test
    void pullRequest_build_for_update_state_when_updated_state_and_pull_action() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                true,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);


        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.merge, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "false",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);

        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_for_approved_action_when_only_approved_enabled() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "false",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered =
                doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_only_when_approved_and_not_when_updated() throws Exception {
        pullRequest_build_only_when_approved(Action.update);
    }

    @Test
    void pullRequest_build_only_when_approved_and_not_when_opened() throws Exception {
        pullRequest_build_only_when_approved(Action.open);
    }

    @Test
    void pullRequest_build_only_when_approved_and_not_when_pull() throws Exception {
        pullRequest_build_only_when_approved(Action.merge);
    }

    @Test
    void pullRequest_build_only_when_state_modified() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "1",
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false);

        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        PullRequestObjectAttributesBuilder objectAttributes =
                defaultPullRequestObjectAttributes("refs/heads/" + git.nameRev().add(head).call().get(head));
        pullRequestHookTriggerHandler.handle(
                project,
                pullRequestHook()
                        .withState(State.open)
                        .withAction(Action.update)
                        .withActionDesc(ActionDesc.source_branch_changed)
                        .withSender(user()
                                .withId(1)
                                .withName("test-sender")
                                .build())
                        .withPullRequest(objectAttributes
                                .withMergeCommitSha(commit.getName())
                                .withMergeable(true)
                                .build())
                        .withRepo(project().withWebUrl("www.gitee.com").build())
                        .build(),
                null, true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        PullRequestObjectAttributesBuilder objectAttributes2 =
                defaultPullRequestObjectAttributes("refs/heads/"
                                        + git.nameRev().add(head).call().get(head));
        pullRequestHookTriggerHandler.handle(
                project,
                pullRequestHook()
                        .withState(State.merged)
                        .withAction(Action.merge)
                        .withSender(user()
                                .withId(1)
                                .withName("test-sender")
                                .build())
                        .withPullRequest(objectAttributes2
                                .withMergeCommitSha(commit.getName())
                                .withMergeable(true)
                                .build())
                        .withRepo(project().withWebUrl("www.gitee.com").build())
                        .build(),
                null, true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    private void do_not_build_for_state_when_nothing_enabled(State state) throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "false",
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
        
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, state, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    private void pullRequest_build_only_when_approved(Action action) throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(
                false,
                "false",
                false,
                false,
                false,
                true,
                false,
                false,
                false,
                false,
                false);
        
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, action, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(false));
        assertNull(buildHolder.get());
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                pullRequestHook().withAction(action),
                buildHolder,
                true,
                null);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            State state,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                pullRequestHook().withState(state),
                buildHolder,
                true,
                null);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            State state,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                pullRequestHook().withState(state).withAction(action),
                buildHolder,
                true,
                null);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            State state,
            Action action,
            ActionDesc desc,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                pullRequestHook().withState(state).withAction(action).withActionDesc(desc),
                buildHolder,
                true,
                null);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            State state,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder,
            boolean isMergeable,
            FreeStyleProject project)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                pullRequestHook().withState(state).withAction(action),
                buildHolder,
                isMergeable,
                project);
     }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            PullRequestHookBuilder builder,
            AtomicReference<FreeStyleBuild> buildHolder,
            boolean isMergeable,
            FreeStyleProject project)
            throws Exception {
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        String repositoryUrl = tmp.toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        if (project == null) {
            project = jenkins.createFreeStyleProject();
        }
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });

        PullRequestHook hook = builder
                        .withPullRequest(defaultPullRequestObjectAttributes(("refs/heads/" + git.nameRev().add(head).call().get(head)))
                                .withMergeCommitSha(commit.getName())
                                .withMergeable(isMergeable)
                                .build()) 
                        .withRepo(project()
                                .withId(3)
                                .withWebUrl("https://gitee.com/test.git")
                                .build())
                        .withSender(user()
                                .withId(1)
                                .withName("test-sender")
                                .build())
                        .build();

        hook.getPullRequest().getBase().getRepo().getId();

        project.setQuietPeriod(0);
        pullRequestHookTriggerHandler.handle(
                project,
                hook,
                null, true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered;
    }

    private boolean ciSkipTestHelper(
            String MRDescription, String lastCommitMsg, AtomicReference<FreeStyleBuild> buildHolder) throws Exception {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = new PullRequestHookTriggerHandlerImpl(
                Arrays.asList(State.opened, State.reopened), Arrays.asList(Action.approved), null, false, false, false, false, false);
        pullRequestHookTriggerHandler.handle(
                project,
                pullRequestHook()
                        .withAction(Action.approved)
                        .withState(State.opened)
                        .withSender(user()
                                .withId(1)
                                .withName("test-sender")
                                .build())
                        .withRepo(project()
                                .withId(1)
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitee.com/test")
                                .withUrl("git@gitee.com:test.git")
                                .withSshUrl("git@gitee.com:test.git")
                                .withGitHttpUrl("https://gitee.com/test.git")
                                .withPathWithNamespace("/test")
                                .build())
                        .withPullRequest(defaultPullRequestObjectAttributes("test")
                                .withBody(MRDescription)
                                .withMergeCommitSha(lastCommitMsg)
                                .withMergeable(true)
                                .build())
                        .build(),
                BuildInstructionFilterType.CI_SKIP, false, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered.isSignaled();
    }

    private PullRequestObjectAttributesBuilder defaultPullRequestObjectAttributes(String ref) {
        BranchDataBuilder headBuilder = branchData()
                        .withLabel("master")
                        .withUser(user()
                                .withName("test-user")
                                .withId(1)
                                .build())
                        .withRepo(project()
                                .withId(1)
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitee.com/test")
                                .withUrl("git@gitee.com:test.git")
                                .withSshUrl("git@gitee.com:test.git")
                                .withGitHttpUrl("https://gitee.com/test.git")
                                .withPathWithNamespace("/test")
                                .build());
                                
        if (ref != null && !ref.equals("")) {
                headBuilder.withRef(ref);
        }

        return pullRequestObjectAttributes()
                .withId(1)
                .withTitle("test")
                .withBase(branchData()
                        .withLabel("master")
                        .withRef("aaaaaaa")
                        .withUser(user()
                                .withName("test-user")
                                .withId(1)
                                .build())
                        .withRepo(project()
                                .withId(2)
                                .withName("test")
                                .withNamespace("test-namespace")
                                .withHomepage("https://gitee.com/test")
                                .withUrl("git@gitee.com:test.git")
                                .withSshUrl("git@gitee.com:test.git")
                                .withGitHttpUrl("https://gitee.com/test.git")
                                .withPathWithNamespace("/test")
                                .build())
                        .build())
                .withHead(headBuilder.build());
    }

    private class TestGiteeClient implements GiteeClient {
        private String message;

        public TestGiteeClient() {

        }

        public String getMessage() {
            return message;
        }

        @Override
        public String getHostUrl() {
            throw new UnsupportedOperationException("Unimplemented method 'getHostUrl'");
        }

        @Override
        public void acceptPullRequest(PullRequest mr, String mergeCommitMessage, boolean shouldRemoveSourceBranch) {
            throw new UnsupportedOperationException("Unimplemented method 'acceptPullRequest'");
        }

        @Override
        public void createPullRequestNote(PullRequest mr, String body) {
            message = body;
        }

        @Override
        public User getCurrentUser() {
            throw new UnsupportedOperationException("Unimplemented method 'getCurrentUser'");
        }

        @Override
        public void createPullRequest(String owner, String repo, String title, String base, String head) {
                throw new UnsupportedOperationException("Unimplemented method 'createPullRequest'");
        }
    }

    private class TestPublisher extends GiteeMessagePublisher {
        public TestPublisher() {
            super();
        }
    }
}
