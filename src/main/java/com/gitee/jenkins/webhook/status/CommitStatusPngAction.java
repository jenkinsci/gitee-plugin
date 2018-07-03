package com.gitee.jenkins.webhook.status;

import com.gitee.jenkins.util.BuildUtil;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public class CommitStatusPngAction extends StatusPngAction {
    public CommitStatusPngAction(Job<?, ?> project, String sha1) {
        super(project, BuildUtil.getBuildBySHA1WithoutMergeBuilds(project, sha1));
    }
}
