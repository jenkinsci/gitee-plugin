package com.gitee.jenkins.trigger;

import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import hudson.model.FreeStyleProject;
import hudson.model.Job;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

@WithJenkins
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class GiteePushTriggerTest {

    private JenkinsRule jenkins;
    private GiteePushTrigger trigger;

    @Mock
    private StaplerRequest2 mockRequest;

    @Mock 
    private Ancestor mockAncestor;

    private MockedStatic<Stapler>  threadRequest;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        jenkins = rule;
        trigger = new GiteePushTrigger();

        threadRequest = Mockito.mockStatic(Stapler.class);
    }

    @AfterEach
    void tearDown() {
        threadRequest.close();
    }

    @Test 
    void retrieveDisplayName() throws IOException {
        String projectName = "test-proj";
        FreeStyleProject proj = jenkins.createFreeStyleProject(projectName);
        Mockito.when(mockRequest.findAncestor(Job.class))
            .thenReturn(mockAncestor);

        Mockito.when(mockAncestor.getObject()).thenReturn((Job<?,?>) proj);
        threadRequest.when(() -> Stapler.getCurrentRequest2())
            .thenReturn(mockRequest);
        
        assertTrue(trigger.getDescriptor().getDisplayName().contains(projectName));
    }

    @Test
    void retrieveUnknownDisplayName() {
        assertTrue(trigger.getDescriptor().getDisplayName().contains("unknown"));
    }
}
