package com.gitee.jenkins.trigger.handler;

import com.gitee.jenkins.cause.CauseData;
import com.gitee.jenkins.cause.GiteeWebHookCause;
import com.gitee.jenkins.util.LoggerUtil;
import hudson.model.Cause;
import hudson.model.Job;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.util.logging.Level;
import java.util.logging.Logger;

public class PendingBuildsHandler {

    private static final Logger LOGGER = Logger.getLogger(PendingBuildsHandler.class.getName());

    public void cancelPendingBuilds(Job<?, ?> job, Integer projectId, String branch) {
        Queue queue = Jenkins.getInstance().getQueue();
        for (Queue.Item item : queue.getItems()) {
            if (!job.getName().equals(item.task.getName())) {
                continue;
            }
            GiteeWebHookCause queueItemGiteeWebHookCause = getGiteeWebHookCauseData(item);
            if (queueItemGiteeWebHookCause == null) {
                continue;
            }
            CauseData queueItemCauseData = queueItemGiteeWebHookCause.getData();
            if (!projectId.equals(queueItemCauseData.getSourceProjectId())) {
                continue;
            }
            if (branch.equals(queueItemCauseData.getBranch())) {
                cancel(item, queue, branch);
            }
        }
    }

    private GiteeWebHookCause getGiteeWebHookCauseData(Queue.Item item) {
        for (Cause cause : item.getCauses()) {
            if (cause instanceof GiteeWebHookCause) {
                return (GiteeWebHookCause) cause;
            }
        }
        return null;
    }

    private void cancel(Queue.Item item, Queue queue, String branch) {
        try {
            LOGGER.log(Level.INFO, "Cancelling job {0} for branch {1}", LoggerUtil.toArray(item.task.getName(), branch));
            queue.cancel(item);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error cancelling queued build", e);
        }
    }
}
