package com.gitee.jenkins.webhook.build;

import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.hook.model.PullRequestObjectAttributes;
import com.gitee.jenkins.gitee.hook.model.Project;
import com.gitee.jenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gitee.jenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Robin Müller
 */
public class PullRequestBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PullRequestBuildAction.class.getName());
    private Item project;
    private PullRequestHook pullRequestHook;
    private final String secretToken;

    public PullRequestBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "PullRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.pullRequestHook = JsonUtil.read(json, PullRequestHook.class);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // url and homepage are introduced in 8.x versions of Gitee
        final PullRequestObjectAttributes attributes = this.pullRequestHook.getPullRequest();
        if (attributes != null) {
            final Project source = attributes.getSource();
            if (source != null && source.getGitHttpUrl() != null) {
                if (source.getUrl() == null) {
                    source.setUrl(source.getGitHttpUrl());
                }
                if (source.getHomepage() == null) {
                    source.setHomepage(source.getGitHttpUrl().substring(0, source.getGitHttpUrl().lastIndexOf(".git")));
                }
            }

            // The PullRequestHookTriggerHandlerImpl is looking for Project
            if (pullRequestHook.getRepo() == null && attributes.getTarget() != null) {
                pullRequestHook.setRepo(attributes.getTarget());
            }
        }
    }

    public void execute() {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Merge Request Hook is not supported for this project");
        }
        ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
            @Override
            protected void performOnPost(GiteePushTrigger trigger) {
                trigger.onPost(pullRequestHook);
            }
        });
        throw responseWithHook(pullRequestHook);
    }
}
