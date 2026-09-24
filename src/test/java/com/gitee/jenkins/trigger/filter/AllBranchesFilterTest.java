package com.gitee.jenkins.trigger.filter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

/**
 * @author Robin Müller
 */
class AllBranchesFilterTest {

    @Test
    void isRandomBranchNameAllowed() {
        String randomBranchName = RandomStringUtils.random(10, true, false);

        assertThat(new AllBranchesFilter().isBranchAllowed(randomBranchName), is(true));
    }
}
