package com.gitee.jenkins.util;

import static com.gitee.jenkins.util.ProjectIdUtilTest.TestData.forRemoteUrl;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.gitee.jenkins.gitee.api.GiteeClient;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Robin Müller
 */
class ProjectIdUtilTest {

    static TestData[] data() {
        return new TestData[] {
            forRemoteUrl("git@gitee.com", "git@gitee.com:test/project.git").expectProjectId("test/project"),
            forRemoteUrl("https://gitee.com", "https://gitee.com/test/project.git")
                    .expectProjectId("test/project"),
            forRemoteUrl("https://myurl.com/gitee", "https://myurl.com/gitee/group/project.git")
                    .expectProjectId("group/project"),
            forRemoteUrl("git@gitee.com", "git@gitee.com:group/subgroup/project.git")
                    .expectProjectId("group/subgroup/project"),
            forRemoteUrl("https://myurl.com/gitee", "https://myurl.com/gitee/group/subgroup/project.git")
                    .expectProjectId("group/subgroup/project"),
            forRemoteUrl("https://myurl.com", "https://myurl.com/group/subgroup/project.git")
                    .expectProjectId("group/subgroup/project"),
            forRemoteUrl("https://myurl.com", "https://myurl.com/group/subgroup/subsubgroup/project.git")
                    .expectProjectId("group/subgroup/subsubgroup/project"),
            forRemoteUrl("git@gitee.com", "git@gitee.com:group/subgroup/subsubgroup/project.git")
                    .expectProjectId("group/subgroup/subsubgroup/project"),
            forRemoteUrl("http://myhost", "http://myhost.com/group/project.git").expectProjectId("group/project"),
            forRemoteUrl("", "http://myhost.com/group/project.git").expectProjectId("group/project"),
            forRemoteUrl("", "http://myhost.com:group/project.git").expectProjectId("group/project"),
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void retrieveProjectId(TestData testData) throws Exception {
        GiteeClient client = new GiteeClientStub(testData.hostUrl);

        String projectId = ProjectIdUtil.retrieveProjectId(client, testData.remoteUrl);

        assertThat(projectId, is(testData.expectedProjectId));
    }

    static final class TestData {

        private final String hostUrl;
        private final String remoteUrl;
        private String expectedProjectId;

        private TestData(String hostUrl, String remoteUrl) {
            this.hostUrl = hostUrl;
            this.remoteUrl = remoteUrl;
        }

        private TestData expectProjectId(String expectedProjectId) {
            this.expectedProjectId = expectedProjectId;
            return this;
        }

        @Override
        public String toString() {
            return remoteUrl;
        }

        static TestData forRemoteUrl(String baseUrl, String remoteUrl) {
            return new TestData(baseUrl, remoteUrl);
        }
    }
}
