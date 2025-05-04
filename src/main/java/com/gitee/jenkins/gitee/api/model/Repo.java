package com.gitee.jenkins.gitee.api.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Repo {
    private static final String COMMITTERS = "committers";
    private static final String AUTHORS = "authors";

    private String owner;
    private String repo;
    private String type;

    public Repo() {}

    public Repo(String owner, String repo, String type) {
        this.owner = owner;
        this.repo = repo;
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepo() {
        return repo;
    }

    public String getType() {
        return type;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public void setType(String type) {
        if (type.equals(AUTHORS) || type.equals(COMMITTERS)) {
            this.type = type;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Repo that = (Repo) o;
        return new EqualsBuilder()
                .append(owner, that.owner)
                .append(repo, that.repo)
                .append(type, that.type)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(owner)
                .append(repo)
                .append(type)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("owner", owner)
                .append("repo", repo)
                .append("type", type)
                .toString();
    }
}
