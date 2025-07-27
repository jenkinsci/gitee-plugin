package com.gitee.jenkins.environment;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.CauseDataBuilder;
import com.gitee.jenkins.cause.GiteeWebHookCause;

import hudson.EnvVars;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.matrix.MatrixRun;
import hudson.matrix.TextAxis;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.StreamBuildListener;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author Evgeni Golov
 */
@WithJenkins
class GiteeEnvironmentContributorTest {

    private static JenkinsRule jenkins;

    private BuildListener listener;

    @BeforeAll
    static void setUp(JenkinsRule rule) {
        jenkins = rule;
    }

    @BeforeEach
    void setUp() {
        listener = new StreamBuildListener(jenkins.createTaskListener().getLogger(), Charset.defaultCharset());
    }

    @Test
    void freeStyleProjectTest() throws Exception {
        FreeStyleProject p = jenkins.createFreeStyleProject();
        GiteeWebHookCause cause = new GiteeWebHookCause(generateCauseData());
        FreeStyleBuild b = p.scheduleBuild2(0, cause).get();
        EnvVars env = b.getEnvironment(listener);

        assertEnv(env);
    }

    @Test
    void matrixProjectTest() throws Exception {
        EnvVars env;
        MatrixProject p = jenkins.jenkins.createProject(MatrixProject.class, "matrixbuild");
        GiteeWebHookCause cause = new GiteeWebHookCause(generateCauseData());
        // set up 2x2 matrix
        AxisList axes = new AxisList();
        axes.add(new TextAxis("db", "mysql", "oracle"));
        axes.add(new TextAxis("direction", "north", "south"));
        p.setAxes(axes);

        MatrixBuild build = p.scheduleBuild2(0, cause).get();
        List<MatrixRun> runs = build.getRuns();
        assertEquals(4, runs.size());
        for (MatrixRun run : runs) {
            env = run.getEnvironment(listener);
            assertNotNull(env.get("db"));
            assertEnv(env);
        }
    }

    private CauseDataBuilder generateCauseDataBase() {
        return causeData()
                .withActionType(CauseData.ActionType.MERGE)
                .withSourceProjectId(1)
                .withTargetProjectId(1)
                .withBranch("feature")
                .withSourceBranch("feature")
                .withUserName("")
                .withSourceRepoHomepage("https://gitee.com/test")
                .withSourceRepoName("test")
                .withSourceNamespace("test-namespace")
                .withSourceRepoUrl("git@gitee.com:test.git")
                .withSourceRepoSshUrl("git@gitee.org:test.git")
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
                .withTargetProjectUrl("https://gitee.com/test");
    }

    private CauseData generateCauseData() {
        return generateCauseDataBase().build();
    }

    private void assertEnv(EnvVars env) {
        assertEquals("1", env.get("giteePullRequestId"));
        assertEquals("git@gitee.com:test.git", env.get("giteeSourceRepoUrl"));
        assertEquals("master", env.get("giteeTargetBranch"));
        assertEquals("test", env.get("giteeTargetRepoName"));
        assertEquals("feature", env.get("giteeSourceBranch"));
        assertEquals("test", env.get("giteeSourceRepoName"));
    }
}
