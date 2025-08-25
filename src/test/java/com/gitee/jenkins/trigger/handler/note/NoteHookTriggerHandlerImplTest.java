package com.gitee.jenkins.trigger.handler.note;

import static com.gitee.jenkins.gitee.hook.model.builder.generated.CommitBuilder.commit;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestLabelBuilder.pullRequestLabel;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.PullRequestObjectAttributesBuilder.pullRequestObjectAttributes;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.NoteHookBuilder.noteHook;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.NoteObjectAttributesBuilder.noteObjectAttributes;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.ProjectBuilder.project;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.UserBuilder.user;
import static com.gitee.jenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.gitee.jenkins.gitee.hook.model.builder.generated.BranchDataBuilder.branchData;
import static com.gitee.jenkins.trigger.filter.PullRequestLabelFilterFactory.newPullRequestLabelFilter;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.gitee.jenkins.gitee.hook.model.NoteAction;
import com.gitee.jenkins.gitee.hook.model.State;
import com.gitee.jenkins.gitee.hook.model.builder.generated.BranchDataBuilder;
import com.gitee.jenkins.trigger.filter.BranchFilterFactory;
import com.gitee.jenkins.trigger.filter.BranchFilterType;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilter;
import com.gitee.jenkins.trigger.filter.BuildInstructionFilterType;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.plugins.git.GitSCM;
import hudson.util.OneShotEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Nikolay Ustinov
 */
@WithJenkins
class NoteHookTriggerHandlerImplTest {

    private static JenkinsRule jenkins;

    @TempDir
    private File tmp;

    private NoteHookTriggerHandler noteHookTriggerHandler;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() {
        noteHookTriggerHandler = new NoteHookTriggerHandlerImpl(false, true, "ci-build", false, false, false);
    }

    @Test
    void note_ciSkip() throws Exception {
        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                EnvVars env = build.getEnvironment(listener);
                assertNull(env.get("gitlabPullRequestLabels"));
                buildTriggered.signal();
                return true;
            }
        });
        Date currentDate = new Date();
        project.setQuietPeriod(0);
        noteHookTriggerHandler.handle(
                project,
                noteHook()
                        .withAction(NoteAction.comment)
                        .withComment(noteObjectAttributes()
                                .withId(1)
                                .withBody("ci-run")
                                .withAuthorId(1)
                                .withProjectId(1)
                                .withCreatedAt(currentDate)
                                .withUpdatedAt(currentDate)
                                .withHtmlUrl("https://gitlab.org/test/pull_requests/1#note_1")
                                .build())
                        .withPullRequest(pullRequestObjectAttributes()
                                .withBody("ci-skip")
                                .build())
                        .build(),
                null, true,
                BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(false));
    }

    @Test
    void note_build() throws Exception {
        Git.init().setDirectory(tmp).call();
        File.createTempFile("test", null, tmp);
        Git git = Git.open(tmp);
        git.add().addFilepattern("test");
        RevCommit commit = git.commit().setSign(false).setMessage("test").call();
        ObjectId head = git.getRepository().resolve(Constants.HEAD);
        // ObjectId base = git.getRepository().resolve(Constants.ORIG_HEAD);
        String repositoryUrl = tmp.toURI().toString();

        final OneShotEvent buildTriggered = new OneShotEvent();
        FreeStyleProject project = jenkins.createFreeStyleProject();
        project.setScm(new GitSCM(repositoryUrl));
        project.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                // EnvVars env = build.getEnvironment(listener);
                // assertEquals("bugfix", env.get("gitlabPullRequestLabels"));
                System.out.println("I AM TRIGGERED");
                buildTriggered.signal();
                return true;
            }
        });
        Date currentDate = new Date();
        project.setQuietPeriod(0);
        System.out.println(commit.getName());
        noteHookTriggerHandler.handle(
                project,
                noteHook()
                        .withSender(user()
                                .withName("test-user-note")
                                .withId(1)
                                .build()
                        )
                        .withAction(NoteAction.comment)
                        .withComment(noteObjectAttributes()
                                .withCommitId(commit.getId().toString())
                                .withId(1)
                                .withBody("ci-build")
                                .withAuthorId(1)
                                .withProjectId(1)
                                .withCreatedAt(currentDate)
                                .withUpdatedAt(currentDate)
                                .withHtmlUrl("https://gitlab.org/test/pull_requests/1#note_1")
                                .build())
                        .withPullRequest(pullRequestObjectAttributes()
                                .withMergeCommitSha(commit.getName())
                                .withMergeable(true)
                                .withId(1)
                                .withTitle("test")
                                .withHead(branchData()
                                        .withUser(user()
                                                .withName("test-user")
                                                .withId(1)
                                                .build()
                                        )
                                        .withRef(git.nameRev().add(head).call().get(head))
                                        .withLabel("bugfix")
                                        .withRepo(project()
                                                .withId(1)
                                                .withName("test")
                                                .withNamespace("test-namespace")
                                                .withHomepage("https://gitlab.org/test")
                                                .withUrl("git@gitlab.org:test.git")
                                                .withSshUrl("git@gitlab.org:test.git")
                                                .withGitHttpUrl(("https://gitee.com/test"))
                                                .build())
                                        .build())
                                .withBase(branchData()
                                        .withUser(user()
                                                .withName("test-user")
                                                .withId(1)
                                                .build()
                                        )
                                        .withRef("784197afe")
                                        .withLabel("master")
                                        .withRepo(project()
                                                .withId(1)
                                                .withName("test")
                                                .withNamespace("test-namespace")
                                                .withHomepage("https://gitlab.org/test")
                                                .withUrl("git@gitlab.org:test.git")
                                                .withSshUrl("git@gitlab.org:test.git")
                                                .withWebUrl("https://gitlab.org/test.git")
                                                .withGitHttpUrl(("https://gitee.com/test"))
                                                .build())
                                        .build())
                                .build())
                .build(),
                null, false, BranchFilterFactory.newBranchFilter(branchFilterConfig().build(BranchFilterType.All)),
                newPullRequestLabelFilter(null));

        buildTriggered.block(10000);
        assertThat(buildTriggered.isSignaled(), is(true));
    }
}
