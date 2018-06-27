package com.dabsquared.gitlabjenkins.gitlab.hook.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
public abstract class WebHook {

    private Repository repository;
    private String objectKind;
    private String hookName;

    public String getHookName() {
        return this.hookName;
    }

    public void setHookName(String hookName) {
        this.hookName = hookName;
    }

    public String getObjectKind() {
        return objectKind;
    }

    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebHook webHook = (WebHook) o;
        return new EqualsBuilder()
                .append(repository, webHook.repository)
                .append(objectKind, webHook.objectKind)
                .append(hookName, webHook.hookName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(repository)
                .append(hookName)
                .append(objectKind)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("repository", repository)
                .append("hookName", hookName)
                .append("objectKind", objectKind)
                .toString();
    }
}
