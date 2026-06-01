package org.berrycrush.samples.microservices.order.controller;

import org.berrycrush.samples.microservices.order.dto.CreateOrderRequest;
import org.berrycrush.samples.microservices.order.model.Order;
import org.berrycrush.samples.microservices.order.model.OrderStatus;
import org.berrycrush.samples.microservices.order.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderRepository repository;
    
    public OrderController(OrderRepository repository) {
        this.repository = repository;
    }
    
    @GetMapping
    public List<Order> listOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) OrderStatus status) {
        if (customerId != null) {
            return repository.findByCustomerId(customerId);
        }
        if (status != null) {
            return repository.findByStatus(status);
        }
        return repository.findAll();
    }
    
    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        Order order = new Order(
            request.customerId(),
            request.productId(),
            request.quantity(),
            request.unitPrice().multiply(java.math.BigDecimal.valueOf(request.quantity()))
        );
        return repository.save(order);
    }
    
    @PutMapping("/{id}/status")
    public Order updateOrderStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        Order order = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        return repository.save(order);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
        Order order = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        repository.save(order);
        return ResponseEntity.noContent().build();
    }
}
