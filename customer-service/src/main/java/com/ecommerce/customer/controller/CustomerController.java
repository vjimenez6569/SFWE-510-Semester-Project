package com.ecommerce.customer.controller;

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

import com.ecommerce.customer.model.Customer;
import com.ecommerce.customer.service.CustomerService;

@RestController
@RequestMapping(value = "v1/customer")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Customer>> getCustomers() {
        return ResponseEntity.ok(customerService.getCustomers());
    }

    @RequestMapping(value = "/{customerId}", method = RequestMethod.GET)
    public ResponseEntity<Customer> getCustomer(@PathVariable("customerId") String customerId) {
        Customer customer = customerService.getCustomer(customerId);

        customer.add(
                linkTo(methodOn(CustomerController.class).getCustomer(customer.getCustomerId())).withSelfRel(),
                linkTo(methodOn(CustomerController.class).createCustomer(customer)).withRel("createCustomer"),
                linkTo(methodOn(CustomerController.class).updateCustomer(customer)).withRel("updateCustomer"),
                linkTo(methodOn(CustomerController.class).deleteCustomer(customer.getCustomerId())).withRel("deleteCustomer")
        );

        return ResponseEntity.ok(customer);
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer request) {
        return ResponseEntity.ok(customerService.createCustomer(request));
    }

    @PutMapping
    public ResponseEntity<Customer> updateCustomer(@RequestBody Customer request) {
        return ResponseEntity.ok(customerService.updateCustomer(request));
    }

    @DeleteMapping(value = "/{customerId}")
    public ResponseEntity<String> deleteCustomer(@PathVariable("customerId") String customerId) {
        return ResponseEntity.ok(customerService.deleteCustomer(customerId));
    }
}
