package com.invitation.backend.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "app.uptime.keep-alive.enabled", havingValue = "true", matchIfMissing = true)
public class UptimeKeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(UptimeKeepAliveScheduler.class);

    private final String selfUrl;
    private final String clientUrl;
    private final HttpClient httpClient;

    public UptimeKeepAliveScheduler(
            @Value("${app.server.public-url:http://localhost:8080}") String selfUrl,
            @Value("${app.client.url:http://localhost:5173}") String clientUrl) {
        this.selfUrl = selfUrl.endsWith("/") ? selfUrl.substring(0, selfUrl.length() - 1) : selfUrl;
        this.clientUrl = clientUrl.endsWith("/") ? clientUrl.substring(0, clientUrl.length() - 1) : clientUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Executes every 10 minutes (600,000 ms) to keep Render Free Tier instances warm 24/7.
     * Initial delay of 1 minute after server boot to allow full initialization.
     */
    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    public void pingKeepAlive() {
        log.info("⚡ [Keep-Alive] Starting periodic heartbeat ping for Render 24/7 high availability...");

        // 1. Ping Backend Self Health Check
        if (selfUrl.startsWith("http://") || selfUrl.startsWith("https://")) {
            pingEndpoint(selfUrl + "/api/health", "Backend API Service");
        }

        // 2. Ping Frontend Client (if public domain)
        if (clientUrl.startsWith("https://")) {
            pingEndpoint(clientUrl, "Frontend Client Web");
        }
    }

    private void pingEndpoint(String url, String serviceName) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "InvitationApp-KeepAlive-Heartbeat/1.0")
                    .GET()
                    .build();

            httpClient
                    .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .thenAccept(response -> {
                        if (response.statusCode() >= 200 && response.statusCode() < 400) {
                            log.info("💚 [Keep-Alive] {} responded with status {}", serviceName, response.statusCode());
                        } else {
                            log.warn("⚠️ [Keep-Alive] {} returned status {}", serviceName, response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        log.warn("⏳ [Keep-Alive] {} heartbeat warning: {}", serviceName, ex.getMessage());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("⏳ [Keep-Alive] Failed to schedule ping for {}: {}", serviceName, e.getMessage());
        }
    }
}
