package com.gitee.jenkins.gitee.api.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Label {
    /*
          "name" : "bug",
      "color" : "#d9534f",
      "description": "Bug reported by user",
      "open_issues_count": 1,
      "closed_issues_count": 0,
      "open_merge_requests_count": 1
     */
    private String name;
    private String color;
    private String description;
    private long openIssuesCount;
    private long closedIssuesCount;
    private long openPullRequestsCount;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(long openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public long getClosedIssuesCount() {
        return closedIssuesCount;
    }

    public void setClosedIssuesCount(long closedIssuesCount) {
        this.closedIssuesCount = closedIssuesCount;
    }

    public long getOpenPullRequestsCount() {
        return openPullRequestsCount;
    }

    public void setOpenPullRequestsCount(long openPullRequestsCount) {
        this.openPullRequestsCount = openPullRequestsCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Label label = (Label) o;
        return new EqualsBuilder()
            .append(openIssuesCount, label.openIssuesCount)
            .append(closedIssuesCount, label.closedIssuesCount)
            .append(openPullRequestsCount, label.openPullRequestsCount)
            .append(name, label.name)
            .append(color, label.color)
            .append(description, label.description)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
            .append(name)
            .append(color)
            .append(description)
            .append(openIssuesCount)
            .append(closedIssuesCount)
            .append(openPullRequestsCount)
            .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("name", name)
            .append("color", color)
            .append("description", description)
            .append("openIssuesCount", openIssuesCount)
            .append("closedIssuesCount", closedIssuesCount)
            .append("openPullRequestsCount", openPullRequestsCount)
            .toString();
    }
}
