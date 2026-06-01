package org.berrycrush.samples.microservices.inventory.controller;

import org.berrycrush.samples.microservices.inventory.chaos.ChaosConfig;
import org.berrycrush.samples.microservices.inventory.model.InventoryItem;
import org.berrycrush.samples.microservices.inventory.repository.InventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    
    private final InventoryRepository repository;
    private final ChaosConfig chaosConfig;
    
    public InventoryController(InventoryRepository repository, ChaosConfig chaosConfig) {
        this.repository = repository;
        this.chaosConfig = chaosConfig;
    }
    
    private void checkChaos() {
        int failStatus = chaosConfig.shouldFail();
        if (failStatus > 0) {
            throw new ResponseStatusException(
                HttpStatus.valueOf(failStatus),
                "Chaos mode: simulated failure " + chaosConfig.getCurrentFailCount() + " of " + chaosConfig.getFailCount()
            );
        }
    }
    
    @GetMapping
    public List<InventoryItem> listInventory() {
        checkChaos();
        return repository.findAll();
    }
    
    @GetMapping("/{sku}")
    public InventoryItem getInventoryItem(@PathVariable String sku) {
        checkChaos();
        return repository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + sku));
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryItem createInventoryItem(@RequestBody InventoryItem item) {
        checkChaos();
        if (repository.findBySku(item.getSku()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Item with SKU already exists: " + item.getSku());
        }
        return repository.save(item);
    }
    
    @PutMapping("/{sku}")
    public InventoryItem updateInventoryItem(@PathVariable String sku, @RequestBody InventoryItem item) {
        checkChaos();
        InventoryItem existing = repository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + sku));
        existing.setName(item.getName());
        existing.setQuantity(item.getQuantity());
        existing.setUnitPrice(item.getUnitPrice());
        return repository.save(existing);
    }
    
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> deleteInventoryItem(@PathVariable String sku) {
        checkChaos();
        InventoryItem existing = repository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + sku));
        repository.delete(existing);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Check availability of a specific item.
     */
    @GetMapping("/{sku}/availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @PathVariable String sku,
            @RequestParam(defaultValue = "1") int quantity) {
        checkChaos();
        InventoryItem item = repository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + sku));
        
        boolean available = item.getAvailableQuantity() >= quantity;
        return ResponseEntity.ok(Map.of(
            "sku", sku,
            "requestedQuantity", quantity,
            "availableQuantity", item.getAvailableQuantity(),
            "available", available
        ));
    }
    
    /**
     * Reserve inventory for an order.
     */
    @PostMapping("/{sku}/reserve")
    public ResponseEntity<Map<String, Object>> reserveInventory(
            @PathVariable String sku,
            @RequestParam int quantity) {
        checkChaos();
        InventoryItem item = repository.findBySku(sku)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found: " + sku));
        
        if (item.getAvailableQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient inventory");
        }
        
        item.setReservedQuantity(item.getReservedQuantity() + quantity);
        repository.save(item);
        
        return ResponseEntity.ok(Map.of(
            "sku", sku,
            "reservedQuantity", quantity,
            "remainingAvailable", item.getAvailableQuantity()
        ));
    }
}
