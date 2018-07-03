package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.api.GiteeClientBuilder;
import hudson.ProxyConfiguration;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.junit.MockServerRule;

import static com.gitee.jenkins.gitee.api.impl.TestUtility.assertApiImpl;
import static com.gitee.jenkins.gitee.api.impl.TestUtility.buildClientWithDefaults;
import static junit.framework.TestCase.assertNotNull;


public class ResteasyGiteeClientBuilderTest {
    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void buildClient() throws Exception {
        GiteeClientBuilder clientBuilder = new ResteasyGiteeClientBuilder("test", 0, V3GiteeApiProxy.class, null);
        assertApiImpl(buildClientWithDefaults(clientBuilder, "http://localhost/"), V3GiteeApiProxy.class);
    }

    @Test
    public void buildClientWithProxy() throws Exception {
        jenkins.getInstance().proxy = new ProxyConfiguration("example.com", 8080, "test", "test", "*localhost*");
        GiteeClientBuilder clientBuilder = new ResteasyGiteeClientBuilder("test", 0, V3GiteeApiProxy.class, null);
        assertNotNull(buildClientWithDefaults(clientBuilder, "http://localhost"));
    }

}
