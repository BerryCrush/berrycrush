package org.berrycrush.samples.microservices.order.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
    String customerId,
    String productId,
    Integer quantity,
    BigDecimal unitPrice
) {}
