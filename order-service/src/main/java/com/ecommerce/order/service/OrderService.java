package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private OrderRepository orderRepository;

    @Autowired
    private ServiceConfig config;

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
    public CustomerOrder createOrder(CustomerOrder order) {

        order.setOrderId(UUID.randomUUID().toString());
        order.setOrderDate(LocalDateTime.now());

        if (order.getItems() == null) {
            order.setItems(new ArrayList<>());
        }

        for (OrderItem item : order.getItems()) {

            item.setOrderItemId(null);

            item.setOrder(order);
        }

        order.setTotal(calculateTotal(order.getItems()));

        CustomerOrder savedOrder = orderRepository.save(order);

        return savedOrder.withComment(config.getProperty());
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