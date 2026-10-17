package com.gitee.jenkins.gitee.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Date;
import java.util.Optional;

/**
 * @author Nikolay Ustinov
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class NoteObjectAttributes {

    private Integer id;
    private String body;
    private Integer authorId;
    private Integer projectId;
    private Date createdAt;
    private Date updatedAt;
    private String htmlUrl;
    private String commitId;
    private User user;

    public Optional<Integer> getId() {
        return Optional.ofNullable(id);
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Optional<Integer> getProjectId() {
        return Optional.ofNullable(projectId);
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Optional<Integer> getAuthorId() {
        return Optional.ofNullable(authorId);
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Optional<Date> getCreatedAt() {
        return Optional.ofNullable(createdAt);
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Optional<Date> getUpdatedAt() {
        return Optional.ofNullable(updatedAt);
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Optional<String> getBody() {
        return Optional.ofNullable(body);
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Optional<String> getHtmlUrl() {
        return Optional.ofNullable(htmlUrl);
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Optional<String> getCommitId() {
        return Optional.ofNullable(commitId);
    }

    public void setCommitId(String commitId) {
        this.commitId = commitId;
    }

    public Optional<User> getUser() {
        return Optional.ofNullable(user);
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NoteObjectAttributes that = (NoteObjectAttributes) o;
        return new EqualsBuilder()
            .append(id, that.id)
            .append(body, that.body)
            .append(projectId, that.projectId)
            .append(authorId, that.authorId)
            .append(createdAt, that.createdAt)
            .append(updatedAt, that.updatedAt)
            .append(htmlUrl, that.htmlUrl)
            .append(commitId, that.commitId)
            .append(user, that.user)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(body)
            .append(projectId)
            .append(authorId)
            .append(createdAt)
            .append(updatedAt)
            .append(htmlUrl)
            .append(commitId)
            .append(user)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("body", body)
            .append("projectId", projectId)
            .append("authorId", authorId)
            .append("createdAt", createdAt)
            .append("updatedAt", updatedAt)
            .append("htmlUrl", htmlUrl)
            .append("commitId", commitId)
            .append("user", user)
            .toString();
    }
}
