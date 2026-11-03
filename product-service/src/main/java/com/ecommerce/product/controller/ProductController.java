package com.ecommerce.product.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;

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

    @Value("${inventory.internal.secret}")
    private String inventorySecret;

    private void requireInternalSecret(String provided) {
        if (provided == null || !java.security.MessageDigest.isEqual(
                provided.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                inventorySecret.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal inventory access only");
    }

    @PostMapping("/internal/reserve")
    public ResponseEntity<Void> reserve(@RequestHeader(value="X-Inventory-Secret",required=false) String secret,
                                        @RequestBody Map<String,Integer> quantities) {
        requireInternalSecret(secret);
        productService.changeStock(quantities, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/internal/restore")
    public ResponseEntity<Void> restore(@RequestHeader(value="X-Inventory-Secret",required=false) String secret,
                                        @RequestBody Map<String,Integer> quantities) {
        requireInternalSecret(secret);
        productService.changeStock(quantities, true);
        return ResponseEntity.noContent().build();
    }


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

    @PreAuthorize("hasAuthority('MANAGE_PRODUCTS')")
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product request) {
        return ResponseEntity.ok(productService.createProduct(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_PRODUCTS')")
    @PutMapping
    public ResponseEntity<Product> updateProduct(@RequestBody Product request) {
        return ResponseEntity.ok(productService.updateProduct(request));
    }

    @PreAuthorize("hasAuthority('MANAGE_PRODUCTS')")
    @DeleteMapping(value="/{productId}")
    public ResponseEntity<String> deleteProduct(@PathVariable("productId") String productId) {
        return ResponseEntity.ok(productService.deleteProduct(productId));
    }
}
