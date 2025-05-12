package com.gitee.jenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerExtension;
import org.mockserver.model.HttpRequest;

// Adapted heavily from GitLab tests
@WithJenkins
@ExtendWith(MockServerExtension.class)
public class GiteeConnectionConfigTest {
    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";
    
    private JenkinsRule jenkins;

    private static MockServerClient mockServerClient;
    private String giteeUrl;

    @BeforeAll
    static void setUp(MockServerClient client) {
        mockServerClient = client;
    }

    @BeforeEach
    void setUp(JenkinsRule rule) throws Exception {
        jenkins = rule;
        giteeUrl = "http://localhost:" + mockServerClient.getPort() + "/gitee";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new GiteeApiTokenImpl(
                                CredentialsScope.SYSTEM,
                                API_TOKEN_ID,
                                "Gitee API Token",
                                Secret.fromString(API_TOKEN)));
            }
        }
    }

    @AfterEach
    void tearDown() {
        mockServerClient.reset();
    }

    @Test
    void doCheckConnection_success() {
        String expected = connection_success();
        assertThat(doCheckConnection("v5", Response.Status.OK), is(expected));
    }

    private String doCheckConnection(String clientBuilderId, Response.Status status) {
        HttpRequest request =
                request().withPath("/gitee/api/" + clientBuilderId + "/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(status.getStatusCode()));

        GiteeConnectionConfig descriptor = (GiteeConnectionConfig) Jenkins.get().getDescriptor(GiteeConnectionConfig.class);
        FormValidation formValidation =
                descriptor.doTestConnection(giteeUrl, API_TOKEN_ID, clientBuilderId, false, 10, 10);
        mockServerClient.verify(request);
        return formValidation.getMessage();
    }
}
