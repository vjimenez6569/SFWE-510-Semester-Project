package com.ecommerce.order.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.order.model.CustomerOrder;

@Repository
public interface OrderRepository extends CrudRepository<CustomerOrder, String> {

    public List<CustomerOrder> findByCustomerId(String customerId);
}
