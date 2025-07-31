package com.gitee.jenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Robin Müller
 */
class RegexBasedFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {"feature/test", "feature/awesome-feature"})
    void isRegexBranchAllowed(String branchName) {
        RegexBasedFilter featureBranches = new RegexBasedFilter("feature/.*");

        assertThat(featureBranches.isBranchAllowed(branchName), is(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"hotfix/test", "hotfix/awesome-feature", "master", "develop"})
    void isRegexBranchNotAllowed(String branchName) {
        RegexBasedFilter featureBranches = new RegexBasedFilter("feature/.*");

        assertThat(featureBranches.isBranchAllowed(branchName), is(false));
    }
}
