package com.ewalleting.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.security.KeyStore;

/**
 * Builds the client-side TLS identity api-gateway presents when it calls
 * core-facade-rest — a separate keystore/cert from the server certificate it
 * presents to inbound callers (SecurityConfig.java territory), because "being a
 * server" and "being a client" are different roles needing different identities.
 * Spring Cloud Gateway MVC automatically picks up any ClientHttpRequestFactory
 * bean for its outbound proxied calls (GatewayServerMvcAutoConfiguration).
 */
@Configuration
public class MtlsClientConfig {

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory(
            @Value("${mtls.client.key-store}") String keyStorePath,
            @Value("${mtls.client.key-store-password}") String keyStorePassword,
            @Value("${mtls.client.trust-store}") String trustStorePath,
            @Value("${mtls.client.trust-store-password}") String trustStorePassword) throws Exception {

        // This is api-gateway's own identity — the cert it PRESENTS to prove it's really api-gateway.
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(keyStorePath)) {
            keyStore.load(in, keyStorePassword.toCharArray());
        }
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());

        // This is who api-gateway TRUSTS in return — just our mini CA, so it accepts
        // core-facade-rest's server certificate (signed by that same CA) and nothing else.
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(trustStorePath)) {
            trustStore.load(in, trustStorePassword.toCharArray());
        }
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

        HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
        return new JdkClientHttpRequestFactory(httpClient);
    }
}
