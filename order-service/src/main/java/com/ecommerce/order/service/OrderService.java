package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.order.config.ServiceConfig;
import com.ecommerce.order.model.CustomerOrder;
import com.ecommerce.order.model.OrderItem;
import com.ecommerce.order.repository.OrderRepository;

@Service
public class OrderService {

    @Autowired
    private ProductLookup productLookup;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ServiceConfig config;

    public List<CustomerOrder> listFor(org.springframework.security.oauth2.jwt.Jwt jwt) {
        boolean admin = hasAdmin(jwt);
        return getOrders().stream().filter(order -> admin || jwt.getSubject().equals(order.getCustomerId())).toList();
    }
    public CustomerOrder getFor(String id, org.springframework.security.oauth2.jwt.Jwt jwt) {
        CustomerOrder order = getOrder(id);
        if (!hasAdmin(jwt) && !jwt.getSubject().equals(order.getCustomerId()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,"Not your order");
        return order;
    }
    public CustomerOrder createFor(CustomerOrder order, String token, org.springframework.security.oauth2.jwt.Jwt jwt) {
        order.setCustomerId(jwt.getSubject());
        String name = jwt.getClaimAsString("preferred_username");
        order.setCustomerName(name == null ? jwt.getSubject() : name);
        order.setStatus("NEW");
        return createOrder(order,token);
    }
    private boolean hasAdmin(org.springframework.security.oauth2.jwt.Jwt jwt) {
        Object resources=jwt.getClaim("resource_access");
        if (resources instanceof java.util.Map<?,?> clients && clients.get("ecommerce-project") instanceof java.util.Map<?,?> app && app.get("roles") instanceof java.util.Collection<?> roles)
            return roles.contains("MANAGE_ORDERS");
        return false;
    }

    public List<CustomerOrder> getOrders() {
        List<CustomerOrder> orders = new ArrayList<>();

        orderRepository.findAll().forEach(order -> {
            order.withComment(config.getProperty());
            orders.add(order);
        });

        return orders;
    }

    public CustomerOrder getOrder(String orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));

        return order.withComment(config.getProperty());
    }

    @Transactional
    public CustomerOrder createOrder(CustomerOrder order, String bearerToken) {

        order.setOrderId(UUID.randomUUID().toString());
        order.setOrderDate(LocalDateTime.now());

        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }

        Map<String,Integer> requested = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            requested.merge(item.getProductId(), item.getQuantity() == null ? 0 : item.getQuantity(), Integer::sum);

            if (item.getQuantity() == null || item.getQuantity() <= 0) throw new IllegalArgumentException("Quantity must be positive");
            ProductLookup.ProductDetails product = productLookup.get(item.getProductId(), bearerToken);
            if (product == null || product.price() == null || product.quantity() == null || product.quantity() < item.getQuantity())
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Product unavailable or insufficient stock");
            item.setProductName(product.productName());
            item.setPrice(product.price());
            item.setOrderItemId(null);
            item.setOrder(order);
        }

        order.setTotal(calculateTotal(order.getItems()));
        // Recheck aggregate quantities and atomically decrement product rows.
        productLookup.adjustStock(requested, false);
        try {
            CustomerOrder savedOrder = orderRepository.saveAndFlush(order);
            return savedOrder.withComment(config.getProperty());
        } catch (RuntimeException error) {
            try { productLookup.adjustStock(requested, true); }
            catch (Exception compensationFailure) { error.addSuppressed(compensationFailure); }
            throw error;
        }
    }

    @Transactional
    public CustomerOrder changeStatus(String id, String status) {
        if (status == null || !java.util.Set.of("NEW", "PROCESSING", "SHIPPED", "DELIVERED").contains(status)) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid order status");
        }
        CustomerOrder order = orderRepository.findById(id).orElseThrow(() ->
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(status);
        return orderRepository.save(order).withComment(config.getProperty());
    }

    @Transactional
    public CustomerOrder updateOrder(CustomerOrder request) {

        CustomerOrder existingOrder = orderRepository
                .findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + request.getOrderId()));

        // Update the order information
        existingOrder.setCustomerId(request.getCustomerId());
        existingOrder.setCustomerName(request.getCustomerName());
        existingOrder.setStatus(request.getStatus());

        // Replace the existing items with the items from the PUT request
        existingOrder.getItems().clear();

        if (request.getItems() != null) {

            for (OrderItem item : request.getItems()) {
                item.setOrderItemId(null);

                existingOrder.addItem(item);
            }
        }
        existingOrder.setTotal(
                calculateTotal(existingOrder.getItems()));

        CustomerOrder savedOrder = orderRepository.save(existingOrder);

        return savedOrder.withComment(config.getProperty());
    }

    @Transactional
    public String deleteOrder(String orderId) {

        CustomerOrder order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));

        orderRepository.delete(order);

        return "Deleting order with id " + orderId;
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {

        if (items == null) {
            return BigDecimal.ZERO;
        }

        return items.stream()
                .filter(item -> item.getPrice() != null
                        && item.getQuantity() != null)
                .map(item -> item.getPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add);
    }
}