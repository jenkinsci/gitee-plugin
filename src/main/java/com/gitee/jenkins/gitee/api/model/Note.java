package com.gitee.jenkins.gitee.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;


@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Note {
    private Integer id;
    private Integer projectId;
    private User author;
    private Date createdAt;
    private Date updatedAt;
    private String body;

    public Note() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getProjectId() {
        return projectId;
    }

    public void setprojectId(Integer projectId) {
        this.projectId = projectId;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Note that = (Note) o;
        return new EqualsBuilder()
            .append(id, that.id)
            .append(projectId, that.projectId)
            .append(author, that.author)
            .append(createdAt, that.createdAt)
            .append(updatedAt, that.updatedAt)
            .append(body, that.body)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(projectId)
            .append(author)
            .append(createdAt)
            .append(updatedAt)
            .append(body)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("projectId", projectId)
            .append("author", author)
            .append("createdAt", createdAt)
            .append("updatedAt", updatedAt)
            .append("body", body)
            .toString();
    }
}
