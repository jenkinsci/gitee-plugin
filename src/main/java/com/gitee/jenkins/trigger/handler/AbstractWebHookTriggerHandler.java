package com.gitee.jenkins.trigger.handler;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.gitee.hook.model.WebHook;
import com.gitee.jenkins.trigger.exception.NoRevisionToBuildException;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.PullRequestLabelFilter;
import com.gitee.jenkins.util.LoggerUtil;
import hudson.model.Action;
import hudson.model.CauseAction;
import hudson.model.Job;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.RevisionParameterAction;
import hudson.scm.SCM;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Robin Müller
 */
public abstract class AbstractWebHookTriggerHandler<H extends WebHook> implements WebHookTriggerHandler<H> {

    private static final Logger LOGGER = Logger.getLogger(AbstractWebHookTriggerHandler.class.getName());
    protected PendingBuildsHandler pendingBuildsHandler = new PendingBuildsHandler();

    @Override
    public void handle(Job<?, ?> job, H hook, boolean ciSkip, boolean skipLastCommitHasBeenBuild, BranchFilter branchFilter, PullRequestLabelFilter pullRequestLabelFilter) {
        if (ciSkip && isCiSkip(hook)) {
            LOGGER.log(Level.INFO, "Skipping due to ci-skip.");
            return;
        }

        if (skipLastCommitHasBeenBuild && isCommitSkip(job, hook)) {
            LOGGER.log(Level.INFO, "Skipping due to ignore last commit has been build.");
            return;
        }

        String targetBranch = getTargetBranch(hook);
        if (branchFilter.isBranchAllowed(targetBranch)) {
            LOGGER.log(Level.INFO, "{0} triggered for {1}.", LoggerUtil.toArray(job.getFullName(), getTriggerType()));
            cancelPendingBuildsIfNecessary(job, hook);
            scheduleBuild(job, createActions(job, hook));
        } else {
            LOGGER.log(Level.INFO, "branch {0} is not allowed", targetBranch);
        }
    }

    protected abstract String getTriggerType();

    protected abstract boolean isCiSkip(H hook);
    protected abstract boolean isCommitSkip(Job<?, ?> job, H hook);

    protected Action[] createActions(Job<?, ?> job, H hook) {
        ArrayList<Action> actions = new ArrayList<>();
        actions.add(new CauseAction(new GiteeWebHookCause(retrieveCauseData(hook))));
        try {
            SCMTriggerItem item = SCMTriggerItem.SCMTriggerItems.asSCMTriggerItem(job);
            GitSCM gitSCM = getGitSCM(item);
            actions.add(createRevisionParameter(hook, gitSCM));
        } catch (NoRevisionToBuildException e) {
            LOGGER.log(Level.WARNING, "unknown handled situation, dont know what revision to build for req {0} for job {1}",
                    new Object[]{hook, (job != null ? job.getFullName() : null)});
        }
        return actions.toArray(new Action[actions.size()]);
    }

    protected void cancelPendingBuildsIfNecessary(Job<?, ?> job, H hook) {}

    protected abstract CauseData retrieveCauseData(H hook);

    protected abstract String getTargetBranch(H hook);

    protected abstract RevisionParameterAction  createRevisionParameter(H hook, GitSCM gitSCM) throws NoRevisionToBuildException;

    protected abstract BuildStatusUpdate retrieveBuildStatusUpdate(H hook);

    protected URIish retrieveUrIish(WebHook hook) {

        try {
            if (hook.getRepository() != null) {
                return new URIish(hook.getRepository().getUrl());
            }
        } catch (URISyntaxException e) {
            LOGGER.log(Level.WARNING, "could not parse URL");
        }
        return null;
    }

    protected void scheduleBuild(Job<?, ?> job, Action[] actions) {
        int projectBuildDelay = 0;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob abstractProject = (ParameterizedJobMixIn.ParameterizedJob) job;
            if (abstractProject.getQuietPeriod() > projectBuildDelay) {
                projectBuildDelay = abstractProject.getQuietPeriod();
            }
        }
        retrieveScheduleJob(job).scheduleBuild2(projectBuildDelay, actions);
    }

    private ParameterizedJobMixIn retrieveScheduleJob(final Job<?, ?> job) {
        // TODO 1.621+ use standard method
        return new ParameterizedJobMixIn() {
            @Override
            protected Job asJob() {
                return job;
            }
        };
    }

    private GitSCM getGitSCM(SCMTriggerItem item) {
        if (item != null) {
            for (SCM scm : item.getSCMs()) {
                if (scm instanceof GitSCM) {
                    return (GitSCM) scm;
                }
            }
        }
        return null;
    }

    public static class BuildStatusUpdate {
        private final Integer projectId;
        private final String sha;
        private final String ref;

        @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
        public BuildStatusUpdate(Integer projectId, String sha, String ref) {
            this.projectId = projectId;
            this.sha = sha;
            this.ref = ref;
        }

        public Integer getProjectId() {
            return projectId;
        }

        public String getSha() {
            return sha;
        }

        public String getRef() {
            return ref;
        }
    }
}
