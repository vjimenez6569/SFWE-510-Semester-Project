package com.ecommerce.product.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.product.model.Product;

@Repository
public interface ProductRepository extends CrudRepository<Product, String> {

    public List<Product> findByCategory(String category);
}
