package com.gitee.jenkins.connection;


import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.GiteeClientBuilder;
import com.gitee.jenkins.gitee.api.impl.GiteeV5ClientBuilder;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static com.gitee.jenkins.gitee.api.GiteeClientBuilder.getGiteeClientBuilderById;


/**
 * @author Robin Müller
 */
public class GiteeConnection {
    private final String name;
    private final String url;
    private transient String apiToken;
    // TODO make final when migration code gets removed
    private String apiTokenId;
    private GiteeClientBuilder clientBuilder;
    private final boolean ignoreCertificateErrors;
    private final Integer connectionTimeout;
    private final Integer readTimeout;
    private transient GiteeClient apiCache;

    public GiteeConnection(String name, String url, String apiTokenId, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this(
            name,
            url,
            apiTokenId,
            new GiteeV5ClientBuilder(),
            ignoreCertificateErrors,
            connectionTimeout,
            readTimeout
        );
    }

    @DataBoundConstructor
    public GiteeConnection(String name, String url, String apiTokenId, String clientBuilderId, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this(
            name,
            url,
            apiTokenId,
            getGiteeClientBuilderById(clientBuilderId),
            ignoreCertificateErrors,
            connectionTimeout,
            readTimeout
        );
    }

    @Restricted(NoExternalUse.class)
    public GiteeConnection(String name, String url, String apiTokenId, GiteeClientBuilder clientBuilder, boolean ignoreCertificateErrors, Integer connectionTimeout, Integer readTimeout) {
        this.name = name;
        this.url = url;
        this.apiTokenId = apiTokenId;
        this.clientBuilder = clientBuilder;
        this.ignoreCertificateErrors = ignoreCertificateErrors;
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getApiTokenId() {
        return apiTokenId;
    }

    public String getClientBuilderId() {
        return clientBuilder.id();
    }

    public boolean isIgnoreCertificateErrors() {
        return ignoreCertificateErrors;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public GiteeClient getClient() {
        if (apiCache == null) {
            apiCache = clientBuilder.buildClient(url, getApiToken(apiTokenId), ignoreCertificateErrors, connectionTimeout, readTimeout);
        }

        return apiCache;
    }

    private String getApiToken(String apiTokenId) {
        StandardCredentials credentials = CredentialsMatchers.firstOrNull(
            lookupCredentials(StandardCredentials.class, (Item) null, ACL.SYSTEM, new ArrayList<DomainRequirement>()),
            CredentialsMatchers.withId(apiTokenId));
        if (credentials != null) {
            if (credentials instanceof GiteeApiToken) {
                return ((GiteeApiToken) credentials).getApiToken().getPlainText();
            }
            if (credentials instanceof StringCredentials) {
                return ((StringCredentials) credentials).getSecret().getPlainText();
            }
        }
        throw new IllegalStateException("No credentials found for credentialsId: " + apiTokenId);
    }


    protected GiteeConnection readResolve() {
        if (connectionTimeout == null || readTimeout == null) {
            return new GiteeConnection(name, url, apiTokenId, new GiteeV5ClientBuilder(), ignoreCertificateErrors, 10, 10);
        }
        if (clientBuilder == null) {
            return new GiteeConnection(name, url, apiTokenId, new GiteeV5ClientBuilder(), ignoreCertificateErrors, connectionTimeout, readTimeout);
        }

        return this;
    }

    @Initializer(after = InitMilestone.PLUGINS_STARTED)
    public static void migrate() throws IOException {
        GiteeConnectionConfig descriptor = (GiteeConnectionConfig) Jenkins.getInstance().getDescriptor(GiteeConnectionConfig.class);
        for (GiteeConnection connection : descriptor.getConnections()) {
            if (connection.apiTokenId == null && connection.apiToken != null) {
                for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
                    if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                        List<Domain> domains = credentialsStore.getDomains();
                        connection.apiTokenId = UUID.randomUUID().toString();
                        credentialsStore.addCredentials(domains.get(0),
                            new GiteeApiTokenImpl(CredentialsScope.SYSTEM, connection.apiTokenId, "Gitee API Token", Secret.fromString(connection.apiToken)));
                    }
                }
            }
        }
        descriptor.save();
    }
}
