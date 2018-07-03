package com.gitee.jenkins.webhook.status;

import com.gitee.jenkins.util.BuildUtil;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
public class BranchBuildPageRedirectAction extends BuildPageRedirectAction {
    public BranchBuildPageRedirectAction(Job<?, ?> project, String branchName) {
        super(BuildUtil.getBuildByBranch(project, branchName));
    }
}
