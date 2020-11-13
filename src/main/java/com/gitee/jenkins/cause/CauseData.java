package com.gitee.jenkins.cause;

import com.gitee.jenkins.gitee.api.model.PullRequest;
import hudson.markup.EscapedMarkupFormatter;
import jenkins.model.Jenkins;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Robin Müller
 */
public final class CauseData {
    private final ActionType actionType;
    private final Integer sourceProjectId;
    private final Integer targetProjectId;
    private final String branch;
    private final String pathWithNamespace;
    private final String sourceBranch;
    private final String userName;
    private final String userEmail;
    private final String sourceRepoHomepage;
    private final String sourceRepoName;
    private final String sourceNamespace;
    private final String sourceRepoUrl;
    private final String sourceRepoSshUrl;
    private final String sourceRepoHttpUrl;
    private final String pullRequestTitle;
    private final String pullRequestDescription;
    private final Integer pullRequestId;
    private final Integer pullRequestIid;
    private final String pullRequestState;
    private final String mergedByUser;
    private final String pullRequestAssignee;
    private final Integer pullRequestTargetProjectId;
    private final String targetBranch;
    private final String targetRepoName;
    private final String targetNamespace;
    private final String targetRepoSshUrl;
    private final String targetRepoHttpUrl;
    private final String triggeredByUser;
    private final String before;
    private final String after;
    private final String lastCommit;
    private final String targetProjectUrl;
    private final String triggerPhrase;
    private final String ref;
    private final String beforeSha;
    private final String isTag;
    private final String sha;
    private final String status;
    private final String stages;
    private final String createdAt;
    private final String finishedAt;
    private final String buildDuration;
    private final String jsonBody;
    private final String noteBody;
    private final boolean created;
    private final boolean deleted;

    @GeneratePojoBuilder(withFactoryMethod = "*")
    CauseData(ActionType actionType, Integer sourceProjectId, Integer targetProjectId, String branch, String sourceBranch, String userName,
              String userEmail, String sourceRepoHomepage, String sourceRepoName, String sourceNamespace, String sourceRepoUrl,
              String sourceRepoSshUrl, String sourceRepoHttpUrl, String pullRequestTitle, String pullRequestDescription, Integer pullRequestId,
              Integer pullRequestIid, Integer pullRequestTargetProjectId, String targetBranch, String targetRepoName, String targetNamespace,
              String targetRepoSshUrl, String targetRepoHttpUrl, String triggeredByUser, String before, String after, String lastCommit,
              String targetProjectUrl, String triggerPhrase, String pullRequestState, String mergedByUser, String pullRequestAssignee,
              String ref, String isTag, String sha, String beforeSha, String status, String stages, String createdAt, String finishedAt,
              String buildDuration, String pathWithNamespace, boolean created, boolean deleted, String jsonBody, String noteBody) {
        this.actionType = checkNotNull(actionType, "actionType must not be null.");
        this.sourceProjectId = checkNotNull(sourceProjectId, "sourceProjectId must not be null.");
        this.targetProjectId = checkNotNull(targetProjectId, "targetProjectId must not be null.");
        this.branch = checkNotNull(branch, "branch must not be null.");
        this.sourceBranch = checkNotNull(sourceBranch, "sourceBranch must not be null.");
        this.userName = checkNotNull(userName, "userName must not be null.");
        this.userEmail = userEmail == null ? "" : userEmail;
        this.sourceRepoHomepage = sourceRepoHomepage == null ? "" : sourceRepoHomepage;
        this.sourceRepoName = checkNotNull(sourceRepoName, "sourceRepoName must not be null.");
        this.sourceNamespace = checkNotNull(sourceNamespace, "sourceNamespace must not be null.");
        this.sourceRepoUrl = sourceRepoUrl == null ? sourceRepoSshUrl : sourceRepoUrl;
        this.sourceRepoSshUrl = checkNotNull(sourceRepoSshUrl, "sourceRepoSshUrl must not be null.");
        this.sourceRepoHttpUrl = checkNotNull(sourceRepoHttpUrl, "sourceRepoHttpUrl must not be null.");
        this.pullRequestTitle = checkNotNull(pullRequestTitle, "pullRequestTitle must not be null.");
        this.pullRequestDescription = pullRequestDescription == null ? "" : pullRequestDescription;
        this.pullRequestId = pullRequestId;
        this.pullRequestIid = pullRequestIid;
        this.pullRequestState = pullRequestState == null ? "" : pullRequestState;
        this.mergedByUser = mergedByUser == null ? "" : mergedByUser;
        this.pullRequestAssignee = pullRequestAssignee == null ? "" : pullRequestAssignee;
        this.pullRequestTargetProjectId = pullRequestTargetProjectId;
        this.targetBranch = checkNotNull(targetBranch, "targetBranch must not be null.");
        this.targetRepoName = checkNotNull(targetRepoName, "targetRepoName must not be null.");
        this.targetNamespace = checkNotNull(targetNamespace, "targetNamespace must not be null.");
        this.targetRepoSshUrl = checkNotNull(targetRepoSshUrl, "targetRepoSshUrl must not be null.");
        this.targetRepoHttpUrl = checkNotNull(targetRepoHttpUrl, "targetRepoHttpUrl must not be null.");
        this.triggeredByUser = checkNotNull(triggeredByUser, "triggeredByUser must not be null.");
        this.before = before == null ? "" : before;
        this.after = after == null ? "" : after;
//        this.lastCommit = checkNotNull(lastCommit, "lastCommit must not be null");
        // 直接checkout到分支，而非commit sha，暂时不需要确保lastCommit 非空
        this.lastCommit = lastCommit;
        this.targetProjectUrl = targetProjectUrl;
        this.triggerPhrase = triggerPhrase;
        this.ref = ref;
        this.isTag = isTag;
        this.sha = sha;
        this.beforeSha = beforeSha;
        this.status = status;
        this.stages = stages;
        this.createdAt = createdAt;
        this.finishedAt = finishedAt;
        this.buildDuration = buildDuration;
        this.pathWithNamespace = pathWithNamespace;
        this.created = created;
        this.deleted = deleted;
        this.jsonBody = jsonBody;
        this.noteBody = noteBody;
    }

    public Map<String, String> getBuildVariables() {
        MapWrapper<String, String> variables = new MapWrapper<>(new HashMap<String, String>());
        variables.put("giteeBranch", branch);
        variables.put("giteeSourceBranch", sourceBranch);
        variables.put("giteeActionType", actionType.name());
        variables.put("giteeUserName", userName);
        variables.put("giteeUserEmail", userEmail);
        variables.put("giteeSourceRepoHomepage", sourceRepoHomepage);
        variables.put("giteeSourceRepoName", sourceRepoName);
        variables.put("giteeSourceNamespace", sourceNamespace);
        variables.put("giteeSourceRepoURL", sourceRepoUrl);
        variables.put("giteeSourceRepoSshUrl", sourceRepoSshUrl);
        variables.put("giteeSourceRepoHttpUrl", sourceRepoHttpUrl);
        variables.put("giteePullRequestTitle", pullRequestTitle);
        variables.put("giteePullRequestDescription", pullRequestDescription);
        variables.put("giteePullRequestId", pullRequestId == null ? "" : pullRequestId.toString());
        variables.put("giteePullRequestIid", pullRequestIid == null ? "" : pullRequestIid.toString());
        variables.put("giteePullRequestTargetProjectId", pullRequestTargetProjectId == null ? "" : pullRequestTargetProjectId.toString());
        variables.put("giteePullRequestLastCommit", lastCommit);
        variables.put("giteePushCreated", created ? "true" : "false");
        variables.put("giteePushDeleted", deleted ? "true" : "false");
        variables.putIfNotNull("giteePullRequestState", pullRequestState);
        variables.putIfNotNull("giteeMergedByUser", mergedByUser);
        variables.putIfNotNull("giteePullRequestAssignee", pullRequestAssignee);
        variables.put("giteeTargetBranch", targetBranch);
        variables.put("giteeTargetRepoName", targetRepoName);
        variables.put("giteeTargetNamespace", targetNamespace);
        variables.put("giteeTargetRepoSshUrl", targetRepoSshUrl);
        variables.put("giteeTargetRepoHttpUrl", targetRepoHttpUrl);
        variables.put("giteeBefore", before);
        variables.put("giteeAfter", after);
        variables.put("giteeBeforeCommitSha", before);
        variables.put("giteeAfterCommitSha", after);
        variables.put("giteeRef", ref);
        variables.put("ref", ref);
        variables.put("beforeSha", beforeSha);
        variables.put("isTag", isTag);
        variables.put("sha", sha);
        variables.put("status", status);
        variables.put("stages", stages);
        variables.put("createdAt", createdAt);
        variables.put("finishedAt", finishedAt);
        variables.put("duration", buildDuration);
        variables.put("jsonBody", jsonBody);
        variables.put("noteBody", noteBody);
        variables.putIfNotNull("giteeTriggerPhrase", triggerPhrase);
        return variables;
    }

    public Integer getSourceProjectId() {
        return sourceProjectId;
    }

    public Integer getTargetProjectId() {
        return targetProjectId;
    }

    public String getBranch() {
        return branch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public String getSourceRepoHomepage() {
        return sourceRepoHomepage;
    }

    public String getSourceRepoName() {
        return sourceRepoName;
    }

    public String getSourceNamespace() {
        return sourceNamespace;
    }

    public String getSourceRepoUrl() {
        return sourceRepoUrl;
    }

    public String getSourceRepoSshUrl() {
        return sourceRepoSshUrl;
    }

    public String getSourceRepoHttpUrl() {
        return sourceRepoHttpUrl;
    }

    public String getPullRequestTitle() {
        return pullRequestTitle;
    }

    public String getPullRequestDescription() {
        return pullRequestDescription;
    }

    public String getPathWithNamespace() { return pathWithNamespace; }

    public Integer getPullRequestId() {
        return pullRequestId;
    }

    public Integer getPullRequestIid() {
        return pullRequestIid;
    }

    public Integer getPullRequestTargetProjectId() {
        return pullRequestTargetProjectId;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getTargetRepoName() {
        return targetRepoName;
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public String getTargetRepoSshUrl() {
        return targetRepoSshUrl;
    }

    public String getTargetRepoHttpUrl() {
        return targetRepoHttpUrl;
    }

    public String getTriggeredByUser() {
        return triggeredByUser;
    }

    public String getBefore() {
        return before;
    }

    public String getAfter() {
        return after;
    }

    public String getLastCommit() {
        return lastCommit;
    }

    public String getTargetProjectUrl() {
        return targetProjectUrl;
    }

    public String getRef() { return ref; }

    public String getIsTag() { return isTag; }

    public String getSha() { return sha; }

    public String getBeforeSha() {return beforeSha; }

    public String getStatus() { return status; }

    public String getStages() { return stages; }

    public String getCreatedAt() { return createdAt; }

    public String getFinishedAt() { return finishedAt; }

    public String getBuildDuration() { return buildDuration; }

    public String getJsonBody() { return jsonBody; }

    public String getNoteBody() { return noteBody; }


    String getShortDescription() {
        return actionType.getShortDescription(this);
    }

    public String getPullRequestState() {
		return pullRequestState;
	}

	public String getMergedByUser() {
		return mergedByUser;
	}

	public String getPullRequestAssignee() {
		return pullRequestAssignee;
	}


	public boolean getCreated() {
        return created;
    }

    public boolean getDeleted() {
        return deleted;
    }

	public PullRequest getPullRequest() {
        if (pullRequestId == null) {
            return null;
        }

        return new PullRequest(pullRequestId, pullRequestIid, sourceBranch, targetBranch, pullRequestTitle,
            sourceProjectId, targetProjectId, pullRequestDescription, pullRequestState, pathWithNamespace);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CauseData causeData = (CauseData) o;
        return new EqualsBuilder()
            .append(actionType, causeData.actionType)
            .append(sourceProjectId, causeData.sourceProjectId)
            .append(targetProjectId, causeData.targetProjectId)
            .append(branch, causeData.branch)
            .append(sourceBranch, causeData.sourceBranch)
            .append(userName, causeData.userName)
            .append(userEmail, causeData.userEmail)
            .append(sourceRepoHomepage, causeData.sourceRepoHomepage)
            .append(sourceRepoName, causeData.sourceRepoName)
            .append(sourceNamespace, causeData.sourceNamespace)
            .append(sourceRepoUrl, causeData.sourceRepoUrl)
            .append(sourceRepoSshUrl, causeData.sourceRepoSshUrl)
            .append(sourceRepoHttpUrl, causeData.sourceRepoHttpUrl)
            .append(pullRequestTitle, causeData.pullRequestTitle)
            .append(pullRequestDescription, causeData.pullRequestDescription)
            .append(pullRequestId, causeData.pullRequestId)
            .append(pullRequestIid, causeData.pullRequestIid)
            .append(pullRequestState, causeData.pullRequestState)
            .append(mergedByUser, causeData.mergedByUser)
            .append(pullRequestAssignee, causeData.pullRequestAssignee)
            .append(pullRequestTargetProjectId, causeData.pullRequestTargetProjectId)
            .append(targetBranch, causeData.targetBranch)
            .append(targetRepoName, causeData.targetRepoName)
            .append(targetNamespace, causeData.targetNamespace)
            .append(targetRepoSshUrl, causeData.targetRepoSshUrl)
            .append(targetRepoHttpUrl, causeData.targetRepoHttpUrl)
            .append(triggeredByUser, causeData.triggeredByUser)
            .append(before, causeData.before)
            .append(after, causeData.after)
            .append(lastCommit, causeData.lastCommit)
            .append(targetProjectUrl, causeData.targetProjectUrl)
            .append(ref, causeData.getRef())
            .append(isTag, causeData.getIsTag())
            .append(sha, causeData.getSha())
            .append(beforeSha, causeData.getBeforeSha())
            .append(status, causeData.getStatus())
            .append(stages, causeData.getStages())
            .append(createdAt, causeData.getCreatedAt())
            .append(finishedAt, causeData.getFinishedAt())
            .append(buildDuration, causeData.getBuildDuration())
            .append(pathWithNamespace, causeData.getPathWithNamespace())
            .append(created, causeData.getCreated())
            .append(deleted, causeData.getDeleted())
            .append(jsonBody, causeData.getJsonBody())
            .append(noteBody, causeData.getNoteBody())
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(actionType)
            .append(sourceProjectId)
            .append(targetProjectId)
            .append(branch)
            .append(sourceBranch)
            .append(userName)
            .append(userEmail)
            .append(sourceRepoHomepage)
            .append(sourceRepoName)
            .append(sourceNamespace)
            .append(sourceRepoUrl)
            .append(sourceRepoSshUrl)
            .append(sourceRepoHttpUrl)
            .append(pullRequestTitle)
            .append(pullRequestDescription)
            .append(pullRequestId)
            .append(pullRequestIid)
            .append(pullRequestState)
            .append(mergedByUser)
            .append(pullRequestAssignee)
            .append(pullRequestTargetProjectId)
            .append(targetBranch)
            .append(targetRepoName)
            .append(targetNamespace)
            .append(targetRepoSshUrl)
            .append(targetRepoHttpUrl)
            .append(triggeredByUser)
            .append(before)
            .append(after)
            .append(lastCommit)
            .append(targetProjectUrl)
            .append(ref)
            .append(isTag)
            .append(sha)
            .append(beforeSha)
            .append(status)
            .append(stages)
            .append(createdAt)
            .append(finishedAt)
            .append(buildDuration)
            .append(pathWithNamespace)
            .append(created)
            .append(deleted)
            .append(jsonBody)
            .append(noteBody)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("actionType", actionType)
            .append("sourceProjectId", sourceProjectId)
            .append("targetProjectId", targetProjectId)
            .append("branch", branch)
            .append("sourceBranch", sourceBranch)
            .append("userName", userName)
            .append("userEmail", userEmail)
            .append("sourceRepoHomepage", sourceRepoHomepage)
            .append("sourceRepoName", sourceRepoName)
            .append("sourceNamespace", sourceNamespace)
            .append("sourceRepoUrl", sourceRepoUrl)
            .append("sourceRepoSshUrl", sourceRepoSshUrl)
            .append("sourceRepoHttpUrl", sourceRepoHttpUrl)
            .append("pullRequestTitle", pullRequestTitle)
            .append("pullRequestDescription", pullRequestDescription)
            .append("pullRequestId", pullRequestId)
            .append("pullRequestIid", pullRequestIid)
            .append("pullRequestState", pullRequestState)
            .append("mergedByUser", mergedByUser)
            .append("pullRequestAssignee", pullRequestAssignee)
            .append("pullRequestTargetProjectId", pullRequestTargetProjectId)
            .append("targetBranch", targetBranch)
            .append("targetRepoName", targetRepoName)
            .append("targetNamespace", targetNamespace)
            .append("targetRepoSshUrl", targetRepoSshUrl)
            .append("targetRepoHttpUrl", targetRepoHttpUrl)
            .append("triggeredByUser", triggeredByUser)
            .append("before", before)
            .append("after", after)
            .append("lastCommit", lastCommit)
            .append("targetProjectUrl", targetProjectUrl)
            .append("ref", ref)
            .append("isTag", isTag)
            .append("sha", sha)
            .append("beforeSha", beforeSha)
            .append("status", status)
            .append("stages", stages)
            .append("createdAt", createdAt)
            .append("finishedAt", finishedAt)
            .append("duration", buildDuration)
            .append("pathWithNamespace", pathWithNamespace)
            .append("created", created)
            .append("deleted", deleted)
            .append("jsonBody", jsonBody)
            .append("noteBody", noteBody)
            .toString();
    }

    public enum ActionType {
        PUSH {
            @Override
            String getShortDescription(CauseData data) {
                return getShortDescriptionPush(data);
            }
        }, TAG_PUSH {
            @Override
            String getShortDescription(CauseData data) {
                return getShortDescriptionPush(data);
            }
        }, MERGE {
            @Override
            String getShortDescription(CauseData data) {
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch()) ? "" : data.getSourceNamespace() + "/";
                if (Jenkins.getActiveInstance().getMarkupFormatter() instanceof EscapedMarkupFormatter || data.getTargetProjectUrl() == null) {
                    return Messages.GiteeWebHookCause_ShortDescription_PullRequestHook_plain(String.valueOf(data.getPullRequestIid()),
                                                                                               forkNamespace + data.getSourceBranch(),
                                                                                               data.getTargetBranch());
                } else {
                    return Messages.GiteeWebHookCause_ShortDescription_PullRequestHook_html(String.valueOf(data.getPullRequestIid()),
                                                                                              forkNamespace + data.getSourceBranch(),
                                                                                              data.getTargetBranch(),
                                                                                              data.getTargetProjectUrl());
                }
            }
        }, NOTE {
            @Override
            String getShortDescription(CauseData data) {
                String triggeredBy = data.getTriggeredByUser();
                String forkNamespace = StringUtils.equals(data.getSourceNamespace(), data.getTargetBranch()) ? "" : data.getSourceNamespace() + "/";
                if (Jenkins.getActiveInstance().getMarkupFormatter() instanceof EscapedMarkupFormatter || data.getTargetProjectUrl() == null) {
                    return Messages.GiteeWebHookCause_ShortDescription_NoteHook_plain(triggeredBy,
                        String.valueOf(data.getPullRequestIid()),
                        forkNamespace + data.getSourceBranch(),
                        data.getTargetBranch());
                } else {
                    return Messages.GiteeWebHookCause_ShortDescription_NoteHook_html(triggeredBy,
                        String.valueOf(data.getPullRequestIid()),
                        forkNamespace + data.getSourceBranch(),
                        data.getTargetBranch(),
                        data.getTargetProjectUrl());
                }
            }
        }, PIPELINE {
                @Override
                String getShortDescription(CauseData data) {
                    String getStatus = data.getStatus();
                    if (getStatus == null) {
                       return Messages.GiteeWebHookCause_ShortDescription_PipelineHook_noStatus();
                    } else {
                      return Messages.GiteeWebHookCause_ShortDescription_PipelineHook(getStatus);
                    }
                }
        };

        private static String getShortDescriptionPush(CauseData data) {
            String pushedBy = data.getTriggeredByUser();
            if (pushedBy == null) {
                return Messages.GiteeWebHookCause_ShortDescription_PushHook_noUser();
            } else {
                return Messages.GiteeWebHookCause_ShortDescription_PushHook(pushedBy);
            }
        }

        abstract String getShortDescription(CauseData data);
    }

    private static class MapWrapper<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> map;

        MapWrapper(Map<K, V> map) {
            this.map = map;
        }

        @Override
        public V put(K key, V value) {
            return map.put(key, value);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return map.entrySet();
        }

        void putIfNotNull(K key, V value) {
            if (value != null) {
                map.put(key, value);
            }
        }
    }
}
