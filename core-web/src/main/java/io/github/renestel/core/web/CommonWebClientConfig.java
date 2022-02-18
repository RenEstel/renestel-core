package io.github.renestel.core.web;

import io.github.renestel.core.web.config.HttpConnectionProps;
import io.github.renestel.core.web.config.SslConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIMatcher;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static io.netty.handler.ssl.SslProvider.JDK;
import static java.nio.file.StandardOpenOption.READ;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunction.ofRequestProcessor;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunction.ofResponseProcessor;
import static org.springframework.web.reactive.function.client.WebClient.builder;
import static reactor.netty.tcp.SslProvider.DefaultConfigurationType.NONE;

@Slf4j
@RequiredArgsConstructor
public class CommonWebClientConfig {

    public static final String SUN_X_509 = "SunX509";
    private HttpConnectionProps properties;

    public CommonWebClientConfig(HttpConnectionProps properties) {
        this.properties = properties;
    }

    public WebClient webClient() {
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(http());
        WebClient.Builder builder = commonConfig(connector);
        configureDebug(builder);
        return builder.build();
    }

    private HttpClient http() {
        var httpClient = HttpClient.create()
                .option(CONNECT_TIMEOUT_MILLIS, properties.getConnectionTimeout() * 1000)
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(properties.getReadTimeout() * 1000L, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(properties.getWriteTimeout() * 1000L, TimeUnit.MILLISECONDS)));
        if (properties.getSsl().isEnabled()) return httpClient.secure(this::configSsl);
        else if (properties.getSsl().isSelfSigned()) {
            SslContext sslContext;
            try {
                sslContext = SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            } catch (SSLException e) {
                log.error("http config failed", e);
                throw new RuntimeException(e);
            }
            SslContext finalSslContext = sslContext;
            httpClient = httpClient.secure(t -> t.sslContext(finalSslContext));
        }
        return httpClient;
    }

    private void configSsl(SslProvider.SslContextSpec spec) {
        SslConfig ssl = properties.getSsl();
        SslProvider.Builder builder = spec.sslContext(getSslContextBuilder(ssl))
                .defaultConfiguration(NONE)
                .handshakeTimeout(Duration.ofSeconds(2));
        if (!ssl.isSniCheck()) {
            builder.handlerConfigurator(h -> {
                SSLEngine engine = h.engine();
                SSLParameters params = new SSLParameters();
                params.setSNIMatchers(Collections.singletonList(new SNIMatcher(0) {
                    @Override
                    public boolean matches(SNIServerName serverName) {
                        return true;
                    }
                }));
                engine.setSSLParameters(params);
            });
        }
    }

    private SslContextBuilder getSslContextBuilder(SslConfig ssl) {
        try {
            char[] ksPass = ssl.getKeystorePassword().toCharArray();
            char[] tsPass = ssl.getTrustStorePassword().toCharArray();
            KeyStore keyStore = readKeystore(ssl.getKeystoreLocation(), ssl.getKeystoreType(), ksPass);
            KeyStore truststore = readKeystore(ssl.getTruststoreLocation(), ssl.getTrustStoreType(), tsPass);

            KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(SUN_X_509);
            TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(SUN_X_509);
            keyFactory.init(keyStore, ksPass);
            trustFactory.init(truststore);
            return SslContextBuilder
                    .forClient()
                    .sslProvider(JDK)
                    .keyManager(keyFactory)
                    .trustManager(trustFactory);
        } catch (Exception e) {
            log.error("Couldn't Initialize Keystores");
            throw new IllegalStateException(e);
        }
    }

    private KeyStore readKeystore(String path, String type, char[] password) {
        try {
            Path absPath = Paths.get(path).toAbsolutePath();
            KeyStore ks = KeyStore.getInstance(type);
            ks.load(Files.newInputStream(absPath, READ), password);
            return ks;
        } catch (Exception e) {
            log.error("Could not read the Keystore {}: {}", path, e.getMessage(), e);
        }
        return null;
    }

    private WebClient.Builder commonConfig(ReactorClientHttpConnector connector) {
        return builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .clientConnector(connector);
    }

    private void configureDebug(WebClient.Builder webClientBuilder) {
        if (properties.isDebug()) {
            webClientBuilder.filters(ef -> {
                        ef.add(ofRequestProcessor(r -> {
                            r.headers().forEach((n, v) -> log.info("Request Header {}: {}", n, v));
                            return Mono.just(r);
                        }));
                        ef.add(ofResponseProcessor(resp -> {
                                    resp.headers().asHttpHeaders()
                                            .forEach((n, v) -> log.info("Response Header {}: {}", n, v));
                                    return Mono.just(resp);
                                }
                        ));
                    }
            );
            log.info("DEBUG configured");
        }
    }
}
