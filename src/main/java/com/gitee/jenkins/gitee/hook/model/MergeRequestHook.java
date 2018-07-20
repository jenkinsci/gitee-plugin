package com.gitee.jenkins.gitee.hook.model;


import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.List;

/**
 * @author Robin Müller
 * @author Yashin Luo
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class MergeRequestHook extends WebHook {

    private User user;
    private User assignee;
    private Project repo;
    private Action action;
    private State state;
    private MergeRequestObjectAttributes pullRequest;
    private List<MergeRequestLabel> labels;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public State getState() {
        return this.state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Project getRepo() {
        return repo;
    }

    public void setRepo(Project repo) {
        this.repo = repo;
    }

    public MergeRequestObjectAttributes getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(MergeRequestObjectAttributes pullRequest) {
        this.pullRequest = pullRequest;
    }

    public List<MergeRequestLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<MergeRequestLabel> labels) {
        this.labels = labels;
    }

    public String getWebHookDescription() {
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
        MergeRequestHook that = (MergeRequestHook) o;
        return new EqualsBuilder()
                .append(user, that.user)
                .append(assignee, that.assignee)
                .append(repo, that.repo)
                .append(action, that.action)
                .append(state, that.state)
                .append(pullRequest, that.pullRequest)
                .append(labels, that.labels)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(user)
                .append(assignee)
                .append(repo)
                .append(pullRequest)
                .append(labels)
                .append(state)
                .append(action)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("assignee", assignee)
                .append("repo", repo)
                .append("state", state)
                .append("action", action)
                .append("pullRequest", pullRequest)
                .append("labels", labels)
                .toString();
    }
}
