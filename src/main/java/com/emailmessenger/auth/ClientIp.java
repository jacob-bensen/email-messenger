package com.emailmessenger.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Picks the client IP off the current request — prefers the leftmost
 * value in {@code X-Forwarded-For} so a reverse-proxy deploy (Caddy,
 * Cloudflare, the platform load balancer) records the original caller
 * rather than 127.0.0.1. Falls back to {@code remoteAddr} for direct
 * connections and tests.
 */
final class ClientIp {

    private ClientIp() {}

    static String from(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = (comma >= 0 ? forwarded.substring(0, comma) : forwarded).trim();
            if (!first.isEmpty()) {
                return clamp(first);
            }
        }
        String remote = request.getRemoteAddr();
        return clamp(remote);
    }

    static String fromCurrentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return from(sra.getRequest());
        }
        return null;
    }

    private static String clamp(String ip) {
        if (ip == null) {
            return null;
        }
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
