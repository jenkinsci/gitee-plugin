package com.gitee.jenkins.gitee.api.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import net.karneim.pojobuilder.GeneratePojoBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class WebHook {
    private String url;
    private String title;
    private int encryptionType;
    private boolean pushEvents;
    private boolean tagPushEvents;
    private boolean issuesEvents;
    private boolean noteEvents;
    private boolean mergeRequestsEvents;

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public int getEncryptionType() {
        return encryptionType;
    }

    public boolean getPushEvents() {
        return pushEvents;
    }

    public boolean getTagPushEvents() {
        return tagPushEvents;
    }

    public boolean getIssuesEvents() {
        return issuesEvents;
    }

    public boolean getNoteEvents() {
        return noteEvents;
    }

    public boolean getMergeRequestsEvents() {
        return mergeRequestsEvents;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setEncryptionType(int encryptionType) {
        this.encryptionType = encryptionType;
    }

    public void setPushEvents(boolean pushEvents) {
        this.pushEvents = pushEvents;
    }

    public void setTagPushEvents(boolean tagPushEvents) {
        this.tagPushEvents = tagPushEvents;
    }

    public void setIssuesEvents(boolean issuesEvents) {
        this.issuesEvents = issuesEvents;
    }

    public void setNoteEvents(boolean noteEvents) {
        this.noteEvents = noteEvents;
    }

    public void setMergeRequestsEvents(boolean mergeRequestsEvents) {
        this.mergeRequestsEvents = mergeRequestsEvents;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebHook hook = (WebHook) o;
        return new EqualsBuilder()
                .append(url, hook.url)
                .append(title, hook.title)
                .append(encryptionType, hook.encryptionType)
                .append(pushEvents, hook.pushEvents)
                .append(tagPushEvents, hook.tagPushEvents)
                .append(issuesEvents, hook.issuesEvents)
                .append(noteEvents, hook.noteEvents)
                .append(mergeRequestsEvents, hook.mergeRequestsEvents)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(url)
                .append(title)
                .append(encryptionType)
                .append(pushEvents)
                .append(tagPushEvents)
                .append(issuesEvents)
                .append(noteEvents)
                .append(mergeRequestsEvents)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("url", url)
                .append("title", title)
                .append("encryptionType", encryptionType)
                .append("pushEvents", pushEvents)
                .append("tagPushEvents", tagPushEvents)
                .append("issuesEvents", issuesEvents)
                .append("noteEvents", noteEvents)
                .append("mergeRequestsEvents", mergeRequestsEvents)
                .toString();
    }
}