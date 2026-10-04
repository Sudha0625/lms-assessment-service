package com.learninghub.assessment.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import java.util.*;

/** Calls enrollment-service GET /api/enrollment/my-classes. Fails soft: returns null when unreachable. */
@Component
public class EnrollmentClient {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentClient.class);
    private final RestClient rest;

    public EnrollmentClient(@Value("${services.enrollment-url:http://localhost:8104}") String baseUrl) {
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(1000); f.setReadTimeout(2000);
        this.rest = RestClient.builder().baseUrl(baseUrl).requestFactory(f).build();
    }

    @SuppressWarnings("unchecked")
    public Set<String> enrolledSubjects(String username) {
        try {
            Map<String, Object> body = rest.get().uri("/api/enrollment/my-classes")
                    .header("X-Username", username).header("X-Role", "student")
                    .retrieve().body(Map.class);
            Set<String> names = new HashSet<>();
            for (Map<String, Object> c : (List<Map<String, Object>>) body.get("classes")) names.add((String) c.get("name"));
            return names;
        } catch (Exception e) {
            log.warn("enrollment-service unavailable, not filtering by enrollment: {}", e.getMessage());
            return null;
        }
    }
}
