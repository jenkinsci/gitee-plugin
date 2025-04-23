package com.gitee.jenkins.webhook;

import org.kohsuke.stapler.StaplerResponse;

/**
 * @author Robin Müller
 */
public interface WebHookAction {
    void execute(StaplerResponse response);
}
