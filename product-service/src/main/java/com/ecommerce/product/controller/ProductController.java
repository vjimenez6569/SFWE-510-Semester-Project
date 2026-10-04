package com.ecommerce.product.controller;

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

import com.ecommerce.product.model.Product;
import com.ecommerce.product.service.ProductService;

@RestController
@RequestMapping(value="v1/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(productService.getProducts());
    }

    @RequestMapping(value="/{productId}", method = RequestMethod.GET)
    public ResponseEntity<Product> getProduct(@PathVariable("productId") String productId) {
        Product product = productService.getProduct(productId);

        product.add(
                linkTo(methodOn(ProductController.class).getProduct(product.getProductId())).withSelfRel(),
                linkTo(methodOn(ProductController.class).createProduct(product)).withRel("createProduct"),
                linkTo(methodOn(ProductController.class).updateProduct(product)).withRel("updateProduct"),
                linkTo(methodOn(ProductController.class).deleteProduct(product.getProductId())).withRel("deleteProduct")
        );

        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PutMapping
    public ResponseEntity<Product> updateProduct(@RequestBody Product request) {
        return ResponseEntity.ok(productService.updateProduct(request));
    }

    @DeleteMapping(value="/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable("productId") String productId) {
        return ResponseEntity.ok(productService.deleteProduct(productId));
    }
}
