package com.gitee.jenkins.trigger;

import com.gitee.jenkins.connection.GiteeConnection;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.hook.model.PullRequestHook;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.WebHook;
import com.gitee.jenkins.gitee.api.model.builder.generated.WebHookBuilder;
import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.gitee.hook.model.PipelineHook;
import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.publisher.GiteeAcceptPullRequestPublisher;
import com.gitee.jenkins.publisher.GiteeMessagePublisher;
import com.gitee.jenkins.trigger.filter.*;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilterConfig.PullRequestLabelFilterConfigBuilder;
import com.gitee.jenkins.trigger.handler.pull.PullRequestHookTriggerHandler;
import com.gitee.jenkins.trigger.handler.note.NoteHookTriggerHandler;
import com.gitee.jenkins.trigger.handler.pipeline.PipelineHookTriggerHandler;
import com.gitee.jenkins.trigger.handler.push.PushHookTriggerHandler;
import com.gitee.jenkins.webhook.GiteeWebHook;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractDescribableImpl;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.SequentialExecutionQueue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;
import org.springframework.security.access.AccessDeniedException;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static com.gitee.jenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.gitee.jenkins.trigger.handler.pull.PullRequestHookTriggerHandlerFactory.newPullRequestHookTriggerHandler;
import static com.gitee.jenkins.trigger.handler.note.NoteHookTriggerHandlerFactory.newNoteHookTriggerHandler;
import static com.gitee.jenkins.trigger.handler.pipeline.PipelineHookTriggerHandlerFactory.newPipelineHookTriggerHandler;
import static com.gitee.jenkins.trigger.handler.push.PushHookTriggerHandlerFactory.newPushHookTriggerHandler;

/**
 * Triggers a build when we receive a Gitee WebHook.
 *
 * @author Daniel Brooks
 * @author Yashin Luo
 *
 */
public class GiteePushTrigger extends Trigger<Job<?, ?>> {
    private static final Logger LOGGER = Logger.getLogger(GiteePushTrigger.class.getName());

    private boolean triggerOnPush = true;
    private boolean triggerOnCommitComment = false;
    private boolean triggerOnOpenPullRequest = true;
    private boolean triggerOnPipelineEvent = false;
    private boolean triggerOnAcceptedPullRequest = false;
    private String triggerOnUpdatePullRequest = "3";
    private boolean triggerOnClosedPullRequest = false;
    private boolean triggerOnApprovedPullRequest = false;
    private boolean triggerOnTestedPullRequest = false;
    private boolean triggerOnNoteRequest = true;
    private String noteRegex = "";
    private transient boolean ciSkip = true;
    private BuildInstructionFilterType buildInstructionFilterType = BuildInstructionFilterType.NONE;
    private boolean skipWorkInProgressPullRequest;
    private boolean ciSkipFroTestNotRequired;
    private boolean ciBuildForDeleteRef;
    private boolean skipLastCommitHasBeenBuild;
    private boolean setBuildDescription = true;
    private transient boolean addNoteOnPullRequest;
    private transient boolean addCiMessage;
    private transient boolean addVoteOnPullRequest;
    private transient boolean allowAllBranches = false;
    private transient String branchFilterName;
    private BranchFilterType branchFilterType;
    private String includeBranchesSpec;
    private String excludeBranchesSpec;
    private String targetBranchRegex;
    private String includeLabelSpec;
    private String excludeLabelSpec;
    private volatile Secret secretToken;
    private String pendingBuildName;
    private boolean cancelPendingBuildsOnUpdate;
    private boolean cancelIncompleteBuildOnSamePullRequest;
    private boolean ignorePullRequestConflicts;
    private List<WebhookEntry> webhooks = Collections.<WebhookEntry>emptyList();

    private transient BranchFilter branchFilter;
    private transient PushHookTriggerHandler pushHookTriggerHandler;
    private transient PullRequestHookTriggerHandler pullRequestHookTriggerHandler;
    private transient NoteHookTriggerHandler noteHookTriggerHandler;
    private transient PipelineHookTriggerHandler pipelineTriggerHandler;
    private transient boolean acceptPullRequestOnSuccess;
    private transient PullRequestLabelFilter pullRequestLabelFilter;

    /**
     * @deprecated use {@link #GiteePushTrigger()} with setters to configure an
     *             instance of this class.
     */
    @Deprecated
    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public GiteePushTrigger(boolean triggerOnPush,
            boolean triggerOnCommitComment,
            boolean triggerOnOpenPullRequest,
            String triggerOnUpdatePullRequest,
            boolean triggerOnAcceptedPullRequest,
            boolean triggerOnClosedPullRequest,
            boolean triggerOnNoteRequest, String noteRegex,
            boolean skipWorkInProgressPullRequest, boolean ciSkip,
            BuildInstructionFilterType buildInstructionFilterType,
            boolean setBuildDescription, boolean addNoteOnPullRequest, boolean addCiMessage,
            boolean addVoteOnPullRequest,
            boolean acceptPullRequestOnSuccess, BranchFilterType branchFilterType,
            String includeBranchesSpec, String excludeBranchesSpec, String targetBranchRegex, String secretToken,
            boolean triggerOnPipelineEvent,
            boolean triggerOnApprovedPullRequest, String pendingBuildName, boolean cancelPendingBuildsOnUpdate,
            boolean cancelIncompleteBuildOnSamePullRequest,
            boolean ignorePullRequestConflicts) {
        this.triggerOnPush = triggerOnPush;
        this.triggerOnCommitComment = triggerOnCommitComment;
        this.triggerOnOpenPullRequest = triggerOnOpenPullRequest;
        this.triggerOnUpdatePullRequest = triggerOnUpdatePullRequest;
        this.triggerOnAcceptedPullRequest = triggerOnAcceptedPullRequest;
        this.triggerOnClosedPullRequest = triggerOnClosedPullRequest;
        this.triggerOnNoteRequest = triggerOnNoteRequest;
        this.noteRegex = noteRegex;
        this.triggerOnPipelineEvent = triggerOnPipelineEvent;
        this.ciSkip = ciSkip;
        this.buildInstructionFilterType = buildInstructionFilterType;
        this.skipWorkInProgressPullRequest = skipWorkInProgressPullRequest;
        this.setBuildDescription = setBuildDescription;
        this.addNoteOnPullRequest = addNoteOnPullRequest;
        this.addCiMessage = addCiMessage;
        this.addVoteOnPullRequest = addVoteOnPullRequest;
        this.branchFilterType = branchFilterType;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.targetBranchRegex = targetBranchRegex;
        this.acceptPullRequestOnSuccess = acceptPullRequestOnSuccess;
        this.secretToken = Secret.fromString(secretToken);
        this.triggerOnApprovedPullRequest = triggerOnApprovedPullRequest;
        this.pendingBuildName = pendingBuildName;
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
        this.cancelIncompleteBuildOnSamePullRequest = cancelIncompleteBuildOnSamePullRequest;
        this.ignorePullRequestConflicts = ignorePullRequestConflicts;

        initializeTriggerHandler();
        initializeBranchFilter();
        initializePullRequestLabelFilter();
    }

    @DataBoundConstructor
    public GiteePushTrigger() {
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void migrateJobs() throws IOException {
        GiteePushTrigger.DescriptorImpl oldConfig = Trigger.all().get(GiteePushTrigger.DescriptorImpl.class);
        if (!oldConfig.jobsMigrated) {
            GiteeConnectionConfig giteeConfig = (GiteeConnectionConfig) Jenkins.get()
                    .getDescriptor(GiteeConnectionConfig.class);
            giteeConfig.getConnections().add(new GiteeConnection(
                    oldConfig.giteeHostUrl,
                    oldConfig.giteeHostUrl,
                    oldConfig.GiteeApiToken,
                    "autodetect",
                    oldConfig.ignoreCertificateErrors,
                    10,
                    10));

            String defaultConnectionName = giteeConfig.getConnections().get(0).getName();
            for (AbstractProject<?, ?> project : Jenkins.get().getAllItems(AbstractProject.class)) {
                GiteePushTrigger trigger = project.getTrigger(GiteePushTrigger.class);
                if (trigger != null) {
                    project.addProperty(new GiteeConnectionProperty(defaultConnectionName));
                    project.save();
                }
            }
            giteeConfig.save();
            oldConfig.jobsMigrated = true;
            oldConfig.save();
        }
        if (!oldConfig.jobsMigrated2) {
            for (AbstractProject<?, ?> project : Jenkins.get().getAllItems(AbstractProject.class)) {
                GiteePushTrigger trigger = project.getTrigger(GiteePushTrigger.class);
                if (trigger != null) {
                    if (trigger.addNoteOnPullRequest) {
                        project.getPublishersList().add(new GiteeMessagePublisher());
                    }
                    if (trigger.acceptPullRequestOnSuccess) {
                        project.getPublishersList().add(new GiteeAcceptPullRequestPublisher());
                    }
                    project.save();
                }
            }
            oldConfig.jobsMigrated2 = true;
            oldConfig.save();
        }

        // 兼容构建指令升级
        if (!oldConfig.jobsMigrated4) {
            for (AbstractProject<?, ?> project : Jenkins.get().getAllItems(AbstractProject.class)) {
                GiteePushTrigger trigger = project.getTrigger(GiteePushTrigger.class);
                if (trigger != null) {
                    if (trigger.getCiSkip()) {
                        trigger.setBuildInstructionFilterType(BuildInstructionFilterType.CI_SKIP);
                    } else if (trigger.getBuildInstructionFilterType() == null) {
                        trigger.setBuildInstructionFilterType(BuildInstructionFilterType.NONE);
                    }
                    project.save();
                }
            }
            oldConfig.jobsMigrated3 = false;
            oldConfig.jobsMigrated4 = true;
            oldConfig.save();
        }

    }

    public boolean getAddNoteOnPullRequest() {
        return addNoteOnPullRequest;
    }

    public boolean getTriggerOnPush() {
        return triggerOnPush;
    }

    public boolean isTriggerOnCommitComment() {
        return triggerOnCommitComment;
    }

    public boolean getTriggerOnOpenPullRequest() {
        return triggerOnOpenPullRequest;
    }

    public boolean getTriggerOnTestedPullRequest() {
        return triggerOnTestedPullRequest;
    }

    public String getTriggerOnUpdatePullRequest() {
        return triggerOnUpdatePullRequest;
    }

    public boolean isTriggerOnAcceptedPullRequest() {
        return triggerOnAcceptedPullRequest;
    }

    public boolean isTriggerOnApprovedPullRequest() {
        return triggerOnApprovedPullRequest;
    }

    public boolean isTriggerOnClosedPullRequest() {
        return triggerOnClosedPullRequest;
    }

    public boolean getTriggerOnNoteRequest() {
        return triggerOnNoteRequest;
    }

    public boolean getTriggerOnPipelineEvent() {
        return triggerOnPipelineEvent;
    }

    public String getNoteRegex() {
        return this.noteRegex == null ? "" : this.noteRegex;
    }

    public boolean getSetBuildDescription() {
        return setBuildDescription;
    }

    public boolean getCiSkip() {
        return ciSkip;
    }

    public BuildInstructionFilterType getBuildInstructionFilterType() {
        return buildInstructionFilterType;
    }

    public boolean getCiSkipFroTestNotRequired() {
        return ciSkipFroTestNotRequired;
    }

    public boolean getCiBuildForDeleteRef() {
        return ciBuildForDeleteRef;
    }

    public boolean getSkipLastCommitHasBeenBuild() {
        return skipLastCommitHasBeenBuild;
    }

    public boolean isSkipWorkInProgressPullRequest() {
        return skipWorkInProgressPullRequest;
    }

    public boolean isSkipLastCommitHasBuild() {
        return skipLastCommitHasBeenBuild;
    }

    public boolean isSkipFroTestNotRequired() {
        return ciSkipFroTestNotRequired;
    }

    public BranchFilterType getBranchFilterType() {
        return branchFilterType;
    }

    public String getIncludeBranchesSpec() {
        return includeBranchesSpec;
    }

    public String getExcludeBranchesSpec() {
        return excludeBranchesSpec;
    }

    public String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    public String getSecretToken() {
        return secretToken == null ? null : secretToken.getPlainText();
    }

    public String getPendingBuildName() {
        return pendingBuildName;
    }

    public boolean getCancelPendingBuildsOnUpdate() {
        return this.cancelPendingBuildsOnUpdate;
    }

    public boolean isCancelIncompleteBuildOnSamePullRequest() {
        return cancelIncompleteBuildOnSamePullRequest;
    }

    public boolean isIgnorePullRequestConflicts() {
        return ignorePullRequestConflicts;
    }

    public List<WebhookEntry> getWebhooks() {
        return webhooks;
    }

    public String getIncludeLabelSpec() {
        return includeLabelSpec;
    }

    public String getExcludeLabelSpec() {
        return excludeLabelSpec;
    }

    @DataBoundSetter
    public void setIncludeLabelSpec(String includeLabelSpec) {
        this.includeLabelSpec = includeLabelSpec;
    }

    @DataBoundSetter
    public void setExcludeLabelSpec(String excludeLabelSpec) {
        this.excludeLabelSpec = excludeLabelSpec;
    }

    @DataBoundSetter
    public void setWebhooks(List<WebhookEntry> webhooks) {
        this.webhooks = webhooks;
    }

    @DataBoundSetter
    public void setTriggerOnPush(boolean triggerOnPush) {
        this.triggerOnPush = triggerOnPush;
    }

    @DataBoundSetter
    public void setTriggerOnCommitComment(boolean triggerOnCommitComment) {
        this.triggerOnCommitComment = triggerOnCommitComment;
    }

    @DataBoundSetter
    public void setTriggerOnApprovedPullRequest(boolean triggerOnApprovedPullRequest) {
        this.triggerOnApprovedPullRequest = triggerOnApprovedPullRequest;
    }

    @DataBoundSetter
    public void setTriggerOnTestedPullRequest(boolean triggerOnTestedPullRequest) {
        this.triggerOnTestedPullRequest = triggerOnTestedPullRequest;
    }

    @DataBoundSetter
    public void setTriggerOnOpenPullRequest(boolean triggerOnOpenPullRequest) {
        this.triggerOnOpenPullRequest = triggerOnOpenPullRequest;
    }

    @DataBoundSetter
    public void setTriggerOnAcceptedPullRequest(boolean triggerOnAcceptedPullRequest) {
        this.triggerOnAcceptedPullRequest = triggerOnAcceptedPullRequest;
    }

    @DataBoundSetter
    public void setTriggerOnClosedPullRequest(boolean triggerOnClosedPullRequest) {
        this.triggerOnClosedPullRequest = triggerOnClosedPullRequest;
    }

    @DataBoundSetter
    public void setTriggerOnNoteRequest(boolean triggerOnNoteRequest) {
        this.triggerOnNoteRequest = triggerOnNoteRequest;
    }

    @DataBoundSetter
    public void setNoteRegex(String noteRegex) {
        this.noteRegex = noteRegex;
    }

    @DataBoundSetter
    public void setCiSkip(boolean ciSkip) {
        this.ciSkip = ciSkip;
    }

    @DataBoundSetter
    public void setBuildInstructionFilterType(BuildInstructionFilterType buildInstructionFilterType) {
        this.buildInstructionFilterType = buildInstructionFilterType;
    }

    @DataBoundSetter
    public void setCiSkipFroTestNotRequired(boolean ciSkipFroTestNotRequired) {
        this.ciSkipFroTestNotRequired = ciSkipFroTestNotRequired;
    }

    @DataBoundSetter
    public void setCiBuildForDeleteRef(boolean ciBuildForDeleteRef) {
        this.ciBuildForDeleteRef = ciBuildForDeleteRef;
    }

    @DataBoundSetter
    public void setSkipWorkInProgressPullRequest(boolean skipWorkInProgressPullRequest) {
        this.skipWorkInProgressPullRequest = skipWorkInProgressPullRequest;
    }

    @DataBoundSetter
    public void setSkipLastCommitHasBeenBuild(boolean skipLastCommitHasBeenBuild) {
        this.skipLastCommitHasBeenBuild = skipLastCommitHasBeenBuild;
    }

    @DataBoundSetter
    public void setSetBuildDescription(boolean setBuildDescription) {
        this.setBuildDescription = setBuildDescription;
    }

    @DataBoundSetter
    public void setAddNoteOnPullRequest(boolean addNoteOnPullRequest) {
        this.addNoteOnPullRequest = addNoteOnPullRequest;
    }

    @DataBoundSetter
    public void setAddCiMessage(boolean addCiMessage) {
        this.addCiMessage = addCiMessage;
    }

    @DataBoundSetter
    public void setAddVoteOnPullRequest(boolean addVoteOnPullRequest) {
        this.addVoteOnPullRequest = addVoteOnPullRequest;
    }

    @DataBoundSetter
    public void setBranchFilterName(String branchFilterName) {
        this.branchFilterName = branchFilterName;
    }

    @DataBoundSetter
    public void setBranchFilterType(BranchFilterType branchFilterType) {
        this.branchFilterType = branchFilterType;
    }

    @DataBoundSetter
    public void setIncludeBranchesSpec(String includeBranchesSpec) {
        this.includeBranchesSpec = includeBranchesSpec;
    }

    @DataBoundSetter
    public void setExcludeBranchesSpec(String excludeBranchesSpec) {
        this.excludeBranchesSpec = excludeBranchesSpec;
    }

    @DataBoundSetter
    public void setTargetBranchRegex(String targetBranchRegex) {
        this.targetBranchRegex = targetBranchRegex;
    }

    @DataBoundSetter
    public void setSecretToken(String secretToken) {
        this.secretToken = Secret.fromString(secretToken);
    }

    @DataBoundSetter
    public void setAcceptPullRequestOnSuccess(boolean acceptPullRequestOnSuccess) {
        this.acceptPullRequestOnSuccess = acceptPullRequestOnSuccess;
    }

    @DataBoundSetter
    public void setTriggerOnUpdatePullRequest(String triggerOnUpdatePullRequest) {
        this.triggerOnUpdatePullRequest = triggerOnUpdatePullRequest;
    }

    @DataBoundSetter
    public void setTriggerOnPipelineEvent(boolean triggerOnPipelineEvent) {
        this.triggerOnPipelineEvent = triggerOnPipelineEvent;
    }

    @DataBoundSetter
    public void setPendingBuildName(String pendingBuildName) {
        this.pendingBuildName = pendingBuildName;
    }

    @DataBoundSetter
    public void setCancelPendingBuildsOnUpdate(boolean cancelPendingBuildsOnUpdate) {
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
    }

    @DataBoundSetter
    public void setCancelIncompleteBuildOnSamePullRequest(boolean cancelIncompleteBuildOnSamePullRequest) {
        this.cancelIncompleteBuildOnSamePullRequest = cancelIncompleteBuildOnSamePullRequest;
    }

    @DataBoundSetter
    public void setIgnorePullRequestConflicts(boolean ignorePullRequestConflicts) {
        this.ignorePullRequestConflicts = ignorePullRequestConflicts;
    }

    // executes when the Trigger receives a push request
    public void onPost(final PushHook hook) {
        if (branchFilter == null) {
            initializeBranchFilter();
        }
        if (pullRequestLabelFilter == null) {
            initializePullRequestLabelFilter();
        }
        if (pushHookTriggerHandler == null) {
            initializeTriggerHandler();
        }

        pushHookTriggerHandler.handle(job, hook, buildInstructionFilterType, skipLastCommitHasBeenBuild, branchFilter,
                pullRequestLabelFilter);
    }

    // executes when the Trigger receives a pull request
    public void onPost(final PullRequestHook hook) {
        if (branchFilter == null) {
            initializeBranchFilter();
        }
        if (pullRequestLabelFilter == null) {
            initializePullRequestLabelFilter();
        }
        if (pullRequestHookTriggerHandler == null) {
            initializeTriggerHandler();
        }
        pullRequestHookTriggerHandler.handle(job, hook, buildInstructionFilterType, skipLastCommitHasBeenBuild,
                branchFilter, pullRequestLabelFilter);
    }

    // executes when the Trigger receives a note request
    public void onPost(final NoteHook hook) {
        if (branchFilter == null) {
            initializeBranchFilter();
        }
        if (pullRequestLabelFilter == null) {
            initializePullRequestLabelFilter();
        }
        if (noteHookTriggerHandler == null) {
            initializeTriggerHandler();
        }
        noteHookTriggerHandler.handle(job, hook, buildInstructionFilterType, skipLastCommitHasBeenBuild, branchFilter,
                pullRequestLabelFilter);
    }

    // executes when the Trigger receives a pipeline event
    public void onPost(final PipelineHook hook) {
        if (pipelineTriggerHandler == null) {
            initializeTriggerHandler();
        }
        pipelineTriggerHandler.handle(job, hook, buildInstructionFilterType, skipLastCommitHasBeenBuild, branchFilter,
                pullRequestLabelFilter);
    }

    private void initializeTriggerHandler() {
        pullRequestHookTriggerHandler = newPullRequestHookTriggerHandler(triggerOnOpenPullRequest,
                triggerOnUpdatePullRequest, triggerOnAcceptedPullRequest, triggerOnClosedPullRequest,
                skipWorkInProgressPullRequest, triggerOnApprovedPullRequest, triggerOnTestedPullRequest,
                cancelPendingBuildsOnUpdate, ciSkipFroTestNotRequired,
                cancelIncompleteBuildOnSamePullRequest,
                ignorePullRequestConflicts);
        noteHookTriggerHandler = newNoteHookTriggerHandler(triggerOnCommitComment, triggerOnNoteRequest, noteRegex,
                ciSkipFroTestNotRequired, cancelIncompleteBuildOnSamePullRequest, ignorePullRequestConflicts);
        pushHookTriggerHandler = newPushHookTriggerHandler(triggerOnPush, skipWorkInProgressPullRequest,
                ciBuildForDeleteRef);
        pipelineTriggerHandler = newPipelineHookTriggerHandler(triggerOnPipelineEvent);
    }

    private void initializeBranchFilter() {
        if (branchFilterType == null) {
            branchFilterType = BranchFilterType.All;
        }

        branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec(includeBranchesSpec)
                .withExcludeBranchesSpec(excludeBranchesSpec)
                .withTargetBranchRegex(targetBranchRegex)
                .build(branchFilterType));
    }

    private void initializePullRequestLabelFilter() {
        pullRequestLabelFilter = PullRequestLabelFilterFactory
                .newPullRequestLabelFilter
                    (new PullRequestLabelFilterConfigBuilder()
                        .withIncludeBranchesSpec(includeLabelSpec)
                        .withExcludeBranchesSpec(excludeLabelSpec)
                        .build());
    }

    @Override
    protected Object readResolve() throws ObjectStreamException {
        initializeTriggerHandler();
        initializeBranchFilter();
        initializePullRequestLabelFilter();
        return super.readResolve();
    }

    public static GiteePushTrigger getFromJob(Job<?, ?> job) {
        GiteePushTrigger trigger = null;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob p) {
            for (Object t : p.getTriggers().values()) {
                if (t instanceof GiteePushTrigger pushTrigger) {
                    trigger = pushTrigger;
                }
            }
        }
        return trigger;
    }

    @Extension
    @Symbol("gitee")
    public static class DescriptorImpl extends TriggerDescriptor {

        private final transient SequentialExecutionQueue queue = new SequentialExecutionQueue(
                Jenkins.MasterComputer.threadPoolForRemoting);
        private boolean jobsMigrated = false;
        private boolean jobsMigrated2 = false;
        private boolean jobsMigrated3 = false;
        private boolean jobsMigrated4 = false;
        private String GiteeApiToken;
        private String giteeHostUrl = "";
        private boolean ignoreCertificateErrors = false;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job
                    && SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @NonNull
        @Override
        public String getDisplayName() {
            Job<?, ?> project = retrieveCurrentJob();
            if (project != null) {
                try {
                    return Messages.Build_Gitee_WebHook(retrieveProjectUrl(project));
                    // return "Build when a change is pushed to Gitee";
                } catch (IllegalStateException e) {
                    // nothing to do
                }
            }
            return "Build when a change is pushed to Gitee, unknown URL";
        }

        private StringBuilder retrieveProjectUrl(Job<?, ?> project) {
            return new StringBuilder()
                    .append(Jenkins.get().getRootUrl())
                    .append(GiteeWebHook.WEBHOOK_URL)
                    .append(retrieveParentUrl(project))
                    .append('/').append(Util.rawEncode(project.getName()));
        }

        private StringBuilder retrieveParentUrl(Item item) {
            if (item.getParent() instanceof Item parent) {
                return retrieveParentUrl(parent).append('/').append(Util.rawEncode(parent.getName()));
            } else {
                return new StringBuilder();
            }
        }

        private Job<?, ?> retrieveCurrentJob() {
            StaplerRequest2 request = Stapler.getCurrentRequest2();
            if (request != null) {
                Ancestor ancestor = request.findAncestor(Job.class);
                return ancestor == null ? null : (Job<?, ?>) ancestor.getObject();
            }
            return null;
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }
    }

    public static final class WebhookEntry extends AbstractDescribableImpl<WebhookEntry> {
        private String name;
        private String owner;
        private String repo;
        private boolean isPush;
        private boolean isTagPush;
        private boolean isIssue;
        private boolean isNote;
        private boolean isPullRequest;

        @DataBoundConstructor
        public WebhookEntry(String name, String owner, String repo, boolean isPush, boolean isTagPush, boolean isIssue,
                boolean isNote, boolean isPulRequest) {
            this.name = name;
            this.owner = owner;
            this.repo = repo;
            this.isPush = isPush;
            this.isTagPush = isTagPush;
            this.isIssue = isIssue;
            this.isNote = isNote;
            this.isPullRequest = isPulRequest;
        }

        public String getOwner() {
            return owner;
        }

        public String getRepo() {
            return repo;
        }

        public boolean isPush() {
            return isPush;
        }

        public boolean isTagPush() {
            return isTagPush;
        }

        public boolean isIssue() {
            return isIssue;
        }

        public boolean isNote() {
            return isNote;
        }

        public boolean isPullRequest() {
            return isPullRequest;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Extension
        public static class DescriptorImpl extends Descriptor<WebhookEntry> {
            @Override
            public String getDisplayName() {

                return "Webhooks";
            }

            @Override
            public String getHelpFile() {
                return "/plugin/gitee/help/help-add-webhooks.html";
            }

            @POST
            public FormValidation doAddWebhook(
                    @QueryParameter String repo,
                    @QueryParameter String owner,
                    @QueryParameter String name,
                    @QueryParameter boolean isPush,
                    @QueryParameter boolean isTagPush,
                    @QueryParameter boolean isIssue,
                    @QueryParameter boolean isNote,
                    @QueryParameter boolean isPullRequest,
                    @AncestorInPath Job<?, ?> job) {

                String url = Jenkins.get().getRootUrl();
                if (url.contains("localhost") || url.contains("127.0.0.1")) {
                    return FormValidation
                            .error(Messages.localhost_error());
                }

                GiteeConnectionProperty giteeConnectionProp = (GiteeConnectionProperty) job
                        .getProperty(GiteeConnectionProperty.class);

                try {
                    Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                    GiteeClient client = giteeConnectionProp.getClient();
                    List<WebHook> hooks = client.getWebHooks(owner, repo);
                    for (WebHook hook : hooks) {
                        if (hook.getTitle().equals(name)) {
                            return FormValidation.ok(Messages.webhook_exist());
                        }
                    }

                    WebHook hook = new WebHookBuilder()
                            .withUrl(Jenkins.get().getRootUrl())
                            .withTitle(name)
                            .withPushEvents(isPush)
                            .withTagPushEvents(isTagPush)
                            .withNoteEvents(isNote)
                            .withIssuesEvents(isIssue)
                            .withMergeRequestsEvents(isPullRequest)
                            .withEncryptionType(0)
                            .build();

                    WebHook hookCreated = client.createWebHook(owner, repo, hook);
                    return FormValidation.ok(Messages.connection_success(hookCreated.getTitle()));
                } catch (WebApplicationException e) {
                    return FormValidation.error(Messages.connection_error(e.getMessage()));
                } catch (ProcessingException e) {
                    return FormValidation.error(Messages.connection_error(e.getCause().getMessage()));
                } catch (AccessDeniedException e) {
                    return FormValidation.error(Messages.connection_error(e.getMessage()));
                }
            }
        }
    }
}
