package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.model.MergeRequest;
import com.google.common.base.Function;
import hudson.Extension;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;


@Extension
@Restricted(NoExternalUse.class)
public final class GiteeV5ClientBuilder extends ResteasyGiteeClientBuilder {
    private static final int ORDINAL = 3;
    private static final Function<MergeRequest, Integer> MERGE_REQUEST_ID_PROVIDER = new Function<MergeRequest, Integer>() {
        @Override
        public Integer apply(MergeRequest mergeRequest) {
            return mergeRequest.getIid();
        }
    };

    public GiteeV5ClientBuilder() {
        super(GiteeV5ApiProxy.ID, ORDINAL, GiteeV5ApiProxy.class, MERGE_REQUEST_ID_PROVIDER);
    }
}
