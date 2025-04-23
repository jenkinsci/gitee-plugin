package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.google.common.base.Function;
import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class GiteeV5ClientBuilder extends ResteasyGiteeClientBuilder {
    private static final int ORDINAL = 3;
    private static final Function<PullRequest, Integer> MERGE_REQUEST_ID_PROVIDER = new Function<PullRequest, Integer>() {
        @Override
        public Integer apply(PullRequest pullRequest) {
            return pullRequest.getIid();
        }
    };

    public GiteeV5ClientBuilder() {
        super(GiteeV5ApiProxy.ID, ORDINAL, GiteeV5ApiProxy.class, MERGE_REQUEST_ID_PROVIDER);
    }
}
