package com.gitee.jenkins.gitee.api.impl;

import com.gitee.jenkins.gitee.api.GiteeClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static com.gitee.jenkins.gitee.api.impl.TestUtility.*;
import static org.junit.Assert.fail;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.matchers.Times.once;

public class AutodetectingGiteeClientTest {
    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private MockServerClient mockServerClient;
    private String gitLabUrl;
    private GiteeClientBuilder clientBuilder;
    private AutodetectingGiteeClient api;
    private HttpRequest v3Request;
    private HttpRequest v4Request;

    @Before
    public void setup() throws IOException {
        gitLabUrl = "http://localhost:" + mockServer.getPort() + "/gitee";
        addGiteeApiToken();

        List<GiteeClientBuilder> builders = Arrays.<GiteeClientBuilder>asList(new V3GiteeClientBuilder(), new V4GiteeClientBuilder());
        api = new AutodetectingGiteeClient(builders, gitLabUrl, API_TOKEN, true, 10, 10);

        v3Request = versionRequest(V3GiteeApiProxy.ID);
        v4Request = versionRequest(V4GiteeApiProxy.ID);
    }

    @Test
    public void buildClient_success_v3() throws Exception {
        mockServerClient.when(v3Request).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V3GiteeApiProxy.class);
        mockServerClient.verify(v3Request, v3Request);
    }

    @Test
    public void buildClient_success_v4() throws Exception {
        mockServerClient.when(v3Request).respond(responseNotFound());
        mockServerClient.when(v4Request).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V4GiteeApiProxy.class);
        mockServerClient.verify(v3Request, v4Request, v4Request);
    }

    @Test
    public void buildClient_success_switching_apis() throws Exception {
        mockServerClient.when(v3Request, once()).respond(responseNotFound());
        mockServerClient.when(v4Request, exactly(2)).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V4GiteeApiProxy.class);

        mockServerClient.when(v4Request, once()).respond(responseNotFound());
        mockServerClient.when(v3Request, exactly(2)).respond(responseOk());
        api.getCurrentUser();
        assertApiImpl(api, V3GiteeApiProxy.class);

        mockServerClient.verify(v3Request, v4Request, v4Request, v3Request, v3Request);
    }

    @Test
    public void buildClient_no_match() {
        mockServerClient.when(v3Request).respond(responseNotFound());
        mockServerClient.when(v4Request).respond(responseNotFound());
        try {
            api.getCurrentUser();
            fail("endpoint should throw exception when no matching delegate is found");
        } catch (NoSuchElementException e) {
            mockServerClient.verify(v3Request, v4Request);
        }
    }
}
