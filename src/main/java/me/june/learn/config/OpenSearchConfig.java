package me.june.ingest.config;


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.server}")
    private String server;

    @Value("${opensearch.port}")
    private int port;

    @Value("${opensearch.hostname}")
    private String hostname;

    @Value("${opensearch.ssl.ignore}")
    private boolean sslIgnore;

    @Value("${opensearch.usename}")
    private String username;

    @Value("${opensearch.password}")
    private String password;




    @Bean
    public OpenSearchClient openSearchClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        OpenSearchTransport transport = new RestClientTransport(defaultRestClient(), new JacksonJsonpMapper());
        return new OpenSearchClient(transport);
    }

    private SSLContext defaultSSLContext() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        return sslIgnore?
                SSLContextBuilder.create()
                        .loadTrustMaterial((x509Certificates, s) -> true).build() : SSLContext.getDefault();
    }

    private RestClient defaultRestClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        HttpHost host = new HttpHost(server,port,hostname);
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        SSLContext sslContext = defaultSSLContext();
        credentialsProvider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials(username, password));
        return RestClient.builder(host)
                .setHttpClientConfigCallback(httpAsyncClientBuilder -> {
                    httpAsyncClientBuilder
                            .setDefaultCredentialsProvider(credentialsProvider)
                            .setSSLContext(sslContext);
                    return !sslIgnore ? httpAsyncClientBuilder : httpAsyncClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                })
                .build();
    }
}
