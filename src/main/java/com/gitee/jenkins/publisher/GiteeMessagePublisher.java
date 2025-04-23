package com.gitee.jenkins.publisher;


import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.trigger.GiteePushTrigger;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Nikolay Ustinov
 */
public class GiteeMessagePublisher extends PullRequestNotifier {
    private static final Logger LOGGER = Logger.getLogger(GiteeMessagePublisher.class.getName());
    private boolean onlyForFailure = false;
    private boolean replaceSuccessNote = false;
    private boolean replaceFailureNote = false;
    private boolean replaceAbortNote = false;
    private boolean replaceUnstableNote = false;
    private String successNoteText;
    private String failureNoteText;
    private String abortNoteText;
    private String unstableNoteText;

    /**
     * @deprecated use {@link #GiteeMessagePublisher()} with setters to configure an instance of this class.
     */
    @Deprecated
    public GiteeMessagePublisher(boolean onlyForFailure, boolean replaceSuccessNote, boolean replaceFailureNote, boolean replaceAbortNote, boolean replaceUnstableNote,
                                 String successNoteText, String failureNoteText, String abortNoteText, String unstableNoteText) {
        this.onlyForFailure = onlyForFailure;
        this.replaceSuccessNote = replaceSuccessNote;
        this.replaceFailureNote = replaceFailureNote;
        this.replaceAbortNote = replaceAbortNote;
        this.replaceUnstableNote = replaceUnstableNote;
        this.successNoteText = successNoteText;
        this.failureNoteText = failureNoteText;
        this.abortNoteText = abortNoteText;
        this.unstableNoteText = unstableNoteText;
    }

    @DataBoundConstructor
    public GiteeMessagePublisher() { }

    public static GiteeMessagePublisher getFromJob(Job<?, ?> job) {
        GiteeMessagePublisher publisher = null;
        if (job instanceof AbstractProject) {
            AbstractProject p = (AbstractProject) job;
            Map<Descriptor<Publisher>, Publisher> map = p.getPublishersList().toMap();
            for (Publisher n : map.values()) {
                if (n instanceof GiteeMessagePublisher) {
                    publisher = (GiteeMessagePublisher) n;
                }
            }
        }
        return publisher;
    }

    public boolean isOnlyForFailure() {
        return onlyForFailure;
    }

    public boolean isReplaceSuccessNote() {
        return replaceSuccessNote;
    }

    public boolean isReplaceFailureNote() {
        return replaceFailureNote;
    }

    public boolean isReplaceAbortNote() {
        return replaceAbortNote;
    }

    public boolean isReplaceUnstableNote() {
        return replaceUnstableNote;
    }

    public String getSuccessNoteText() {
        return this.successNoteText == null ? "" : this.successNoteText;
    }

    public String getFailureNoteText() {
        return this.failureNoteText == null ? "" : this.failureNoteText;
    }

    public String getAbortNoteText() {
        return this.abortNoteText == null ? "" : this.abortNoteText;
    }

    public String getUnstableNoteText() {
        return this.unstableNoteText == null ? "" : this.unstableNoteText;
    }

    @DataBoundSetter
    public void setOnlyForFailure(boolean onlyForFailure) {
        this.onlyForFailure = onlyForFailure;
    }

    @DataBoundSetter
    public void setReplaceSuccessNote(boolean replaceSuccessNote) {
        this.replaceSuccessNote = replaceSuccessNote;
    }

    @DataBoundSetter
    public void setReplaceFailureNote(boolean replaceFailureNote) {
        this.replaceFailureNote = replaceFailureNote;
    }

    @DataBoundSetter
    public void setReplaceAbortNote(boolean replaceAbortNote) {
        this.replaceAbortNote = replaceAbortNote;
    }

    @DataBoundSetter
    public void setReplaceUnstableNote(boolean replaceUnstableNote) {
        this.replaceUnstableNote = replaceUnstableNote;
    }

    @DataBoundSetter
    public void setSuccessNoteText(String successNoteText) {
        this.successNoteText = successNoteText;
    }

    @DataBoundSetter
    public void setFailureNoteText(String failureNoteText) {
        this.failureNoteText = failureNoteText;
    }

    @DataBoundSetter
    public void setAbortNoteText(String abortNoteText) {
        this.abortNoteText = abortNoteText;
    }

    @DataBoundSetter
    public void setUnstableNoteText(String unstableNoteText) {
        this.unstableNoteText = unstableNoteText;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.GiteeMessagePublisher_DisplayName();
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitee/help/help-messagesOnResult.html";
        }
    }

    @Override
    protected void perform(Run<?, ?> build, TaskListener listener, GiteeClient client, PullRequest pullRequest) {
        try {
            if (!onlyForFailure || build.getResult() == Result.FAILURE || build.getResult() == Result.UNSTABLE) {
                client.createPullRequestNote(pullRequest, getNote(build, listener));
            }
        } catch (WebApplicationException | ProcessingException e) {
            listener.getLogger().printf("Failed to add comment on Pull Request for project '%s': %s%n", pullRequest.getProjectId(), e.getMessage());
            LOGGER.log(Level.SEVERE, String.format("Failed to add comment on Pull Request for project '%s'", pullRequest.getProjectId()), e);
        }
    }

    private String getResultIcon(Result result) {
        if (result == Result.SUCCESS) {
            return ":white_check_mark:";
        } else if (result == Result.ABORTED) {
            return ":point_up:";
        } else if (result == Result.UNSTABLE) {
            return ":warning:";
        } else {
            return ":x:";
        }
    }

    private static String replaceMacros(Run<?, ?> build, TaskListener listener, String inputString) {
        String returnString = inputString;
        if (build != null && inputString != null) {
            try {
                Map<String, String> messageEnvVars = getEnvVars(build, listener);
                returnString = Util.replaceMacro(inputString, messageEnvVars);

            } catch (Exception e) {
                listener.getLogger().printf("Couldn't replace macros in message: %s%n", e.getMessage());
                LOGGER.log(Level.WARNING, "Couldn't replace macros in message", e);
            }
        }
        return returnString;
    }

    private static Map<String, String> getEnvVars(Run<?, ?> build, TaskListener listener) {
        Map<String, String> messageEnvVars = new HashMap<>();
        if (build != null) {
            messageEnvVars.putAll(build.getCharacteristicEnvVars());
            try {
                messageEnvVars.putAll(build.getEnvironment(listener));
            } catch (Exception e) {
                listener.getLogger().printf("Couldn't get Env Variables: %s%n", e.getMessage());
                LOGGER.log(Level.WARNING, "Couldn't get Env Variables", e);
            }
        }
        return messageEnvVars;
    }

    private String getNote(Run<?, ?> build, TaskListener listener) {
        String message;
        if (this.replaceSuccessNote && build.getResult() == Result.SUCCESS) {
            message = replaceMacros(build, listener, this.getSuccessNoteText());
        } else if (this.replaceAbortNote && build.getResult() == Result.ABORTED) {
            message = replaceMacros(build, listener, this.getAbortNoteText());
        } else if (this.replaceUnstableNote && build.getResult() == Result.UNSTABLE) {
            message = replaceMacros(build, listener, this.getUnstableNoteText());
        } else if (this.replaceFailureNote && build.getResult() == Result.FAILURE) {
            message = replaceMacros(build, listener, this.getFailureNoteText());
        } else {
            String icon = getResultIcon(build.getResult());
            String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
            message = MessageFormat.format("{0} Jenkins Build {1}\n\nResults available at: [Jenkins [{2} # {3}]]({4})",
                                           icon, build.getResult().toString(), build.getParent().getDisplayName(), build.getNumber(), buildUrl);
        }
        return message;
    }
}
