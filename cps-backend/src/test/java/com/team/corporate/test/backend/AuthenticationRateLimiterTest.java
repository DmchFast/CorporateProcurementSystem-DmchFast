package com.team.corporate.test.backend;

import com.team.corporate.backend.config.RateLimitProperties;
import com.team.corporate.backend.exceptions.RateLimitExceededException;
import com.team.corporate.backend.security.AuthenticationRateLimiter;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationRateLimiterTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void concurrentAttemptsCannotExceedAccountLimit() throws Exception {
        var limiter = limiter(5, 1000, 1000);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var attempts = new ArrayList<Future<Boolean>>();
            for (int index = 0; index < 100; index++) {
                attempts.add(executor.submit(() -> {
                    start.await();
                    try {
                        limiter.acquireAccount("login", "user", "127.0.0.1");
                        return true;
                    } catch (RateLimitExceededException exception) {
                        return false;
                    }
                }));
            }
            start.countDown();
            int accepted = 0;
            for (var attempt : attempts) if (attempt.get(5, TimeUnit.SECONDS)) accepted++;
            assertEquals(5, accepted);
        }
    }

    @Test
    void fillingCacheDoesNotEvictAnActiveAccountLockout() {
        var limiter = limiter(1, 1000, 10);
        for (int index = 0; index < 5; index++) {
            limiter.acquireAccount("login", "user-" + index, "ip-" + index);
        }
        assertThrows(RateLimitExceededException.class,
                () -> limiter.acquireAccount("login", "new-user", "new-ip"));
        assertThrows(RateLimitExceededException.class,
                () -> limiter.acquireAccount("login", "user-0", "ip-0"));
    }

    @Test
    void successfulLoginDoesNotResetIpLimit() {
        var limiter = limiter(1, 2, 100);
        limiter.acquireAccount("login", "user", "ip");
        limiter.accountSucceeded("login", "user");
        limiter.acquireAccount("login", "user", "ip");
        limiter.accountSucceeded("login", "user");
        assertThrows(RateLimitExceededException.class, () -> limiter.acquireAccount("login", "user", "ip"));
    }

    private AuthenticationRateLimiter limiter(int account, int ip, int capacity) {
        return new AuthenticationRateLimiter(CLOCK, new RateLimitProperties(
                account, Duration.ofMinutes(15), ip, Duration.ofMinutes(1), 60, capacity));
    }
}
