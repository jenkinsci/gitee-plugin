package com.gitee.jenkins.webhook.build;

import java.io.IOException;
import java.util.logging.Logger;

import com.gitee.jenkins.gitee.hook.model.WebHook;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.Messages;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.webhook.WebHookAction;

import javax.servlet.ServletException;

/**
 * @author Xinran Xiao
 * @author Yashin Luo
 */
abstract class BuildWebHookAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(BuildWebHookAction.class.getName());

    abstract void processForCompatibility();

    abstract void execute();

    public final void execute(StaplerResponse response) {
        processForCompatibility();
        execute();
    }

    public static HttpResponses.HttpResponseException responseWithHook(final WebHook webHook) {
        return new HttpResponses.HttpResponseException() {
            public void generateResponse(StaplerRequest req, StaplerResponse rsp, Object node) throws IOException, ServletException {
                String text = webHook.getWebHookDescription() + " has been accepted.";
                rsp.setContentType("text/plain;charset=UTF-8");
                rsp.getWriter().println(text);
            }
        };
    }

    protected abstract static class TriggerNotifier implements Runnable {

        private final Item project;
        private final String secretToken;
        private final Authentication authentication;

        public TriggerNotifier(Item project, String secretToken, Authentication authentication) {
            this.project = project;
            this.secretToken = secretToken;
            this.authentication = authentication;
        }

        public void run() {
            GiteePushTrigger trigger = GiteePushTrigger.getFromJob((Job<?, ?>) project);
            if (trigger != null) {
                if (StringUtils.isEmpty(trigger.getSecretToken())) {
                    checkPermission(Item.BUILD);
                } else if (!StringUtils.equals(trigger.getSecretToken(), secretToken)) {
                    throw HttpResponses.errorWithoutStack(401, "Invalid token");
                }
                performOnPost(trigger);
            }
        }

        private void checkPermission(Permission permission) {
            if (((GiteeConnectionConfig) Jenkins.getInstance().getDescriptor(GiteeConnectionConfig.class)).isUseAuthenticatedEndpoint()) {
                if (!Jenkins.getActiveInstance().getACL().hasPermission(authentication, permission)) {
                    String message = Messages.AccessDeniedException2_MissingPermission(authentication.getName(), permission.group.title+"/"+permission.name);
                    LOGGER.finest("Unauthorized (Did you forget to add API Token to the web hook ?)");
                    throw HttpResponses.errorWithoutStack(403, message);
                }
            }
        }

        protected abstract void performOnPost(GiteePushTrigger trigger);
    }
}
