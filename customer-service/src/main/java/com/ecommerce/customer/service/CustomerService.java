package com.ecommerce.customer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.customer.config.ServiceConfig;
import com.ecommerce.customer.model.Customer;
import com.ecommerce.customer.repository.CustomerRepository;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    ServiceConfig config;

    public List<Customer> getCustomers() {
        List<Customer> customers = new ArrayList<>();
        customerRepository.findAll().forEach(customer -> {
            customer.withComment(config.getProperty());
            customers.add(customer);
        });
        return customers;
    }

    public Customer getCustomer(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        return customer.withComment(config.getProperty());
    }

    public Customer createCustomer(Customer customer) {
        customer.setCustomerId(UUID.randomUUID().toString());
        customerRepository.save(customer);
        return customer.withComment(config.getProperty());
    }

    public Customer updateCustomer(Customer customer) {
        customerRepository.save(customer);
        return customer.withComment(config.getProperty());
    }

    public String deleteCustomer(String customerId) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customerRepository.delete(customer);
        return "Deleting customer with id " + customerId;
    }
}
