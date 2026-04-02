package com.keyfeed.keyfeedmonolithic.global.client.toss;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class TossPaymentsConfig {

    private final TossPaymentsProperties properties;

    @Bean(name = "tossRestTemplate")
    public RestTemplate tossRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        String credentials = properties.getSecretKey() + ":";
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.setInterceptors(List.of((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
            request.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return execution.execute(request, body);
        }));

        return restTemplate;
    }
}
