package com.gitee.jenkins.webhook.status;

import com.gitee.jenkins.webhook.WebHookAction;
import hudson.model.Run;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;

/**
 * @author Robin Müller
 */
abstract class BuildPageRedirectAction implements WebHookAction {

    private Run<?, ?> build;

    protected BuildPageRedirectAction(Run<?, ?> build) {
        this.build = build;
    }

    public void execute(StaplerResponse response) {
        if (build != null) {
            try {
                response.sendRedirect2(Jenkins.getInstance().getRootUrl() + build.getUrl());
            } catch (IOException e) {
                try {
                    response.sendRedirect2(Jenkins.getInstance().getRootUrl() + build.getBuildStatusUrl());
                } catch (IOException e1) {
                    throw HttpResponses.error(500, "Failed to redirect to build page");
                }
            }
        }
    }
}
