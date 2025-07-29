package com.gitee.jenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Müller
 */
class PullRequestLabelFilterImplTest {

    @Test
    void includeLabels() {
        PullRequestLabelFilterImpl pullRequestLabelFilter = new PullRequestLabelFilterImpl("include, include2", "");

        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("include")), is(true));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("include2")), is(true));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("other-label")), is(false));
    }

    @Test
    void excludeLabels() {
        PullRequestLabelFilterImpl pullRequestLabelFilter = new PullRequestLabelFilterImpl("", "exclude, exclude2");

        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("exclude")), is(false));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("exclude2")), is(false));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("other-label")), is(true));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.emptySet()), is(true));
    }

    @Test
    void includeAndExcludeLabels() {
        PullRequestLabelFilterImpl pullRequestLabelFilter =
                new PullRequestLabelFilterImpl("include, include2", "exclude, exclude2");

        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("include")), is(true));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("include2")), is(true));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("exclude")), is(false));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("exclude2")), is(false));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Collections.singleton("other-label")), is(false));
        assertThat(pullRequestLabelFilter.isPullRequestAllowed(Arrays.asList("include", "exclude")), is(false));
    }
}
