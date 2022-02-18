package io.github.renestel.core.web.rest;

import lombok.SneakyThrows;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustAllStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.web.server.Ssl;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.HostnameVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class HttpClientConfigurer {

    @SneakyThrows
    public ClientHttpRequestFactory createRequestFactory(BaseRestClientProperties properties,
                                                         HttpRequestInterceptor requestInterceptor,
                                                         HttpResponseInterceptor responseInterceptor) {
        LayeredConnectionSocketFactory socketFactory;
        var ssl = properties.getSsl();
        var protocol = ssl.getProtocol();
        var trustStore = ssl.getTrustStore();
        var trustStorePassword = ssl.getTrustStorePassword();
        var keyStorePassword = ssl.getKeyStorePassword();

        if (ssl.isEnabled()) {
            HostnameVerifier hostnameVerifier;
            if (properties.getCheckCertificateHostname()) {
                hostnameVerifier = new DefaultHostnameVerifier();
            } else {
                hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            }
            if (ssl.getClientAuth() == Ssl.ClientAuth.NONE) {
                socketFactory = new SSLConnectionSocketFactory(
                        SSLContexts
                                .custom()
                                .setProtocol(protocol)
                                .loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray())
                                .build(),
                        hostnameVerifier);
            } else {
                var keystore = KeyStore.getInstance(ssl.getKeyStoreType());
                try (var store = new FileInputStream(ssl.getKeyStore())) {
                    keystore.load(store, keyStorePassword.toCharArray());
                }
                socketFactory = new SSLConnectionSocketFactory(
                        SSLContexts
                                .custom()
                                .setProtocol(protocol)
                                .loadKeyMaterial(keystore, keyStorePassword.toCharArray())
                                .loadTrustMaterial(new File(trustStore), trustStorePassword.toCharArray())
                                .build(),
                        hostnameVerifier);
            }
        } else {
            socketFactory = new SSLConnectionSocketFactory(
                    SSLContexts
                            .custom()
                            .setProtocol(protocol)
                            .loadTrustMaterial(TrustAllStrategy.INSTANCE)
                            .build(),
                    NoopHostnameVerifier.INSTANCE);
        }
        var connectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder
                        .<ConnectionSocketFactory>create()
                        .register("https", socketFactory)
                        .register("http", new PlainConnectionSocketFactory())
                        .build());
        if (properties.getMaxTotalConnections() != null) {
            connectionManager.setMaxTotal(properties.getMaxTotalConnections());
        }
        if (properties.getMaxDefaultPerRouteConnections() != null) {
            connectionManager.setDefaultMaxPerRoute(properties.getMaxDefaultPerRouteConnections());
        }
        connectionManager.setValidateAfterInactivity(10 * 1000);
        var httpClient = HttpClients
                .custom()
                .setConnectionManager(connectionManager)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .addInterceptorFirst(requestInterceptor)
                .addInterceptorFirst(responseInterceptor)
                .disableCookieManagement()
                .build();
        return new HttpComponentsClientHttpRequestFactory(httpClient);
    }
}
