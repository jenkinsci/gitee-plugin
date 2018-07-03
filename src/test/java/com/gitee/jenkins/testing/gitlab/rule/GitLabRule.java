package com.gitee.jenkins.testing.gitlab.rule;


import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.gitee.jenkins.connection.GiteeConnection;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.model.MergeRequest;
import com.gitee.jenkins.gitee.api.model.Pipeline;
import com.gitee.jenkins.gitee.api.model.Project;
import com.gitee.jenkins.gitee.api.model.User;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Robin Müller
 */
public class GiteeRule implements TestRule {
    private static final String API_TOKEN_ID = "apiTokenId";
    private static final String PASSWORD = "integration-test";

    private final String url;
    private final int postgresPort;

    private GiteeClient clientCache;

    private List<String> projectIds = new ArrayList<>();

    public GiteeRule(String url, int postgresPort) {
        this.url = url;
        this.postgresPort = postgresPort;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new GiteeStatement(base);
    }

    public Project getProject(final String projectName) {
        return client().getProject(projectName);
    }

    public List<Pipeline> getPipelines(int projectId) {
        return client().getPipelines(String.valueOf(projectId));
    }

    public List<String> getProjectIds() {
        return projectIds;
    }

    public String createProject(ProjectRequest request) {
        Project project = client().createProject(request.getName());
        projectIds.add(project.getId().toString());
        if (request.getWebHookUrl() != null && (request.isPushHook() || request.isMergeRequestHook() || request.isNoteHook())) {
            client().addProjectHook(project.getId().toString(), request.getWebHookUrl(), request.isPushHook(), request.isMergeRequestHook(), request.isNoteHook());
        }
        return project.getHttpUrlToRepo();
    }

    public GiteeConnectionProperty createGiteeConnectionProperty() throws IOException {
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "Gitee API Token", Secret.fromString(getApiToken())));
            }
        }

        GiteeConnectionConfig config = Jenkins.getInstance().getDescriptorByType(GiteeConnectionConfig.class);
        GiteeConnection connection = new GiteeConnection("test", url, API_TOKEN_ID, new V3GiteeClientBuilder(), true,10, 10);
        config.addConnection(connection);
        config.save();
        return new GiteeConnectionProperty(connection.getName());
    }

    public MergeRequest createMergeRequest(final Integer projectId,
                                           final String sourceBranch,
                                           final String targetBranch,
                                           final String title) {
        return client().createMergeRequest(projectId, sourceBranch, targetBranch, title);
    }

    public void createMergeRequestNote(MergeRequest mr, String body) {
        client().createMergeRequestNote(mr, body);
    }

    public String getUsername() {
        return client().getCurrentUser().getUsername();
    }

    public String getPassword() {
        return PASSWORD;
    }

    private void cleanup() {
        for (String projectId : projectIds) {
            String randomProjectName = UUID.randomUUID().toString();
            // rename the project before deleting as the deletion will take a while
            client().updateProject(projectId, randomProjectName, randomProjectName);
            client().deleteProject(projectId);
        }
    }

    private String getApiToken() {
        try {
            Class.forName("org.postgresql.Driver");
            try (Connection connection = DriverManager.getConnection("jdbc:postgresql://localhost:" + postgresPort + "/gitlabhq_production", "gitee", "password")) {
                ResultSet resultSet = connection.createStatement().executeQuery("SELECT authentication_token FROM users WHERE username = 'root'");
                resultSet.next();
                return resultSet.getString("authentication_token");
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private GiteeClient client() {
        if (clientCache == null) {
            clientCache = new V3GiteeClientBuilder().buildClient(url, getApiToken(), false, -1, -1);
            User user = clientCache.getCurrentUser();
            client().updateUser(user.getId().toString(), user.getEmail(), user.getUsername(), user.getName(), PASSWORD);
        }
        return clientCache;
    }

    private class GiteeStatement extends Statement {
        private final Statement next;

        private GiteeStatement(Statement next) {
            this.next = next;
        }

        @Override
        public void evaluate() throws Throwable {
            try {
                next.evaluate();
            } finally {
                GiteeRule.this.cleanup();
            }
        }
    }
}
