package com.gitee.jenkins.gitee.api;


import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.List;
import java.util.NoSuchElementException;

import static com.gitee.jenkins.gitee.api.GiteeClientBuilder.getAllGiteeClientBuilders;
import static com.gitee.jenkins.gitee.api.GiteeClientBuilder.getGiteeClientBuilderById;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;


public class GiteeClientBuilderTest {
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void getAllGiteeClientBuilders_list_is_sorted_by_ordinal() {
        List<GiteeClientBuilder> builders = getAllGiteeClientBuilders();
        assertThat(builders.get(0), instanceOf(AutodetectGiteeClientBuilder.class));
        assertThat(builders.get(1), instanceOf(V4GiteeClientBuilder.class));
        assertThat(builders.get(2), instanceOf(V3GiteeClientBuilder.class));
    }

    @Test
    public void getGiteeClientBuilderById_success() {
        assertThat(getGiteeClientBuilderById(new V3GiteeClientBuilder().id()), instanceOf(V3GiteeClientBuilder.class));
    }

    @Test(expected = NoSuchElementException.class)
    public void getGiteeClientBuilderById_no_match() {
        getGiteeClientBuilderById("unknown");
    }
}
