package com.gitee.jenkins.gitee.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.Date;

/**
 * @author Robin Müller
 * @author Yashin
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class PullRequestObjectAttributes {
    private Integer id;
    private Integer number;
    private Integer authorId;
    private Integer assigneeId;
    private String title;
    private Date createdAt;
    private Date updatedAt;
    private String body;
    private BranchData head;
    private BranchData base;
    private String mergeStatus;
    private boolean mergeable;
    private boolean needReview;
    private boolean needTest;
    private String mergeCommitSha;
    private String mergeReferenceName;
    private String htmlUrl;
    private Boolean workInProgress;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getSourceBranch() {
        return head.getRef();
    }

    public String getTargetBranch() {
        return base.getRef();
    }

    public Integer getSourceProjectId() {
        return head.getRepo().getId();
    }

    public Integer getTargetProjectId() {
        return base.getRepo().getId();
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public Integer getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Integer assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public Project getSource() {
        return head.getRepo();
    }

    public BranchData getHead() {
        return head;
    }

    public void setHead(BranchData head) {
        this.head = head;
    }

    public BranchData getBase() {
        return base;
    }

    public void setBase(BranchData base) {
        this.base = base;
    }

    public Project getTarget() {
        return base.getRepo();
    }

    public boolean getNeedTest() {
        return needTest;
    }

    public void setNeedTest(boolean needTest) {
        this.needTest = needTest;
    }

    public boolean getNeedReview() {
        return needReview;
    }

    public void setNeedReview(boolean needReview) {
        this.needReview = needReview;
    }

    public String getMergeCommitSha() {
        return mergeCommitSha;
    }

    public void setMergeCommitSha(String mergeCommitSha) {
        this.mergeCommitSha = mergeCommitSha;
    }

    public String getMergeReferenceName() {
        return mergeReferenceName;
    }

    public void setMergeReferenceName(String mergeReferenceName) {
        this.mergeReferenceName = mergeReferenceName;
    }

    public String getMergeStatus() {
        return mergeStatus;
    }

    public void setMergeStatus(String mergeStatus) {
        this.mergeStatus = mergeStatus;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public Boolean getWorkInProgress() {
        return workInProgress;
    }

    public void setWorkInProgress(Boolean workInProgress) {
        this.workInProgress = workInProgress;
    }

    public boolean getMergeable() {
        return mergeable;
    }

    public void setMergeable(boolean mergeable) {
        this.mergeable = mergeable;
    }

    public boolean isMergeable() {
        return mergeable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PullRequestObjectAttributes that = (PullRequestObjectAttributes) o;
        return new EqualsBuilder()
            .append(id, that.id)
            .append(number, that.number)
            .append(authorId, that.authorId)
            .append(assigneeId, that.assigneeId)
            .append(title, that.title)
            .append(createdAt, that.createdAt)
            .append(updatedAt, that.updatedAt)
            .append(body, that.body)
            .append(head, that.head)
            .append(base, that.base)
            .append(mergeCommitSha, that.mergeCommitSha)
            .append(mergeReferenceName, that.mergeReferenceName)
            .append(mergeStatus, that.mergeStatus)
            .append(mergeable, that.mergeable)
            .append(needReview, that.needReview)
            .append(needTest, that.needTest)
            .append(htmlUrl, that.htmlUrl)
            .append(workInProgress, that.workInProgress)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(id)
            .append(number)
            .append(authorId)
            .append(assigneeId)
            .append(title)
            .append(createdAt)
            .append(updatedAt)
            .append(body)
            .append(head)
            .append(base)
            .append(mergeStatus)
            .append(mergeable)
            .append(needReview)
            .append(needTest)
            .append(mergeCommitSha)
            .append(mergeReferenceName)
            .append(htmlUrl)
            .append(workInProgress)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("id", id)
            .append("number", number)
            .append("authorId", authorId)
            .append("assigneeId", assigneeId)
            .append("title", title)
            .append("createdAt", createdAt)
            .append("updatedAt", updatedAt)
            .append("body", body)
            .append("head", head)
            .append("base", base)
            .append("mergeCommitSha", mergeCommitSha)
            .append("mergeReferenceName", mergeReferenceName)
            .append("mergeStatus", mergeStatus)
            .append("mergeable", mergeable)
            .append("needReview", needReview)
            .append("needTest", needTest)
            .append("htmlUrl", htmlUrl)
            .append("workInProgress", workInProgress)
            .toString();
    }
}
