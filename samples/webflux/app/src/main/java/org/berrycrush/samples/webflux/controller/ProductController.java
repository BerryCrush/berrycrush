package org.berrycrush.samples.webflux.controller;

import org.berrycrush.samples.webflux.model.Product;
import org.berrycrush.samples.webflux.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    private final ProductRepository repository;
    private final Sinks.Many<Product> productSink;
    
    public ProductController(ProductRepository repository) {
        this.repository = repository;
        this.productSink = Sinks.many().multicast().onBackpressureBuffer();
    }
    
    @GetMapping
    public Flux<Product> getAllProducts(@RequestParam(required = false) String search) {
        if (search != null && !search.isEmpty()) {
            return repository.findByNameContainingIgnoreCase(search);
        }
        return repository.findAll();
    }
    
    @GetMapping("/{id}")
    public Mono<Product> getProductById(@PathVariable Long id) {
        return repository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")));
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> createProduct(@RequestBody Product product) {
        return repository.save(product)
            .doOnSuccess(productSink::tryEmitNext);
    }
    
    @PutMapping("/{id}")
    public Mono<Product> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        return repository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")))
            .flatMap(existing -> repository.save(product.withId(id)))
            .doOnSuccess(productSink::tryEmitNext);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(@PathVariable Long id) {
        return repository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found")))
            .flatMap(product -> repository.deleteById(id));
    }
    
    /**
     * Server-Sent Events endpoint for real-time product updates
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Product>> streamProducts() {
        return productSink.asFlux()
            .map(product -> ServerSentEvent.<Product>builder()
                .id(String.valueOf(product.id()))
                .event("product-update")
                .data(product)
                .build());
    }
    
    /**
     * Streaming endpoint with simulated delay (for demonstrating reactive behavior)
     */
    @GetMapping(value = "/delayed", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Product> getProductsWithDelay() {
        return repository.findAll()
            .delayElements(Duration.ofMillis(100));
    }
}
