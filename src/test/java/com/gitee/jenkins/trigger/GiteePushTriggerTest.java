package com.gitee.jenkins.trigger;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.gitee.hook.model.PipelineHook;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.gitee.hook.model.PushHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.stapler.StaplerRequest2;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@WithJenkins
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GiteePushTriggerTest {

    private JenkinsRule jenkins;
    private GiteePushTrigger trigger;


    @Mock
    private StaplerRequest2 request;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
        trigger = new GiteePushTrigger();
    }

    @Test
    void initNullPushHookOnPost() throws Exception {
        PushHook hook = null;
        trigger.setTriggerOnPush(false);
        
        assertDoesNotThrow(() -> trigger.onPost(hook));
    }

    @Test
    void initNullPullHookOnPost() throws Exception {
        PullRequestHook hook = null;
        trigger.setTriggerOnAcceptedPullRequest(false);
        trigger.setTriggerOnOpenPullRequest(false);
        trigger.setTriggerOnClosedPullRequest(false);
        trigger.setTriggerOnUpdatePullRequest("false");
        trigger.setTriggerOnApprovedPullRequest(false);
        trigger.setTriggerOnTestedPullRequest(false);

        assertDoesNotThrow(() -> trigger.onPost(hook));
    }

    @Test
    void initNullNoteHookOnPost() throws Exception {
        NoteHook hook = null;
        trigger.setTriggerOnNoteRequest(false);

        assertDoesNotThrow(() -> trigger.onPost(hook));
    }

    @Test
    void initNullPipelineHookOnPost() throws Exception {
        trigger.setTriggerOnPipelineEvent(false);
        PipelineHook hook = null;

        assertDoesNotThrow(() -> trigger.onPost(hook));        
    }
}
