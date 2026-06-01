package org.berrycrush.samples.webflux.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("PRODUCTS")
public record Product(
    @Id Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stock
) {
    public Product withId(Long id) {
        return new Product(id, name, description, price, stock);
    }
    
    public Product withStock(Integer stock) {
        return new Product(id, name, description, price, stock);
    }
}
