package com.gitee.jenkins.testhelpers;

import com.gitee.jenkins.gitee.hook.model.PushHook;

public interface GiteePushRequestSamples {
    PushHook pushBrandNewMasterBranchRequest();

    PushHook pushNewBranchRequest();

    PushHook pushCommitRequest();

    PushHook mergePushRequest();

    PushHook pushNewTagRequest();

    PushHook deleteBranchRequest();
}
