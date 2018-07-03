package com.gitee.jenkins.publisher;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.connection.GiteeConnection;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.model.MergeRequest;
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
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.junit.MockServerRule;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


final class TestUtility {
    static final String GITEE_CONNECTION_V3 = "GiteeV3";
    static final String GITEE_CONNECTION_V4 = "GiteeV4";
    static final String BUILD_URL = "/build/123";
    static final int BUILD_NUMBER = 1;
    static final int PROJECT_ID = 3;
    static final int MERGE_REQUEST_ID = 1;
    static final int MERGE_REQUEST_IID = 2;

    private static final String API_TOKEN = "secret";

    static void setupGiteeConnections(JenkinsRule jenkins, MockServerRule mockServer) throws IOException {
        GiteeConnectionConfig connectionConfig = jenkins.get(GiteeConnectionConfig.class);
        String apiTokenId = "apiTokenId";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, apiTokenId, "Gitee API Token", Secret.fromString(TestUtility.API_TOKEN)));
            }
        }
        connectionConfig.addConnection(new GiteeConnection(TestUtility.GITEE_CONNECTION_V3, "http://localhost:" + mockServer.getPort() + "/gitee", apiTokenId, new V3GiteeClientBuilder(), false, 10, 10));
        connectionConfig.addConnection(new GiteeConnection(TestUtility.GITEE_CONNECTION_V4, "http://localhost:" + mockServer.getPort() + "/gitee", apiTokenId, new V4GiteeClientBuilder(), false, 10, 10));

    }

    static <T  extends Notifier & MatrixAggregatable> void verifyMatrixAggregatable(Class<T> publisherClass, BuildListener listener) throws InterruptedException, IOException {
        AbstractBuild build = mock(AbstractBuild.class);
        AbstractProject project = mock(MatrixConfiguration.class);
        Notifier publisher = mock(publisherClass);
        MatrixBuild parentBuild = mock(MatrixBuild.class);

        when(build.getParent()).thenReturn(project);
        when(((MatrixAggregatable) publisher).createAggregator(any(MatrixBuild.class), any(Launcher.class), any(BuildListener.class))).thenCallRealMethod();
        when(publisher.perform(any(AbstractBuild.class), any(Launcher.class), any(BuildListener.class))).thenReturn(true);

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
        when(project.getProperty(GiteeConnectionProperty.class)).thenReturn(new GiteeConnectionProperty(gitLabConnection));
        when(build.getProject()).thenReturn(project);
        return build;
    }

    @SuppressWarnings("ConstantConditions")
    static String formatNote(AbstractBuild build, String note) {
        String buildUrl = Jenkins.getInstance().getRootUrl() + build.getUrl();
        return MessageFormat.format(note, build.getResult(), build.getParent().getDisplayName(), BUILD_NUMBER, buildUrl);
    }

    static <P extends MergeRequestNotifier> P preparePublisher(P publisher, AbstractBuild build) {
        P spyPublisher = spy(publisher);
        MergeRequest mergeRequest = new MergeRequest(MERGE_REQUEST_ID, MERGE_REQUEST_IID, "", "", "", PROJECT_ID, PROJECT_ID, "", "");
        doReturn(mergeRequest).when(spyPublisher).getMergeRequest(build);
        return spyPublisher;
    }

    private TestUtility() { /* contains only static utility-methods */ }
}
