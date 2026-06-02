package com.gov.ac.feature.attachment.verification;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

/**
 * In-memory sliding-window rate limiter for the permitAll public verify endpoint.
 *
 * <p>Keyed by {@code (ip, token_hash)} so a leaked token can still be probed from one IP at a
 * bounded rate but a determined attacker who switches IPs sees no relief on the token itself
 * (only IP relief). Production deployments are expected to layer an edge-level rate limit at the
 * reverse proxy on top of this in-app guard.
 *
 * <p>Memory is bounded by eviction of keys that have not been touched in {@code 5 * window}.
 */
@Component
public class PublicVerifyRateLimiter {

  private final AttachmentVerificationProperties properties;
  private final Map<String, Deque<Instant>> hits = new ConcurrentHashMap<>();
  private final Map<String, Instant> lastTouch = new ConcurrentHashMap<>();

  private static final Duration WINDOW = Duration.ofMinutes(1);

  public PublicVerifyRateLimiter(AttachmentVerificationProperties properties) {
    this.properties = properties;
  }

  /** Returns true iff the caller is over the configured per-minute budget for this key. */
  public boolean tryAcquireBlocked(String ip, String tokenHash) {
    String key = (ip == null ? "-" : ip) + "|" + (tokenHash == null ? "-" : tokenHash);
    int limit = properties.rateLimitPerMinute();
    Instant now = Instant.now();
    Instant cutoff = now.minus(WINDOW);
    Deque<Instant> bucket = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
    synchronized (bucket) {
      Iterator<Instant> it = bucket.iterator();
      while (it.hasNext()) {
        Instant ts = it.next();
        if (ts.isBefore(cutoff)) {
          it.remove();
        } else {
          break;
        }
      }
      if (bucket.size() >= limit) {
        lastTouch.put(key, now);
        return true;
      }
      bucket.addLast(now);
      lastTouch.put(key, now);
    }
    evictStaleKeys(now);
    return false;
  }

  private void evictStaleKeys(Instant now) {
    if (hits.size() < 1024) {
      return;
    }
    Instant evictBefore = now.minus(WINDOW.multipliedBy(5));
    lastTouch.entrySet().removeIf(e -> {
      if (e.getValue().isBefore(evictBefore)) {
        hits.remove(e.getKey());
        return true;
      }
      return false;
    });
  }

  /** Test-only: clear all internal state. */
  public void resetForTests() {
    hits.clear();
    lastTouch.clear();
  }
}
