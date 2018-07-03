package com.gitee.jenkins.trigger.filter;

/**
 * @author Robin Müller
 */
public interface BranchFilter {

    boolean isBranchAllowed(String branchName);
}
