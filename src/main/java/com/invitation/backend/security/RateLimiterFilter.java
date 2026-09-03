package com.invitation.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int MAX_BUCKETS_IN_MEMORY = 20000;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucket(int capacity, Duration duration) {
        Refill refill = Refill.greedy(capacity, duration);
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Evict if cache exceeds max to prevent OutOfMemory DoS
        if (buckets.size() > MAX_BUCKETS_IN_MEMORY) {
            buckets.clear();
        }

        String ip = getClientIP(request);

        // 1. Auth & Credential endpoints: 15 req/min (Anti-Bruteforce)
        if (path.startsWith("/api/v1/auth/login") ||
            path.startsWith("/api/v1/auth/verify-2fa") ||
            path.startsWith("/api/v1/auth/register") ||
            path.startsWith("/api/v1/auth/change-password")) {

            Bucket bucket = buckets.computeIfAbsent("AUTH:" + ip, k -> createBucket(15, Duration.ofMinutes(1)));
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Quá nhiều yêu cầu đăng nhập/xác thực. Vui lòng thử lại sau 1 phút.");
                return;
            }
        }
        // 2. File Upload endpoint: 20 req/min (Anti-Flooding / Anti-Disk-Exhaustion)
        else if (path.startsWith("/api/v1/upload")) {
            Bucket bucket = buckets.computeIfAbsent("UPLOAD:" + ip, k -> createBucket(20, Duration.ofMinutes(1)));
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Tải tệp quá nhanh. Vui lòng chờ một chút trước khi tiếp tục.");
                return;
            }
        }
        // 3. Webhook endpoint: 60 req/min (Protect Webhook processor from spam flooding)
        else if (path.startsWith("/api/v1/payment/webhook")) {
            Bucket bucket = buckets.computeIfAbsent("WEBHOOK:" + ip, k -> createBucket(60, Duration.ofMinutes(1)));
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Too many webhook requests.");
                return;
            }
        }
        // 4. Public Card Wishes endpoint: 10 req/min (Anti-Spam comments on wedding cards)
        else if (path.contains("/wishes") && "POST".equalsIgnoreCase(method)) {
            Bucket bucket = buckets.computeIfAbsent("WISH:" + ip, k -> createBucket(10, Duration.ofMinutes(1)));
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Bạn gửi lời chúc quá nhanh. Vui lòng chờ trong giây lát.");
                return;
            }
        }
        // 5. General API routes: 120 req/min (Anti-DDoS / General Traffic Spikes)
        else if (path.startsWith("/api/v1/")) {
            Bucket bucket = buckets.computeIfAbsent("GEN:" + ip, k -> createBucket(120, Duration.ofMinutes(1)));
            if (!bucket.tryConsume(1)) {
                sendRateLimitResponse(response, "Hệ thống đang bận. Vui lòng thử lại sau ít phút.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(String.format("{\"success\":false,\"message\":\"%s\"}", message));
    }
}
