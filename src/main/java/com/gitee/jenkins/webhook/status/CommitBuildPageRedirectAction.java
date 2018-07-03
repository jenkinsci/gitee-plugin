package com.gitee.jenkins.webhook.status;

import com.gitee.jenkins.util.BuildUtil;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public class CommitBuildPageRedirectAction extends BuildPageRedirectAction {
    public CommitBuildPageRedirectAction(Job<?, ?> project, String sha1) {
        super(BuildUtil.getBuildBySHA1IncludingMergeBuilds(project, sha1));
    }
}
