package com.ecommerce.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.order.model.CustomerOrder;

@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, String> {

    public List<CustomerOrder> findByCustomerId(String customerId);
}
