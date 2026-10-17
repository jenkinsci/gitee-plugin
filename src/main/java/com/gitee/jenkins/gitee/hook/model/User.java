package com.gitee.jenkins.gitee.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;

import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class User {

    private Integer id;
    private String name;
    private String username;
    private String email;
    private String avatarUrl;

    public Optional<Integer> getId() {
        return Optional.ofNullable(id);  
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Optional<String> getAvatarUrl() {
        return Optional.ofNullable(avatarUrl);
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return new EqualsBuilder()
                .append(id, user.id)
                .append(name, user.name)
                .append(username, user.username)
                .append(email, user.email)
                .append(avatarUrl, user.avatarUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(username)
                .append(email)
                .append(avatarUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("username", username)
                .append("email", email)
                .append("avatarUrl", avatarUrl)
                .toString();
    }
}
