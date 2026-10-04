package com.ecommerce.order.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude="order")
@Entity
@Table(name="order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="order_item_id")
    private Long orderItemId;

    @Column(name="product_id", nullable=false)
    private String productId;

    @Column(name="product_name", nullable=false)
    private String productName;

    @Column(nullable=false)
    private Integer quantity;

    @Column(nullable=false)
    private BigDecimal price;

    @JsonBackReference
    @ManyToOne
    @JoinColumn(name="order_id", nullable=false)
    private CustomerOrder order;
}