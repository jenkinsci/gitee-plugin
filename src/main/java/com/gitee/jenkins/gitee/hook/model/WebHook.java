package com.gitee.jenkins.gitee.hook.model;

import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Robin Müller
 * @author Yashin Luo
 */
public abstract class WebHook {

    private Repository repository;
    private String objectKind;
    private String hookName;
    private String jsonBody;
    private User sender;

    public Optional<String> getJsonBody() { return  Optional.ofNullable(this.jsonBody); }

    public void setJsonBody(String json) { this.jsonBody = json; }

    public Optional<String> getHookName() {
        return Optional.ofNullable(this.hookName);
    }

    public void setHookName(String hookName) {
        this.hookName = hookName;
    }

    public Optional<String> getObjectKind() {
        return Optional.ofNullable(objectKind);
    }

    public void setObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }

    public Optional<Repository> getRepository() {
        return Optional.ofNullable(repository);
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Optional<User> getSender() {
        return Optional.ofNullable(sender);
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public String getWebHookDescription() {
        return hookName;
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
                .append(sender, webHook.sender)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(repository)
                .append(hookName)
                .append(objectKind)
                .append(sender)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("repository", repository)
                .append("hookName", hookName)
                .append("objectKind", objectKind)
                .append("sender", sender)
                .toString();
    }
}
