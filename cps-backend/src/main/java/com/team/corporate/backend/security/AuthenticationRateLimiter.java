package com.team.corporate.backend.security;

import com.team.corporate.backend.config.RateLimitProperties;
import com.team.corporate.backend.exceptions.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class AuthenticationRateLimiter {
    private final Clock clock;
    private final RateLimitProperties properties;
    private final Map<Key, Window> windows = new HashMap<>();

    public AuthenticationRateLimiter(Clock clock, RateLimitProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public synchronized void acquireAccount(String operation, String username, String ip) {
        Instant now = clock.instant();
        removeExpired(now);
        consume(new Key(operation + ":ip", ip), properties.ipAttempts(), properties.ipWindow(), now);
        consume(new Key(operation + ":account", username), properties.accountAttempts(), properties.accountWindow(), now);
    }

    public synchronized void accountSucceeded(String operation, String username) {
        windows.remove(new Key(operation + ":account", username));
    }

    public synchronized void acquireTokenRequest(String operation, String ip) {
        Instant now = clock.instant();
        removeExpired(now);
        consume(new Key(operation + ":ip", ip), properties.refreshAttempts(), properties.ipWindow(), now);
    }

    private void consume(Key key, int maximum, Duration duration, Instant now) {
        Window window = windows.get(key);
        if (window == null) {
            if (windows.size() >= properties.maxTrackedKeys()) {
                Instant next = windows.values().stream().map(value -> value.resetsAt).min(Instant::compareTo).orElse(now);
                throw exceeded(now, next);
            }
            window = new Window(now.plus(duration));
            windows.put(key, window);
        }
        if (window.attempts >= maximum) throw exceeded(now, window.resetsAt);
        window.attempts++;
    }

    private void removeExpired(Instant now) {
        windows.values().removeIf(window -> !window.resetsAt.isAfter(now));
    }

    private static RateLimitExceededException exceeded(Instant now, Instant resetsAt) {
        long seconds = Math.max(1, (Duration.between(now, resetsAt).toMillis() + 999) / 1000);
        return new RateLimitExceededException(seconds);
    }

    private record Key(String category, String value) {
    }

    private static class Window {
        private final Instant resetsAt;
        private int attempts;

        private Window(Instant resetsAt) { this.resetsAt = resetsAt; }
    }
}
