package com.gitee.jenkins.webhook.build;

import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.hook.model.PipelineHook;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static com.gitee.jenkins.cause.CauseDataBuilder.causeData;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Milena Zachow
 */
@RunWith(MockitoJUnitRunner.class)
public class PipelineBuildActionTest {

    @ClassRule
    public static JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private StaplerResponse response;

    @Mock
    private GiteePushTrigger trigger;

    FreeStyleProject testProject;

    @Before
    public void setUp() throws IOException{
        testProject = jenkins.createFreeStyleProject();
        testProject.addTrigger(trigger);
    }

    @Test
    public void buildOnSuccess () throws IOException {
        exception.expect(HttpResponses.HttpResponseException.class);
        new PipelineBuildAction(testProject, getJson("PipelineEvent.json"), null).execute(response);

        verify(trigger).onPost(any(PipelineHook.class));
    }

    @Test
    public void doNotBuildOnFailure() throws IOException {
        exception.expect(HttpResponses.HttpResponseException.class);
        new PipelineBuildAction(testProject, getJson("PipelineFailureEvent.json"), null).execute(response);

        verify(trigger, never()).onPost(any(PipelineHook.class));
    }

    private String getJson(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name));
    }
}
