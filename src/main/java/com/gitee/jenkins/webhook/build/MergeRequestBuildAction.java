package com.gitee.jenkins.webhook.build;

import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.hook.model.MergeRequestHook;
import com.gitee.jenkins.gitee.hook.model.MergeRequestObjectAttributes;
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
public class MergeRequestBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(MergeRequestBuildAction.class.getName());
    private Item project;
    private MergeRequestHook mergeRequestHook;
    private final String secretToken;

    public MergeRequestBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "MergeRequest: {0}", toPrettyPrint(json));
        this.project = project;
        this.mergeRequestHook = JsonUtil.read(json, MergeRequestHook.class);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // url and homepage are introduced in 8.x versions of Gitee
        final MergeRequestObjectAttributes attributes = this.mergeRequestHook.getObjectAttributes();
        if (attributes != null) {
            final Project source = attributes.getSource();
            if (source != null && source.getHttpUrl() != null) {
                if (source.getUrl() == null) {
                    source.setUrl(source.getHttpUrl());
                }
                if (source.getHomepage() == null) {
                    source.setHomepage(source.getHttpUrl().substring(0, source.getHttpUrl().lastIndexOf(".git")));
                }
            }

            // The MergeRequestHookTriggerHandlerImpl is looking for Project
            if (mergeRequestHook.getProject() == null && attributes.getTarget() != null) {
                mergeRequestHook.setProject(attributes.getTarget());
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
                trigger.onPost(mergeRequestHook);
            }
        });
        throw responseWithHook(mergeRequestHook);
    }
}
