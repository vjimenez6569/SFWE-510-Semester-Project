package com.ecommerce.gatewayserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class GatewaySecurity {
  @Bean
  SecurityWebFilterChain security(ServerHttpSecurity http) {
    return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(ex -> ex
            .pathMatchers("/auth/**", "/actuator/health").permitAll()
            .pathMatchers(HttpMethod.GET, "/v1/product/**").permitAll()
            .pathMatchers("/v1/**").authenticated()
            .anyExchange().denyAll())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
        })).build();
  }
}
