/**
 * Adapted from GitLab automated tests
 * https://github.com/jenkinsci/gitlab-plugin/tree/master/src/test/java/com/dabsquared/gitlabjenkins
 */

package com.gitee.jenkins.publisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.connection.GiteeConnection;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.impl.GiteeV5ClientBuilder;
import com.gitee.jenkins.gitee.api.model.PullRequest;

import hudson.Launcher;
import hudson.matrix.MatrixAggregatable;
import hudson.matrix.MatrixAggregator;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.git.util.BuildData;
import hudson.tasks.Notifier;
import hudson.util.Secret;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.MockServerClient;

final class TestUtility {
    static final String GITEE_CONNECTION_V5 = "GiteeV5";
    static final String BUILD_URL = "/build/123";
    static final String REPO_PATH = "testPath";
    static final String PULL_COMMIT_SHA = "eKJ3wuqJT98Kc8TCcBK7oggLR1E9Bty7eqSHfSLT";
    static final int BUILD_NUMBER = 1;
    static final int PROJECT_ID = 3;
    static final int PULL_REQUEST_ID = 1;
    static final int PULL_REQUEST_IID = 2;

    private static final String API_TOKEN = "secret";

    static void setupGiteeConnections(JenkinsRule jenkins, MockServerClient client) throws Exception {
        GiteeConnectionConfig connectionConfig = jenkins.get(GiteeConnectionConfig.class);
        String apiTokenId = "apiTokenId";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstanceOrNull())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(
                        domains.get(0),
                        new StringCredentialsImpl(
                                CredentialsScope.SYSTEM,
                                apiTokenId,
                                "Gitee API Token",
                                Secret.fromString(TestUtility.API_TOKEN)));
            }
        }
        connectionConfig.addConnection(new GiteeConnection(
                TestUtility.GITEE_CONNECTION_V5,
                "http://localhost:" + client.getPort() + "/gitee",
                apiTokenId,
                new GiteeV5ClientBuilder(),
                false,
                10,
                10));
    }

    static <T extends Notifier & MatrixAggregatable> void verifyMatrixAggregatable(
            Class<T> publisherClass, BuildListener listener) throws Exception {
        AbstractBuild build = mock(AbstractBuild.class);
        AbstractProject project = mock(MatrixConfiguration.class);
        Notifier publisher = mock(publisherClass);
        MatrixBuild parentBuild = mock(MatrixBuild.class);

        when(build.getParent()).thenReturn(project);
        when(((MatrixAggregatable) publisher).createAggregator(any(MatrixBuild.class), any(), any(BuildListener.class)))
                .thenCallRealMethod();
        when(publisher.perform(any(AbstractBuild.class), any(Launcher.class), any(BuildListener.class)))
                .thenReturn(true);

        MatrixAggregator aggregator = ((MatrixAggregatable) publisher).createAggregator(parentBuild, null, listener);
        aggregator.startBuild();
        aggregator.endBuild();
        verify(publisher).perform(parentBuild, null, listener);
    }

    static AbstractBuild mockSimpleBuild(String gitLabConnection, Result result, String... remoteUrls) {
        AbstractBuild build = mock(AbstractBuild.class);
        BuildData buildData = mock(BuildData.class);
        when(buildData.getRemoteUrls()).thenReturn(new HashSet<>(Arrays.asList(remoteUrls)));
        when(build.getAction(BuildData.class)).thenReturn(buildData);
        when(build.getResult()).thenReturn(result);
        when(build.getUrl()).thenReturn(BUILD_URL);
        when(build.getResult()).thenReturn(result);
        when(build.getNumber()).thenReturn(BUILD_NUMBER);

        AbstractProject<?, ?> project = mock(AbstractProject.class);
        when(project.getProperty(GiteeConnectionProperty.class))
                .thenReturn(new GiteeConnectionProperty(gitLabConnection));
        doReturn(project).when(build).getParent();
        doReturn(project).when(build).getProject();
        return build;
    }

    @SuppressWarnings("ConstantConditions")
    static String formatNote(AbstractBuild build, String note) {
        String buildUrl = Jenkins.getInstanceOrNull().getRootUrl() + build.getUrl();
        return MessageFormat.format(
                note, build.getResult(), build.getParent().getDisplayName(), BUILD_NUMBER, buildUrl);
    }

    static <P extends PullRequestNotifier> P preparePublisher(P publisher, AbstractBuild build) {
        P spyPublisher = spy(publisher);
        PullRequest pullRequest = new PullRequest(
                PULL_REQUEST_ID, PULL_REQUEST_IID, PULL_COMMIT_SHA, "", "", PROJECT_ID, PROJECT_ID, "", "", String.format("%d/%s", PROJECT_ID, REPO_PATH));
        doReturn(pullRequest).when(spyPublisher).getPullRequest(build);
        return spyPublisher;
    }

    private TestUtility() {
        /* contains only static utility-methods */
    }
}
