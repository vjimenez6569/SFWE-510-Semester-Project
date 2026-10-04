package com.ecommerce.order.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.order.model.CustomerOrder;
import com.ecommerce.order.service.OrderService;

@RestController
@RequestMapping(value="v1/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<CustomerOrder>> getOrders() {
        return ResponseEntity.ok(orderService.getOrders());
    }

    @RequestMapping(value="/{orderId}", method = RequestMethod.GET)
    public ResponseEntity<CustomerOrder> getOrder(@PathVariable("orderId") String orderId) {
        CustomerOrder order = orderService.getOrder(orderId);

        order.add(
                linkTo(methodOn(OrderController.class).getOrder(order.getOrderId())).withSelfRel(),
                linkTo(methodOn(OrderController.class).createOrder(order)).withRel("createOrder"),
                linkTo(methodOn(OrderController.class).updateOrder(order)).withRel("updateOrder"),
                linkTo(methodOn(OrderController.class).deleteOrder(order.getOrderId())).withRel("deleteOrder")
        );

        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<CustomerOrder> createOrder(@RequestBody CustomerOrder request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PutMapping
    public ResponseEntity<CustomerOrder> updateOrder(@RequestBody CustomerOrder request) {
        return ResponseEntity.ok(orderService.updateOrder(request));
    }

    @DeleteMapping(value="/{orderId}")
    public ResponseEntity<String> deleteOrder(@PathVariable("orderId") String orderId) {
        return ResponseEntity.ok(orderService.deleteOrder(orderId));
    }
}
