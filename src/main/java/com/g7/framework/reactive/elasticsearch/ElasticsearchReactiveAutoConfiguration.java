package com.g7.framework.reactive.elasticsearch;

import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRestClientProperties;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration;
import org.springframework.data.elasticsearch.config.EnableReactiveElasticsearchAuditing;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author dreamyao
 * @title
 * @date 2021/11/29 2:42 下午
 * @since 1.0.0
 */
@AutoConfiguration
@EnableReactiveElasticsearchAuditing
@ConditionalOnClass({ReactiveRestClients.class, WebClient.class, HttpClient.class})
@EnableConfigurationProperties({ElasticsearchProperties.class, ReactiveElasticsearchRestClientProperties.class})
@EnableReactiveElasticsearchRepositories(basePackages = "com.**.elasticsearch.**")
public class ElasticsearchReactiveAutoConfiguration extends AbstractReactiveElasticsearchConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchReactiveAutoConfiguration.class);
    private final ConsolidatedProperties properties;

    public ElasticsearchReactiveAutoConfiguration(ElasticsearchProperties properties,
                                                  ReactiveElasticsearchRestClientProperties restClientProperties) {
        this.properties = new ConsolidatedProperties(properties, restClientProperties);
    }

    @Bean
    @Override
    public ReactiveElasticsearchClient reactiveElasticsearchClient() {
        return ReactiveRestClients.create(clientConfiguration());
    }

    private ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder = ClientConfiguration.builder()
                .connectedTo(this.properties.getEndpoints().toArray(new String[0]));
        PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
        map.from(this.properties.isUseSsl()).whenTrue().toCall(() -> {
            try {
                final SSLContext ssl = new SSLContextBuilder().loadTrustMaterial(null,
                        (unused1, unused2) -> true).build();
                builder.usingSsl(ssl);
            } catch (Exception e) {
                logger.error("reactive elasticsearch create ssl context failed", e);
            }
        });
        map.from(this.properties.getCredentials())
                .to((credentials) -> builder.withBasicAuth(credentials.getUsername(),
                        credentials.getPassword()));
        map.from(this.properties.getConnectionTimeout()).to(builder::withConnectTimeout);
        map.from(this.properties.getSocketTimeout()).to(builder::withSocketTimeout);
        map.from(this.properties.getPathPrefix()).to(builder::withPathPrefix);
        configureExchangeStrategies(map, builder);
        return builder.build();
    }

    private void configureExchangeStrategies(PropertyMapper map,
                                             ClientConfiguration.TerminalClientConfigurationBuilder builder) {
        map.from(this.properties.getMaxInMemorySize()).asInt(DataSize::toBytes).to(
                (maxInMemorySize) -> builder.withClientConfigurer(
                        ReactiveRestClients.WebClientConfigurationCallback.from(
                                (webClient) -> {
                                    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                                            .codecs((configurer) -> configurer.defaultCodecs()
                                                    .maxInMemorySize(maxInMemorySize))
                                            .build();
                                    return webClient.mutate().exchangeStrategies(exchangeStrategies).build();
                                })));
    }

    private static final class ConsolidatedProperties {

        private final ElasticsearchProperties properties;
        private final ReactiveElasticsearchRestClientProperties restClientProperties;
        private final List<URI> uris;

        private ConsolidatedProperties(ElasticsearchProperties properties,
                                       ReactiveElasticsearchRestClientProperties restClientProperties) {
            this.properties = properties;
            this.restClientProperties = restClientProperties;
            this.uris = properties.getUris().stream().map((s) -> s.startsWith("http") ? s : "http://" + s)
                    .map(URI::create).collect(Collectors.toList());
        }

        private List<String> getEndpoints() {
            return this.uris.stream().map(this::getEndpoint).collect(Collectors.toList());
        }

        private String getEndpoint(URI uri) {
            return uri.getHost() + ":" + uri.getPort();
        }

        private ConsolidatedProperties.Credentials getCredentials() {
            ConsolidatedProperties.Credentials propertyCredentials = ConsolidatedProperties.Credentials
                    .from(this.properties);
            ConsolidatedProperties.Credentials uriCredentials = ConsolidatedProperties.Credentials
                    .from(this.properties.getUris());
            if (uriCredentials == null) {
                return propertyCredentials;
            }
            Assert.isTrue(propertyCredentials == null || uriCredentials.equals(propertyCredentials),
                    "Credentials from URI user info do not match those from spring.elasticsearch.username and "
                            + "spring.elasticsearch.password");
            return uriCredentials;
        }

        private Duration getConnectionTimeout() {
            return this.properties.getConnectionTimeout();
        }

        private Duration getSocketTimeout() {
            return this.properties.getSocketTimeout();
        }

        private boolean isUseSsl() {
            Set<String> schemes = this.uris.stream().map(URI::getScheme).collect(Collectors.toSet());
            Assert.isTrue(schemes.size() == 1, "Configured Elasticsearch URIs have varying schemes");
            return schemes.iterator().next().equals("https");
        }

        private DataSize getMaxInMemorySize() {
            return this.restClientProperties.getMaxInMemorySize();
        }

        private String getPathPrefix() {
            return this.properties.getPathPrefix();
        }

        private static final class Credentials {

            private final String username;
            private final String password;

            private Credentials(String username, String password) {
                this.username = username;
                this.password = password;
            }

            private String getUsername() {
                return this.username;
            }

            private String getPassword() {
                return this.password;
            }

            private static ConsolidatedProperties.Credentials from(List<String> uris) {
                Set<String> userInfos = uris.stream().map(URI::create).map(URI::getUserInfo)
                        .collect(Collectors.toSet());
                Assert.isTrue(userInfos.size() == 1, "Configured Elasticsearch " +
                        "URIs have varying user infos");
                String userInfo = userInfos.iterator().next();
                if (userInfo == null) {
                    return null;
                }
                String[] parts = userInfo.split(":");
                String username = parts[0];
                String password = (parts.length != 2) ? "" : parts[1];
                return new ConsolidatedProperties.Credentials(username, password);
            }

            private static ConsolidatedProperties.Credentials from(ElasticsearchProperties properties) {
                return getCredentials(properties.getUsername(), properties.getPassword());
            }

            private static ConsolidatedProperties.Credentials getCredentials(String username, String password) {
                if (username == null && password == null) {
                    return null;
                }
                return new ConsolidatedProperties.Credentials(username, password);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null || getClass() != obj.getClass()) {
                    return false;
                }
                ConsolidatedProperties.Credentials other = (ConsolidatedProperties.Credentials) obj;
                return ObjectUtils.nullSafeEquals(this.username, other.username)
                        && ObjectUtils.nullSafeEquals(this.password, other.password);
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ObjectUtils.nullSafeHashCode(this.username);
                result = prime * result + ObjectUtils.nullSafeHashCode(this.password);
                return result;
            }
        }
    }
}
