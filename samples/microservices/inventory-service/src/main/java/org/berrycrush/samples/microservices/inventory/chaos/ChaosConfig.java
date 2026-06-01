package org.berrycrush.samples.microservices.inventory.chaos;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Chaos configuration for simulating transient failures.
 * Used to test retry mechanisms in BerryCrush scenarios.
 */
@Component
public class ChaosConfig {
    
    private volatile boolean enabled = false;
    private volatile int failCount = 0;
    private volatile int statusCode = 503;
    private final AtomicInteger currentFailCount = new AtomicInteger(0);
    
    public void enable(int failCount, int statusCode) {
        this.failCount = failCount;
        this.statusCode = statusCode;
        this.currentFailCount.set(0);
        this.enabled = true;
    }
    
    public void disable() {
        this.enabled = false;
        this.currentFailCount.set(0);
    }
    
    public void reset() {
        this.currentFailCount.set(0);
    }
    
    /**
     * Check if the current request should fail.
     * Returns the status code to return, or 0 if the request should proceed normally.
     */
    public int shouldFail() {
        if (!enabled) {
            return 0;
        }
        int current = currentFailCount.incrementAndGet();
        if (current <= failCount) {
            return statusCode;
        }
        return 0;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getFailCount() {
        return failCount;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public int getCurrentFailCount() {
        return currentFailCount.get();
    }
}
