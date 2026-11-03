package com.ecommerce.product.repository;

import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.ecommerce.product.model.Product;

@Repository
public interface ProductRepository extends CrudRepository<Product, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.productId = :id")
    Optional<Product> lockById(@Param("id") String id);

    public List<Product> findByCategory(String category);
}
