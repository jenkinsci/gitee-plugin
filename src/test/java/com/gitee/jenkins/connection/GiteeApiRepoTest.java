package com.gitee.jenkins.connection;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.junit.jupiter.api.Test;

import com.gitee.jenkins.connection.GiteeApiRepoProperty.DescriptorImpl;

import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;

@WithJenkins
public class GiteeApiRepoTest {
    private GiteeApiRepoProperty prop;
    private GiteeApiRepoProperty.DescriptorImpl descriptor;

    private JenkinsRule jenkins;

    @BeforeEach
    void setupProperty(JenkinsRule rule) {
        jenkins = rule;
        prop = new GiteeApiRepoProperty();
        descriptor = (DescriptorImpl) prop.getDescriptor();
    }

    @Test
    void addApiObjectTest() {
        descriptor.addRepoOwner("test", "test");

        ListBoxModel list = descriptor.getDescriptorOptions();
        Option testOption = new Option("test test", "test test");
        assertTrue(list.get(0).value.equals(testOption.value));
    }

    @Test
    void addDuplicateObjectTest() {
        descriptor.addRepoOwner("test", "test");
        descriptor.addRepoOwner("test", "test");

        ListBoxModel list = descriptor.getDescriptorOptions();
        assertTrue(list.size() == 1);
    }

    @Test
    void removeObjectTest() {
        descriptor.addRepoOwner("test", "test");

        ListBoxModel list = descriptor.getDescriptorOptions();
        assertTrue(list.size() == 1);

        descriptor.removeRepoOwner("test test");
        assertTrue(list.size() == 0);
    }

    @Test
    void removeAllObjectsTest() {
        descriptor.addRepoOwner("test1", "test1");
        descriptor.addRepoOwner("test2", "test2");

        ListBoxModel list = descriptor.getDescriptorOptions();
        assertTrue(list.size() == 2);

        descriptor.removeAllRepoOwners();
        assertTrue(list.size() == 0);
    }

    @Test
    void badObjectTest() {
        FormValidation form = descriptor.doCheckRepoOwner("/ /", "/", "/");
        assertTrue(form.kind == Kind.ERROR);

        form = descriptor.doCheckRepoOwner("\n  ", "\n", " ");
        assertTrue(form.kind == Kind.ERROR);

        form = descriptor.doCheckRepoOwner("\\ \\", "\\", "\\");
        assertTrue(form.kind == Kind.ERROR);
    }

    @Test
    void missingRepoTest() {
        FormValidation form = descriptor.doCheckRepoOwner(" owner", "", "owner");
        assertTrue(form.kind == Kind.ERROR);
    }

    @Test
    void missingOwnerTest() {
        FormValidation form = descriptor.doCheckRepoOwner("repo ", "repo", "");
        assertTrue(form.kind == Kind.ERROR);
    }

    @Test
    void goodObjectTest() {
        FormValidation form = descriptor.doCheckRepoOwner("test test", "test", "test");
        assertTrue(form.kind == Kind.OK);
    }

    @Test
    void emptyObjectTest() {
        FormValidation form = descriptor.doCheckRepoOwner("", "", "");
        assertTrue(form.kind == Kind.OK);
        assertTrue(form.getLocalizedMessage().equals(Messages.both_empty_inputs()));
    }
}
