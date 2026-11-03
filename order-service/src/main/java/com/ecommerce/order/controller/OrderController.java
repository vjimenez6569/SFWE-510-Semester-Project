package com.ecommerce.order.controller;
import com.ecommerce.order.model.CustomerOrder;
import com.ecommerce.order.service.OrderService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/v1/order")
public class OrderController {
 private final OrderService service;
 public OrderController(OrderService service) { this.service=service; }
 @PreAuthorize("hasAuthority('VIEW_OWN_ORDERS') or hasAuthority('MANAGE_ORDERS')")
 @GetMapping public List<CustomerOrder> list(@AuthenticationPrincipal Jwt jwt) { return service.listFor(jwt); }
 @PreAuthorize("hasAuthority('VIEW_OWN_ORDERS') or hasAuthority('MANAGE_ORDERS')")
 @GetMapping("/{id}") public CustomerOrder get(@PathVariable String id,@AuthenticationPrincipal Jwt jwt) { return service.getFor(id,jwt); }
 @PreAuthorize("hasAuthority('PLACE_ORDERS')")
 @PostMapping public CustomerOrder create(@RequestBody CustomerOrder order,@RequestHeader("Authorization") String token,@AuthenticationPrincipal Jwt jwt) { return service.createFor(order,token,jwt); }
 @PreAuthorize("hasAuthority('MANAGE_ORDERS')")
 @PatchMapping("/{id}/status") public CustomerOrder changeStatus(@PathVariable String id, @RequestBody java.util.Map<String,String> body) {
     return service.changeStatus(id, body.get("status"));
 }
 @PreAuthorize("hasAuthority('MANAGE_ORDERS')")
 @PutMapping public CustomerOrder update(@RequestBody CustomerOrder order) { return service.updateOrder(order); }
 @PreAuthorize("hasAuthority('MANAGE_ORDERS')")
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id) { service.deleteOrder(id);return ResponseEntity.noContent().build(); }
}
