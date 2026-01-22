package com.gitee.jenkins.gitee.api.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import net.karneim.pojobuilder.GeneratePojoBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Release {
    private String tagName;
    private String name;
    private String body;
    private Boolean prerelease;
    private String targetCommitish;
    private Integer id;

    public String getTagName() {
        return tagName;
    }

    public String getName() {
        return name;
    }

    public String getBody() {
        return body;
    }

    public Boolean isPrerelease() {
        return prerelease;
    }

    public String getTargetCommitish() {
        return targetCommitish;
    }

    public Integer getId() {
        return id;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrerelease(Boolean prerelease) {
        this.prerelease = prerelease;
    }

    public void setTargetCommitish(String targetCommit) {
        this.targetCommitish = targetCommit;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Release release = (Release) o;
        return new EqualsBuilder()
                .append(tagName, release.tagName)
                .append(name, release.name)
                .append(body, release.body)
                .append(prerelease, release.prerelease)
                .append(targetCommitish, release.targetCommitish)
                .append(id, release.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(tagName)
                .append(name)
                .append(body)
                .append(prerelease)
                .append(targetCommitish)
                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("tagName", tagName)
                .append("name", name)
                .append("body", body)
                .append("prerelease", prerelease)
                .append("targetCommit", targetCommitish)
                .append("id", id)
                .toString();

    }
}
