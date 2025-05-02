package com.gitee.jenkins.gitee.api.model;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;

import net.karneim.pojobuilder.GeneratePojoBuilder;

@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class RepoUser {
    private int contributions;
    private String email;
    private String name;

    public RepoUser() {}

    public RepoUser(int contributions, String email, String name) {
        this.contributions = contributions;
        this.email = email;
        this.name = name;
    }

    public int getContributions() {
        return contributions;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public void setContributions(int contributions) {
        this.contributions = contributions;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RepoUser that = (RepoUser) o;
        return new EqualsBuilder()
                .append(contributions, this.contributions)
                .append(name, this.name)
                .append(email, this.email)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(contributions)
                .append(name)
                .append(email)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("contributions", contributions)
                .append("name", name)
                .append("email", email)
                .toString();
    }
}
