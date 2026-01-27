package com.gitee.jenkins.gitee.api.impl;


import com.gitee.jenkins.gitee.JacksonConfig;
import com.gitee.jenkins.gitee.api.GiteeClient;
import com.gitee.jenkins.gitee.api.GiteeClientBuilder;
import com.gitee.jenkins.gitee.api.model.PullRequest;
import com.gitee.jenkins.util.JsonUtil;
import com.gitee.jenkins.util.LoggerUtil;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.ProxyConfiguration;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder.HostnameVerificationPolicy;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.client.jaxrs.internal.proxy.ProxyBuilderImpl;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.providers.InputStreamProvider;
import org.jboss.resteasy.plugins.providers.JaxrsFormProvider;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataWriter;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.RuntimeDelegate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.Proxy.Type.HTTP;


@Restricted(NoExternalUse.class)
public class ResteasyGiteeClientBuilder extends GiteeClientBuilder {
    private static final Logger LOGGER = Logger.getLogger(ResteasyGiteeClientBuilder.class.getName());
    private static final String PRIVATE_TOKEN = "PRIVATE-TOKEN";

    @Initializer(before = InitMilestone.PLUGINS_STARTED)
    public static void setRuntimeDelegate() {
        RuntimeDelegate.setInstance(new ResteasyProviderFactoryImpl());
    }

    private final Class<? extends GiteeApiProxy> apiProxyClass;
    private final Function<PullRequest, Integer> pullRequestIdProvider;

    ResteasyGiteeClientBuilder(String id, int ordinal, Class<? extends GiteeApiProxy> apiProxyClass, Function<PullRequest, Integer> pullRequestIdProvider) {
        super(id, ordinal);
        this.apiProxyClass = apiProxyClass;
        this.pullRequestIdProvider = pullRequestIdProvider;
    }

    @NonNull
    @Override
    public final GiteeClient buildClient(String url, String apiToken, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        return buildClient(
            url,
            apiToken,
            Jenkins.get().proxy,
            ignoreCertificateErrors,
            connectionTimeout,
            readTimeout
        );
    }

    private GiteeClient buildClient(String url, String apiToken, ProxyConfiguration httpProxyConfig, boolean ignoreCertificateErrors, int connectionTimeout, int readTimeout) {
        HttpCredsResteasyClientBuilderImpl builder = new HttpCredsResteasyClientBuilderImpl();

        if (ignoreCertificateErrors) {
            builder.hostnameVerification(HostnameVerificationPolicy.ANY);
            builder.disableTrustManager();
        }

        if (httpProxyConfig != null) {
            Proxy proxy = httpProxyConfig.createProxy(getHost(url));
            if (proxy.type() == HTTP) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                builder.defaultProxy(address.getHostString().replaceFirst("^.*://", ""),
                    address.getPort(),
                    address.getHostName().startsWith("https") ? "https" : "http",
                    httpProxyConfig.getUserName(),
                    httpProxyConfig.getSecretPassword().getPlainText());
            }
        }

        ResteasyWebTarget target = builder
            .connectionPoolSize(60)
            .maxPooledPerRoute(30)
            .connectTimeout(connectionTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .register(new JacksonJsonProvider())
            .register(new JacksonConfig())
            .register(new ApiHeaderTokenFilter(apiToken))
            .register(new LoggingFilter())
            .register(new RemoveAcceptEncodingFilter())
            .register(new JaxrsFormProvider())
            .register(MultipartFormDataWriter.class)
            .register(InputStreamProvider.class)
            .build().target(url);

        // workaround for https://github.com/orgs/resteasy/discussions/4538 when using ResteasyWebTarget#proxyBuilder
        GiteeApiProxy apiProxy = new ProxyBuilderImpl<>(apiProxyClass, target)
            .classloader(apiProxyClass.getClassLoader())
            .build();

        return new ResteasyGiteeClient(url, apiProxy, pullRequestIdProvider);
    }

    private String getHost(String url) {
        try {
            return new URL(url).getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Priority(Priorities.HEADER_DECORATOR)
    private static class ApiHeaderTokenFilter implements ClientRequestFilter {
        private final String giteeApiToken;

        ApiHeaderTokenFilter(String giteeApiToken) {
            this.giteeApiToken = giteeApiToken;
        }

        public void filter(ClientRequestContext requestContext) {
            requestContext.getHeaders().putSingle(PRIVATE_TOKEN, giteeApiToken);
        }
    }

    @Priority(Priorities.USER)
    private static class LoggingFilter implements ClientRequestFilter, ClientResponseFilter {
        @Override
        public void filter(ClientRequestContext context) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Call Gitee:\nHTTP method: {0}\nURL: {1}\nRequest headers: [\n{2}\n]",
                        LoggerUtil.toArray(context.getMethod(), context.getUri(), toFilteredString(context.getHeaders())));
            }
        }

        @Override
        public void filter(ClientRequestContext request, ClientResponseContext response) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.log(Level.FINEST, "Got response from Gitee:\nURL: {0}\nStatus: {1} {2}\nResponse headers: [\n{3}\n]\nResponse body: {4}",
                        LoggerUtil.toArray(request.getUri(), response.getStatus(), response.getStatusInfo(), toString(response.getHeaders()),
                                getPrettyPrintResponseBody(response)));
            }
        }

        private String toFilteredString(MultivaluedMap<String, Object> headers) {
            return FluentIterable.from(headers.entrySet()).transform(new HeaderToFilteredString()).join(Joiner.on(",\n"));
        }

        private String toString(MultivaluedMap<String, String> headers) {
            return FluentIterable.from(headers.entrySet()).transform(new HeaderToString()).join(Joiner.on(",\n"));
        }

        private String getPrettyPrintResponseBody(ClientResponseContext responseContext) {
            String responseBody = getResponseBody(responseContext);
            if (StringUtils.isNotEmpty(responseBody) && responseContext.getMediaType().equals(MediaType.APPLICATION_JSON_TYPE)) {
                return JsonUtil.toPrettyPrint(responseBody);
            }
            return responseBody;
        }

        private String getResponseBody(ClientResponseContext context) {
            try (InputStream entityStream = context.getEntityStream()) {
                byte[] bytes = IOUtils.toByteArray(entityStream);
                context.setEntityStream(new ByteArrayInputStream(bytes));
                return new String(bytes);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failure during reading the response body", e);
                context.setEntityStream(new ByteArrayInputStream(new byte[0]));
            }
            return "";
        }

        private static class HeaderToFilteredString implements Function<Map.Entry<String, List<Object>>, String> {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, List<Object>> input) {
                if (input == null) {
                    return null;
                } else if (input.getKey().equals(PRIVATE_TOKEN)) {
                    return input.getKey() + " = [****FILTERED****]";
                } else {
                    return input.getKey() + " = [" + Joiner.on(", ").join(input.getValue()) + "]";
                }
            }
        }

        private static class HeaderToString implements Function<Map.Entry<String, List<String>>, String> {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, List<String>> input) {
                return input == null ? null : input.getKey() + " = [" + Joiner.on(", ").join(input.getValue()) + "]";
            }
        }
    }

    @Priority(Priorities.HEADER_DECORATOR)
    private static class RemoveAcceptEncodingFilter implements ClientRequestFilter {
        RemoveAcceptEncodingFilter() {}
        @Override
        public void filter(ClientRequestContext clientRequestContext) {
            clientRequestContext.getHeaders().remove("Accept-Encoding");
        }
    }

    private static class HttpCredsResteasyClientBuilderImpl extends ResteasyClientBuilderImpl {
        private CredentialsProvider proxyCredentials;


        @SuppressWarnings("UnusedReturnValue")
        HttpCredsResteasyClientBuilderImpl defaultProxy(String hostname, int port, final String scheme, String username, String password) {
            super.defaultProxy(hostname, port, scheme);
            if (username != null && password != null) {
                proxyCredentials = new BasicCredentialsProvider();
                proxyCredentials.setCredentials(new AuthScope(hostname, port), new UsernamePasswordCredentials(username, password));
            }
            return this;
        }
    }
}
