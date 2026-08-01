package com.gitee.jenkins.gitee.hook.model;

import net.karneim.pojobuilder.GeneratePojoBuilder;

import java.util.Optional;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * @author Robin Müller
 */
@GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
public class Project {

    private Integer id;
    private String name;
    private String description;
    private String webUrl;
    private String avatarUrl;
    private String namespace;
//    private Integer visibilityLevel;
    private String pathWithNamespace;
    private String defaultBranch;
    private String homepage;
    private String url;
    private String sshUrl;
    private String gitHttpUrl;

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Optional<String> getWebUrl() {
        return Optional.ofNullable(webUrl);
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public Optional<String> getAvatarUrl() {
        return Optional.ofNullable(avatarUrl);
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Optional<String> getNamespace() {
        return Optional.ofNullable(namespace);
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

//    public Integer getVisibilityLevel() {
//        return visibilityLevel;
//    }

//    public void setVisibilityLevel(Integer visibilityLevel) {
//        this.visibilityLevel = visibilityLevel;
//    }

    public Optional<String> getPathWithNamespace() {
        return Optional.ofNullable(pathWithNamespace);
    }

    public void setPathWithNamespace(String pathWithNamespace) {
        this.pathWithNamespace = pathWithNamespace;
    }

    public Optional<String> getDefaultBranch() {
        return Optional.ofNullable(defaultBranch);
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public Optional<String> getHomepage() {
        return Optional.ofNullable(homepage);
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Optional<String> getSshUrl() {
        return Optional.ofNullable(sshUrl);
    }

    public void setSshUrl(String sshUrl) {
        this.sshUrl = sshUrl;
    }

    public Optional<String> getGitHttpUrl() {
        return Optional.ofNullable(gitHttpUrl);
    }

    public void setGitHttpUrl(String gitHttpUrl) {
        this.gitHttpUrl = gitHttpUrl;
    }

    public Optional<Integer> getId() {
        return Optional.ofNullable(id);
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
        Project project = (Project) o;
        return new EqualsBuilder()
                .append(id, project.id)
                .append(name, project.name)
                .append(description, project.description)
                .append(webUrl, project.webUrl)
                .append(avatarUrl, project.avatarUrl)
                .append(namespace, project.namespace)
//                .append(visibilityLevel, project.visibilityLevel)
                .append(pathWithNamespace, project.pathWithNamespace)
                .append(defaultBranch, project.defaultBranch)
                .append(homepage, project.homepage)
                .append(url, project.url)
                .append(sshUrl, project.sshUrl)
                .append(gitHttpUrl, project.gitHttpUrl)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(name)
                .append(description)
                .append(webUrl)
                .append(avatarUrl)
                .append(namespace)
//                .append(visibilityLevel)
                .append(pathWithNamespace)
                .append(defaultBranch)
                .append(homepage)
                .append(url)
                .append(sshUrl)
                .append(gitHttpUrl)
                .toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("name", name)
                .append("description", description)
                .append("webUrl", webUrl)
                .append("avatarUrl", avatarUrl)
                .append("namespace", namespace)
//                .append("visibilityLevel", visibilityLevel)
                .append("pathWithNamespace", pathWithNamespace)
                .append("defaultBranch", defaultBranch)
                .append("homepage", homepage)
                .append("url", url)
                .append("sshUrl", sshUrl)
                .append("gitHttpUrl", gitHttpUrl)
                .toString();
    }
}
