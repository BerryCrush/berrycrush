package org.berrycrush.samples.microservices.inventory.chaos;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for managing chaos/fault injection configuration.
 * Used in BerryCrush scenarios to test retry and resilience patterns.
 */
@RestController
@RequestMapping("/api/chaos")
public class ChaosController {
    
    private final ChaosConfig chaosConfig;
    
    public ChaosController(ChaosConfig chaosConfig) {
        this.chaosConfig = chaosConfig;
    }
    
    /**
     * Enable chaos mode to simulate transient failures.
     * @param failCount Number of requests that should fail before succeeding
     * @param statusCode HTTP status code to return for failures (default: 503)
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableChaos(
            @RequestParam(defaultValue = "2") int failCount,
            @RequestParam(defaultValue = "503") int statusCode) {
        chaosConfig.enable(failCount, statusCode);
        return ResponseEntity.ok(Map.of(
            "enabled", true,
            "failCount", failCount,
            "statusCode", statusCode,
            "message", "Chaos mode enabled. Next " + failCount + " requests to /api/inventory/* will return " + statusCode
        ));
    }
    
    /**
     * Disable chaos mode.
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableChaos() {
        chaosConfig.disable();
        return ResponseEntity.ok(Map.of(
            "enabled", false,
            "message", "Chaos mode disabled"
        ));
    }
    
    /**
     * Reset the failure counter without disabling chaos mode.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        chaosConfig.reset();
        return ResponseEntity.ok(Map.of(
            "reset", true,
            "message", "Chaos failure counter reset"
        ));
    }
    
    /**
     * Get current chaos configuration status.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getChaosStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", chaosConfig.isEnabled(),
            "failCount", chaosConfig.getFailCount(),
            "statusCode", chaosConfig.getStatusCode(),
            "currentFailCount", chaosConfig.getCurrentFailCount()
        ));
    }
}
