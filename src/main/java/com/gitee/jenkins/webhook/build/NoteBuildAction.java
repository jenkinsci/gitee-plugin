package com.gitee.jenkins.webhook.build;

import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.security.core.Authentication;

import static com.gitee.jenkins.util.JsonUtil.toPrettyPrint;

/**
 * @author Nikolay Ustinov
 */
public class NoteBuildAction extends BuildWebHookAction {

    private static final Logger LOGGER = Logger.getLogger(NoteBuildAction.class.getName());
    private Item project;
    private NoteHook noteHook;
    private final String secretToken;

    public NoteBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "Note: {0}", toPrettyPrint(json));
        this.project = project;
        this.noteHook = JsonUtil.read(json, NoteHook.class);
        this.noteHook.setJsonBody(json);
        this.secretToken = secretToken;
    }

    @Override
    void processForCompatibility() {

    }

    @Override
    public void execute() {
        if (!(project instanceof Job<?, ?>)) {
            throw HttpResponses.errorWithoutStack(409, "Note Hook is not supported for this project");
        }
        Authentication auth = Jenkins.getAuthentication2();
        try (ACLContext ignored = ACL.as2(ACL.SYSTEM2)) {
            new BuildWebHookAction.TriggerNotifier(project, secretToken, auth) {
                @Override
                protected void performOnPost(GiteePushTrigger trigger) {
                    trigger.onPost(noteHook);
                }
            }.run();
        }
        throw responseWithHook(noteHook);
    }
}
