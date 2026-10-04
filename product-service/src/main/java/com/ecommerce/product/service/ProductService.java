package com.ecommerce.product.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public String deleteProduct(String productId) {
        Product product = new Product();
        product.setProductId(productId);
        productRepository.delete(product);
        return "Deleting product with id " + productId;
    }
}
