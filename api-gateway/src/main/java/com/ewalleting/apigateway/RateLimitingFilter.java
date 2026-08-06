package com.ewalleting.apigateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window rate limiter, keyed per client IP: N requests per window, 429 past that.
 *
 * Deliberately simple and in-memory rather than Redis-backed — correct for a single
 * gateway instance, but it's worth being explicit that this specific design breaks the
 * moment the gateway is horizontally scaled (Module 6): each replica would count its
 * own window independently, so the real limit becomes (N * replica count) instead of N.
 * A production gateway needs a shared store (Redis INCR + TTL) for this to hold once
 * there's more than one instance behind the load balancer.
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final ConcurrentHashMap<String, AtomicInteger> windowCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${gateway.rate-limit.requests-per-window:20}") int maxRequestsPerWindow,
            @Value("${gateway.rate-limit.window-seconds:10}") long windowSeconds) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowMillis = windowSeconds * 1000;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr() + ":" + (System.currentTimeMillis() / windowMillis);
        int countInWindow = windowCounts.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();

        if (countInWindow > maxRequestsPerWindow) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
