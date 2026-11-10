package com.ecommerce.product.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecommerce.product.config.ServiceConfig;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    ServiceConfig config;

    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        productRepository.findAll().forEach(product -> {
            product.withComment(config.getProperty());
            products.add(product);
        });
        return products;
    }

    public Product getProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        return product.withComment(config.getProperty());
    }

    public Product createProduct(Product product) {
        product.setProductId(UUID.randomUUID().toString());
        productRepository.save(product);
        return product.withComment(config.getProperty());
    }

    public Product updateProduct(Product product) {
        productRepository.save(product);
        return product.withComment(config.getProperty());
    }

    @Transactional
    public void changeStock(Map<String, Integer> quantities, boolean restore) {
        // Sorted locking order reduces deadlock risk across concurrent checkouts.
        for (var entry : new TreeMap<>(quantities).entrySet()) {
            int requested = entry.getValue();
            if (requested <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid quantity");
            Product product = productRepository.lockById(entry.getKey())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Product no longer available"));
            if (!restore && product.getQuantity() < requested)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient inventory for " + product.getProductName());
            product.setQuantity(product.getQuantity() + (restore ? requested : -requested));
            productRepository.save(product);
        }
    }

    public String deleteProduct(String productId) {
        Product product = new Product();
        product.setProductId(productId);
        productRepository.delete(product);
        return "Deleting product with id " + productId;
    }
}
