package org.berrycrush.samples.microservices.order.repository;

import org.berrycrush.samples.microservices.order.model.Order;
import org.berrycrush.samples.microservices.order.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerId(String customerId);
    List<Order> findByStatus(OrderStatus status);
}
