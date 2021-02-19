package com.gitee.jenkins.gitee.hook.model;


import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Nikolay Ustinov
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class NoteHook extends WebHook {

    private User user;
    private Project project;
    private PullRequestObjectAttributes pullRequest;
    private NoteObjectAttributes comment;
    private NoteAction action;

    public NoteAction getAction() {
        return action;
    }

    public void setAction(NoteAction action) {
        this.action = action;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public NoteObjectAttributes getComment() {
        return comment;
    }

    public void setComment(NoteObjectAttributes comment) {
        this.comment = comment;
    }

    public PullRequestObjectAttributes getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(PullRequestObjectAttributes pullRequest) {
        this.pullRequest = pullRequest;
    }

    public String getWebHookDescription() {
        // 兼容commit评论
        if (pullRequest == null) {
            return getHookName() + " commit sha = " + comment.getCommitId();
        }

        return getHookName() + " iid = " + pullRequest.getNumber() + " merge commit sha = " + pullRequest.getMergeCommitSha();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoteHook that = (NoteHook) o;
        return new EqualsBuilder()
                .append(user, that.user)
                .append(action, that.action)
                .append(project, that.project)
                .append(comment, that.comment)
                .append(pullRequest, that.pullRequest)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(user)
                .append(action)
                .append(project)
                .append(comment)
                .append(pullRequest)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("action", action)
                .append("project", project)
                .append("comment", comment)
                .append("pullRequest", pullRequest)
                .toString();
    }
}
