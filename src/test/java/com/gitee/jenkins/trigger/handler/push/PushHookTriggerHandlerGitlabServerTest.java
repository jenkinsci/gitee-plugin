package com.gitee.jenkins.trigger.handler.push;

import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.testhelpers.GiteePushRequestSamples;
import com.gitee.jenkins.testhelpers.GiteePushRequestSamples_7_10_5_489b413;
import com.gitee.jenkins.testhelpers.GiteePushRequestSamples_7_5_1_36679b5;
import com.gitee.jenkins.testhelpers.GiteePushRequestSamples_8_1_2_8c8af7b;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.UserRemoteConfig;
import org.eclipse.jgit.transport.RemoteConfig;
import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@RunWith(Theories.class)
public class PushHookTriggerHandlerGiteeServerTest {

    @DataPoints
    public static GiteePushRequestSamples[] samples = {
            new GiteePushRequestSamples_7_5_1_36679b5(),
            new GiteePushRequestSamples_7_10_5_489b413(),
            new GiteePushRequestSamples_8_1_2_8c8af7b()
    };

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Theory
    public void createRevisionParameterAction_pushBrandNewMasterBranchRequest(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushBrandNewMasterBranchRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_mergeRequestMergePushRequest(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.mergePushRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushCommitRequest(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushNewBranchRequest(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewBranchRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushNewTagRequest(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushNewTagRequest();

        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void doNotCreateRevisionParameterAction_deleteBranchRequest(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.deleteBranchRequest();

        exception.expect(NoRevisionToBuildException.class);
        new PushHookTriggerHandlerImpl().createRevisionParameter(hook, null);
    }

    @Theory
    public void createRevisionParameterAction_pushCommitRequestWithGitScm(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        GitSCM gitSCM = new GitSCM("git@test.tld:test.git");
        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, gitSCM);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getRef().replaceFirst("^refs/heads", "remotes/origin")));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }

    @Theory
    public void createRevisionParameterAction_pushCommitRequestWith2Remotes(GiteePushRequestSamples samples) throws Exception {
        PushHook hook = samples.pushCommitRequest();

        GitSCM gitSCM = new GitSCM(Arrays.asList(new UserRemoteConfig("git@test.tld:test.git", null, null, null),
                                                 new UserRemoteConfig("git@test.tld:fork.git", "fork", null, null)),
                                   Collections.singletonList(new BranchSpec("")),
                                   false, Collections.<SubmoduleConfig>emptyList(),
                                   null, null, null);
        RevisionParameterAction revisionParameterAction = new PushHookTriggerHandlerImpl().createRevisionParameter(hook, gitSCM);

        assertThat(revisionParameterAction, is(notNullValue()));
        assertThat(revisionParameterAction.commit, is(hook.getAfter()));
        assertFalse(revisionParameterAction.canOriginateFrom(new ArrayList<RemoteConfig>()));
    }
}
