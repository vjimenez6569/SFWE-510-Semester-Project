package com.ecommerce.order.service;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;

@Service
public class ProductLookup {
 @Value("${inventory.internal.secret}") private String inventorySecret;
 private final WebClient.Builder client;
 private final CircuitBreakerFactory<?,?> breakers;
 public ProductLookup(WebClient.Builder client, CircuitBreakerFactory<?,?> breakers) { this.client=client; this.breakers=breakers; }
 public ProductDetails get(String productId, String token) {
  return breakers.create("productLookup").run(() -> client.build().get()
   .uri("http://product-service/v1/product/{id}",productId)
   .header(HttpHeaders.AUTHORIZATION,token)
   .retrieve().bodyToMono(ProductDetails.class).block(),
   ex -> { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Product verification unavailable",ex); });
 }
 public void adjustStock(Map<String,Integer> items, boolean restore) {
  client.build().post().uri("http://product-service/v1/product/internal/"+(restore?"restore":"reserve"))
    .header("X-Inventory-Secret", inventorySecret).bodyValue(items).retrieve().toBodilessEntity().block();
 }
 public record ProductDetails(String productId, String productName, BigDecimal price, Integer quantity) {}
}
@Configuration
class ProductLookupConfiguration {
 @Bean @LoadBalanced WebClient.Builder loadBalancedWebClient() { return WebClient.builder(); }
}
