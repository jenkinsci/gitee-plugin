package com.gitee.jenkins.trigger.handler.pull;

import static com.gitee.jenkins.gitee.hook.model.builder.generated.CommitBuilder.commit;
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

import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.State;
import com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestObjectAttributesBuilder;
import com.gitee.jenkins.trigger.filter.BranchFilterFactory;
import com.gitee.jenkins.trigger.filter.BranchFilterType;
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
        assertThat(ciSkipTestHelper("enable build", "garbage [ci-skip] garbage", buildHolder), is(false));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

    @Test
    void pullRequest_build_when_opened_with_source() throws Exception {
        PullRequestHookTriggerHandler pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(true, "false", false, false, false, false, false, false, false, false, false);
        final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
        OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, buildHolder);

        assertThat(buildTriggered.isSignaled(), is(true));
        jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
    }

//     @Test
//     void pullRequest_build_when_opened_with_both() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.source)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_opened_with_never() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.never)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.opened, Action.update, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_build_when_reopened() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.reopened, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_opened_with_approved_action_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnApprovedPullRequest(true)
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.source)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_accepted() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnAcceptedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.pulld, Action.pull, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_accepted_with_approved_action_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnAcceptedPullRequest(true)
//                 .setTriggerOnApprovedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.pulld, Action.pull, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_closed() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnClosedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_close() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnClosedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, Action.close, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_closed_with_actions_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnClosedPullRequest(true)
//                 .setTriggerOnApprovedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.closed, Action.close, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_do_not_build_for_accepted_when_nothing_enabled() throws Exception {
//         do_not_build_for_state_when_nothing_enabled(State.pulld);
//     }

//     @Test
//     void pullRequest_do_not_build_for_updated_when_nothing_enabled() throws Exception {
//         do_not_build_for_state_when_nothing_enabled(State.updated);
//     }

//     @Test
//     void pullRequest_do_not_build_for_reopened_when_nothing_enabled() throws Exception {
//         do_not_build_for_state_when_nothing_enabled(State.reopened);
//     }

//     @Test
//     void pullRequest_do_not_build_for_opened_when_nothing_enabled() throws Exception {
//         do_not_build_for_state_when_nothing_enabled(State.opened);
//     }

//     @Test
//     void pullRequest_do_not_build_when_accepted_some_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.source)
//                 .setTriggerOnApprovedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.pulld, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_build_for_accepted_state_when_approved_action_triggered() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnApprovedPullRequest(true)
//                 .setTriggerOnAcceptedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.pulld, Action.approved, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_do_not_build_when_closed() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.source)
//                 .setTriggerOnApprovedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.closed, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_do_not_build_for_updated_state_and_approved_action_when_both_not_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_do_not_build_for_updated_state_and_approved_action_when_updated_enabled_but_approved_not()
//             throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_build_for_update_state_when_updated_state_and_approved_action_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnApprovedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_for_update_state_and_action_when_updated_state_and_approved_action_enabled()
//             throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnApprovedPullRequest(true)
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.source)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_do_not_build_for_update_state_and_action_when_opened_state_and_approved_action_enabled()
//             throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnApprovedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_build_for_update_state_when_updated_state_and_pull_action() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnAcceptedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.pull, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_for_approved_action_when_opened_state_and_approved_action_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnApprovedPullRequest(true).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);
//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_for_approved_action_when_only_approved_enabled() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnPullRequest(false)
//                 .setTriggerOnApprovedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.approved, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_new_commits_were_pushed_state_opened_action_open() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnPullRequest(true)
//                 .setTriggerOnlyIfNewCommitsPushed(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, State.opened, Action.open, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_new_commits_were_pushed_state_reopened_action_reopen() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnPullRequest(true)
//                 .setTriggerOnlyIfNewCommitsPushed(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.reopened, Action.reopen, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void pullRequest_build_when_new_commits_were_pushed_do_not_build_without_commits() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnPullRequest(true)
//                 .setTriggerOnlyIfNewCommitsPushed(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.updated, Action.update, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     @Test
//     void pullRequest_build_only_when_approved_and_not_when_updated() throws Exception {
//         pullRequest_build_only_when_approved(Action.update);
//     }

//     @Test
//     void pullRequest_build_only_when_approved_and_not_when_opened() throws Exception {
//         pullRequest_build_only_when_approved(Action.open);
//     }

//     @Test
//     void pullRequest_build_only_when_approved_and_not_when_pull() throws Exception {
//         pullRequest_build_only_when_approved(Action.pull);
//     }

//     @Test
//     void pullRequest_build_only_when_state_modified() throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnAcceptedPullRequest(true)
//                 .setTriggerOnClosedPullRequest(true)
//                 .setTriggerOpenPullRequest(TriggerOpenPullRequest.source)
//                 .build();
//         Git.init().setDirectory(tmp).call();
//         File.createTempFile("test", null, tmp);
//         Git git = Git.open(tmp);
//         git.add().addFilepattern("test");
//         RevCommit commit = git.commit().setSign(false).setMessage("test").call();
//         ObjectId head = git.getRepository().resolve(Constants.HEAD);
//         String repositoryUrl = tmp.toURI().toString();

//         final OneShotEvent buildTriggered = new OneShotEvent();
//         FreeStyleProject project = jenkins.createFreeStyleProject();
//         project.setScm(new GitSCM(repositoryUrl));
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         project.getBuildersList().add(new TestBuilder() {
//             @Override
//             public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
//                 buildHolder.set((FreeStyleBuild) build);
//                 buildTriggered.signal();
//                 return true;
//             }
//         });
//         project.setQuietPeriod(0);
//         PullRequestObjectAttributesBuilder objectAttributes =
//                 defaultPullRequestObjectAttributes().withAction(Action.update);
//         pullRequestHookTriggerHandler.handle(
//                 project,
//                 pullRequestHook()
//                         .withObjectAttributes(objectAttributes
//                                 .withTargetBranch("refs/heads/"
//                                         + git.nameRev().add(head).call().get(head))
//                                 .withLastCommit(commit().withAuthor(
//                                                 user().withName("test").build())
//                                         .withId(commit.getName())
//                                         .build())
//                                 .build())
//                         .withProject(project()
//                                 .withWebUrl("https://gitlab.org/test.git")
//                                 .build())
//                         .build(),
//                 true,
//                 BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
//                 newPullRequestLabelFilter(null));

//         buildTriggered.block(10000);
//         assertThat(buildTriggered.isSignaled(), is(true));
//         PullRequestObjectAttributesBuilder objectAttributes2 =
//                 defaultPullRequestObjectAttributes().withState(State.pulld).withAction(Action.pull);
//         pullRequestHookTriggerHandler.handle(
//                 project,
//                 pullRequestHook()
//                         .withObjectAttributes(objectAttributes2
//                                 .withTargetBranch("refs/heads/"
//                                         + git.nameRev().add(head).call().get(head))
//                                 .withLastCommit(commit().withAuthor(
//                                                 user().withName("test").build())
//                                         .withId(commit.getName())
//                                         .build())
//                                 .build())
//                         .withProject(project()
//                                 .withWebUrl("https://gitlab.org/test.git")
//                                 .build())
//                         .build(),
//                 true,
//                 BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
//                 newPullRequestLabelFilter(null));

//         buildTriggered.block(10000);
//         assertThat(buildTriggered.isSignaled(), is(true));
//         jenkins.assertBuildStatusSuccess(jenkins.waitForCompletion(buildHolder.get()));
//     }

//     @Test
//     void
//             pullRequest_skips_build_when_not_push_and_not_pull_request_and_accepted_and_trigger_open_pull_request_unspecified()
//                     throws Exception {
//         GitLabPushTrigger gitLabPushTrigger = new GitLabPushTrigger();
//         gitLabPushTrigger.setTriggerOnPush(false);
//         gitLabPushTrigger.setTriggerOnPullRequest(false);
//         gitLabPushTrigger.setTriggerOnAcceptedPullRequest(true);
//         gitLabPushTrigger.setBranchFilterType(BranchFilterType.All);

//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 PullRequestHookTriggerHandlerFactory.newPullRequestHookTriggerHandler(gitLabPushTrigger);
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered =
//                 doHandle(pullRequestHookTriggerHandler, State.opened, Action.update, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     private void do_not_build_for_state_when_nothing_enabled(State state) throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler =
//                 withConfig().setTriggerOnPullRequest(false).build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, state, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

//     private void pullRequest_build_only_when_approved(Action action) throws Exception {
//         PullRequestHookTriggerHandler pullRequestHookTriggerHandler = withConfig()
//                 .setTriggerOnPullRequest(false)
//                 .setTriggerOnApprovedPullRequest(true)
//                 .build();
//         final AtomicReference<FreeStyleBuild> buildHolder = new AtomicReference<>();
//         OneShotEvent buildTriggered = doHandle(pullRequestHookTriggerHandler, action, buildHolder);

//         assertThat(buildTriggered.isSignaled(), is(false));
//         assertNull(buildHolder.get());
//     }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                defaultPullRequestObjectAttributes().withAction(action),
                buildHolder);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            State state,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                defaultPullRequestObjectAttributes().withState(state),
                buildHolder);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            State state,
            Action action,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
        return doHandle(
                pullRequestHookTriggerHandler,
                defaultPullRequestObjectAttributes().withState(state).withAction(action),
                buildHolder);
    }

    private OneShotEvent doHandle(
            PullRequestHookTriggerHandler pullRequestHookTriggerHandler,
            PullRequestObjectAttributesBuilder objectAttributes,
            AtomicReference<FreeStyleBuild> buildHolder)
            throws Exception {
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
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
                buildHolder.set((FreeStyleBuild) build);
                buildTriggered.signal();
                return true;
            }
        });
        project.setQuietPeriod(0);
        pullRequestHookTriggerHandler.handle(
                project,
                pullRequestHook()
                        .withObjectAttributes(objectAttributes
                                .withTargetBranch("refs/heads/"
                                        + git.nameRev().add(head).call().get(head))
                                .withLastCommit(commit().withAuthor(
                                                user().withName("test").build())
                                        .withId(commit.getName())
                                        .build())
                                .build())
                        .withProject(project()
                                .withWebUrl("https://gitlab.org/test.git")
                                .build())
                        .build(),
                true,
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
                        .withObjectAttributes(defaultPullRequestObjectAttributes()
                                .withDescription(MRDescription)
                                .withLastCommit(commit().withMessage(lastCommitMsg)
                                        .withAuthor(user().withName("test").build())
                                        .withId("testid")
                                        .build())
                                .build())
                        .build(),
                true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        return buildTriggered.isSignaled();
    }

    private PullRequestObjectAttributesBuilder defaultPullRequestObjectAttributes() {
        return pullRequestObjectAttributes()
                .withId(1)
                .withAction(Action.open)
                .withState(State.opened)
                .withTitle("test")
                .withTargetProjectId(1)
                .withSourceProjectId(1)
                .withSourceBranch("feature")
                .withTargetBranch("master")
                .withSource(project()
                        .withName("test")
                        .withNamespace("test-namespace")
                        .withHomepage("https://gitlab.org/test")
                        .withUrl("git@gitlab.org:test.git")
                        .withSshUrl("git@gitlab.org:test.git")
                        .withGitHttpUrl("https://gitlab.org/test.git")
                        .build())
                .withTarget(project()
                        .withName("test")
                        .withNamespace("test-namespace")
                        .withHomepage("https://gitlab.org/test")
                        .withUrl("git@gitlab.org:test.git")
                        .withSshUrl("git@gitlab.org:test.git")
                        .withGitHttpUrl("https://gitlab.org/test.git")
                        .build());
    }
}
