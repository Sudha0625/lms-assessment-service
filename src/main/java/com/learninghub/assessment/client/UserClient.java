package com.learninghub.assessment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.Map;

/** Calls user-service GET /api/users/teachers/{username} for the display name. Returns null when unreachable. */
@Component
public class UserClient {
    private static final Logger log = LoggerFactory.getLogger(UserClient.class);
    private final RestClient rest;

    public UserClient(@Value("${services.user-url:http://localhost:8101}") String baseUrl) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(1000); f.setReadTimeout(2000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(f).build();
    }

    @SuppressWarnings("unchecked")
    public String displayName(String username) {
        try {
            Map<String, Object> t = rest.get().uri("/api/users/teachers/{u}", username).retrieve().body(Map.class);
            return (String) t.get("displayName");
        } catch (Exception e) {
            log.warn("user-service unavailable, falling back to stored teacher name: {}", e.getMessage());
            return null;
        }
    }
}
