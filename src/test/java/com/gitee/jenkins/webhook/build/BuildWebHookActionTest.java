/**
 * Adapted from GitLab automated tests
 * https://github.com/jenkinsci/gitlab-plugin/tree/master/src/test/java/com/dabsquared/gitlabjenkins
 */

package com.gitee.jenkins.webhook.build;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.kohsuke.stapler.HttpResponses.HttpResponseException;
import org.springframework.security.core.Authentication;

import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Project;
import hudson.security.ACL;


/**
 * Test the BuildWebHookAction class
 *
 * @author Mark Waite
 */
@WithJenkins
public class BuildWebHookActionTest {
    private JenkinsRule j;

    private FreeStyleProject project;
    private GiteePushTrigger trigger;

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        j = rule;
        j.get(GiteeConnectionConfig.class).setUseAuthenticatedEndpoint(true);

        project = j.createFreeStyleProject();
        trigger = new GiteePushTrigger();
        project.addTrigger(trigger);
    }

    @Test
    void testNotifierTokenMatches() {
        String triggerToken = "testNotifierTokenMatches-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = triggerToken;
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        action.runNotifier();
        assertTrue(action.performOnPostCalled, "performOnPost not called, token did not match?");
    }

    @Test
        void testNotifierTokenDoesNotMatchString() {
        String triggerToken = "testNotifierTokenDoesNotMatchString-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = triggerToken + "-no-match"; // Won't match
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        assertThrows(HttpResponseException.class, action::runNotifier);
        assertFalse(action.performOnPostCalled, "performOnPost was called, unexpected token match?");
    }

    @Test
    void testNotifierTokenDoesNotMatchNull() {
        String triggerToken = "testNotifierTokenDoesNotMatchNull-token";
        trigger.setSecretToken(triggerToken);
        String actionToken = null;
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        assertThrows(HttpResponseException.class, action::runNotifier);
        assertFalse(action.performOnPostCalled, "performOnPost was called, unexpected token match?");
    }

    @Test
    void testNullNotifierTokenAllowsAccess() {
        assertNull(trigger.getSecretToken());
        String actionToken = "testNullNotifierTokenAllowsAccess-token";
        BuildWebHookActionImpl action = new BuildWebHookActionImpl(project, actionToken);
        action.runNotifier();
        assertTrue(action.performOnPostCalled, "performOnPost not called, token did not match?");
    }

    public class BuildWebHookActionImpl extends BuildWebHookAction {

        // Used for the assertion that tokenMatches() returned true
        public boolean performOnPostCalled = false;

        private final MyTriggerNotifier myNotifier;

        public BuildWebHookActionImpl() {
            myNotifier = new MyTriggerNotifier(null, null, null);
        }

        public BuildWebHookActionImpl(@NonNull Project project, @NonNull String token) {
            myNotifier = new MyTriggerNotifier(project, token, ACL.SYSTEM2);
        }

        public void runNotifier() {
            myNotifier.run();
        }

        public class MyTriggerNotifier extends TriggerNotifier {

            public MyTriggerNotifier(Item project, String secretToken, Authentication authentication) {
                super(project, secretToken, authentication);
            }

            @Override
            protected void performOnPost(GiteePushTrigger trigger) {
                performOnPostCalled = true;
            }
        }

        @Override
        public void processForCompatibility() {}

        @Override
        public void execute() {}
    }
}
