package com.supportdesk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Keeps the free Render web service from spinning down after 15 minutes without traffic.
 * It calls the service's own public /api/health URL through Render's edge, so Render counts it
 * as inbound traffic. It only runs on Render, where RENDER_EXTERNAL_URL is set automatically.
 * It cannot wake a service that is already asleep, so an outside pinger is still a good backup.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty("RENDER_EXTERNAL_URL")
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final URI target;

    public KeepAliveScheduler(@Value("${RENDER_EXTERNAL_URL}") String baseUrl) {
        this.target = URI.create(baseUrl.replaceAll("/+$", "") + "/api/health");
    }

    @Scheduled(
            initialDelayString = "${app.keepalive.initial-delay-ms:60000}",
            fixedDelayString = "${app.keepalive.interval-ms:600000}")
    public void ping() {
        try {
            HttpRequest request = HttpRequest.newBuilder(target)
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Keep-alive ping to {} returned {}", target, response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }
}
