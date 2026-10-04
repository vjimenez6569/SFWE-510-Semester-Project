package com.ecommerce.customer.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.customer.model.Customer;

@Repository
public interface CustomerRepository extends CrudRepository<Customer, String> {
}
