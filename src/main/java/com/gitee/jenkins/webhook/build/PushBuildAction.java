package com.gitee.jenkins.webhook.build;

import com.gitee.jenkins.trigger.GiteePushTrigger;
import com.gitee.jenkins.gitee.hook.model.Project;
import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.util.JsonUtil;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.ACL;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMSource;
import jenkins.scm.api.SCMSourceOwner;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.transport.URIish;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.gitee.jenkins.util.JsonUtil.toPrettyPrint;
import static com.gitee.jenkins.util.LoggerUtil.toArray;

/**
 * @author Robin Müller
 */
public class PushBuildAction extends BuildWebHookAction {

    private final static Logger LOGGER = Logger.getLogger(PushBuildAction.class.getName());
    private final Item project;
    private PushHook pushHook;
    private final String secretToken;

    public PushBuildAction(Item project, String json, String secretToken) {
        LOGGER.log(Level.FINE, "Push: {0}", toPrettyPrint(json));
        this.project = project;
        this.pushHook = JsonUtil.read(json, PushHook.class);
        this.pushHook.setJsonBody(json);
        this.secretToken = secretToken;
    }

    void processForCompatibility() {
        // Fill in project if it's not defined.
        if (this.pushHook.getProject() == null && this.pushHook.getRepository() != null) {
            try {
                String path = new URL(this.pushHook.getRepository().getGitHttpUrl()).getPath();
                if (StringUtils.isNotBlank(path)) {
                    Project project = new Project();
                    project.setNamespace(path.replaceFirst("/", "").substring(0, path.lastIndexOf("/")));
                    this.pushHook.setProject(project);
                } else {
                    LOGGER.log(Level.WARNING, "Could not find suitable namespace.");
                }
            } catch (MalformedURLException ignored) {
                LOGGER.log(Level.WARNING, "Invalid repository url found while building namespace.");
            }
        }
    }

    public void execute() {
        if (pushHook.getRepository() != null && pushHook.getRepository().getUrl() == null) {
            LOGGER.log(Level.WARNING, "No repository url found.");
            return;
        }

        if (project instanceof Job<?, ?>) {
            ACL.impersonate(ACL.SYSTEM, new TriggerNotifier(project, secretToken, Jenkins.getAuthentication()) {
                @Override
                protected void performOnPost(GiteePushTrigger trigger) {
                    trigger.onPost(pushHook);
                }
            });
            throw responseWithHook(pushHook);
        }
        if (project instanceof SCMSourceOwner) {
            ACL.impersonate(ACL.SYSTEM, new SCMSourceOwnerNotifier());
            throw responseWithHook(pushHook);
        }
        throw HttpResponses.errorWithoutStack(409, "Push Hook is not supported for this project");
    }

    private class SCMSourceOwnerNotifier implements Runnable {
        public void run() {
            for (SCMSource scmSource : ((SCMSourceOwner) project).getSCMSources()) {
                if (scmSource instanceof GitSCMSource) {
                    GitSCMSource gitSCMSource = (GitSCMSource) scmSource;
                    try {
                        if (new URIish(gitSCMSource.getRemote()).equals(new URIish(gitSCMSource.getRemote()))) {
                            LOGGER.log(Level.FINE, "Notify scmSourceOwner {0} about changes for {1}",
                                       toArray(project.getName(), gitSCMSource.getRemote()));
                            ((SCMSourceOwner) project).onSCMSourceUpdated(scmSource);
                        }
                    } catch (URISyntaxException e) {
                        // nothing to do
                    }
                }
            }
        }
    }

}
