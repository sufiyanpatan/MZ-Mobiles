package mz.mobile.bussiness.security;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowRateLimiter {

    private final int maxRequests;
    private final long windowMs;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> hits = new ConcurrentHashMap<>();

    public SlidingWindowRateLimiter(int maxRequests, long windowMs) {
        this.maxRequests = maxRequests;
        this.windowMs = windowMs;
    }

    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        boolean[] allowed = {false};
        hits.compute(key, (k, q) -> {                 // atomic per key
            if (q == null) q = new ArrayDeque<>();

            prune(q, now);
            if (q.size() < maxRequests) {
                q.addLast(now); allowed[0] = true;
            }
            return q;
        });

        return allowed[0];
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        for (String key : hits.keySet()) {
            hits.computeIfPresent(key, (k, q) -> { prune(q, now); return q.isEmpty() ? null : q; });
        }
    }

    private void prune(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
    }
}
