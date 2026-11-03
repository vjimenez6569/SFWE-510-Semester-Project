package com.ecommerce.customer.controller;
import com.ecommerce.customer.model.Customer;
import com.ecommerce.customer.service.CustomerService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/v1/customer")
public class CustomerController {
 private final CustomerService service;
 public CustomerController(CustomerService service) { this.service = service; }
 @PreAuthorize("hasAuthority('MANAGE_CUSTOMERS')")
 @GetMapping public List<Customer> all() { return service.getCustomers(); }
 @PreAuthorize("hasAuthority('MANAGE_CUSTOMERS')")
 @GetMapping("/{id}") public Customer one(@PathVariable String id) { return service.getCustomer(id); }
 @PreAuthorize("hasAuthority('MANAGE_OWN_PROFILE')")
 @GetMapping("/me") public Customer me(@AuthenticationPrincipal Jwt jwt) { return service.findOrCreate(jwt); }
 @PreAuthorize("hasAuthority('MANAGE_OWN_PROFILE')")
 @PutMapping("/me") public Customer updateMe(@AuthenticationPrincipal Jwt jwt, @RequestBody Customer body) { return service.updateOwn(jwt,body); }
 @PreAuthorize("hasAuthority('MANAGE_CUSTOMERS')")
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable String id) { service.deleteCustomer(id); return ResponseEntity.noContent().build(); }
}
