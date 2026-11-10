package com.ecommerce.customer.service;

import com.ecommerce.customer.model.Customer;
import com.ecommerce.customer.repository.CustomerRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;

@Service
public class CustomerService {
  private final CustomerRepository repo;

  public CustomerService(CustomerRepository repo) {
    this.repo = repo;
  }

  public List<Customer> getCustomers() {
    return java.util.stream.StreamSupport.stream(repo.findAll().spliterator(), false).toList();
  }

  public Customer getCustomer(String id) {
    return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
  }

  @Transactional
  public Customer findOrCreate(Jwt jwt) {
    return repo.findById(jwt.getSubject()).orElseGet(() -> {
      Customer c = new Customer();
      c.setCustomerId(jwt.getSubject());
      c.setFirstName(defaultString(jwt.getClaimAsString("given_name"), "Customer"));
      c.setLastName(defaultString(jwt.getClaimAsString("family_name"), "Account"));
      c.setEmail(defaultString(jwt.getClaimAsString("email"), jwt.getSubject() + "@local.invalid"));
      return repo.save(c);
    });
  }

  @Transactional
  public Customer updateOwn(Jwt jwt, Customer body) {
    Customer c = findOrCreate(jwt);
    if (body.getFirstName() != null && !body.getFirstName().isBlank())
      c.setFirstName(body.getFirstName().trim());
    if (body.getLastName() != null && !body.getLastName().isBlank())
      c.setLastName(body.getLastName().trim());
    c.setAddress(body.getAddress());
    c.setPhoneNumber(body.getPhoneNumber());
    return repo.save(c);
  }

  public void deleteCustomer(String id) {
    repo.deleteById(id);
  }

  private String defaultString(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
