package com.ecommerce.customer.security;

import java.util.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth
        .requestMatchers("/actuator/health", "/actuator/info").permitAll()

        .requestMatchers("/v1/**").authenticated().anyRequest().denyAll())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter())));
    return http.build();
  }

  @Bean
  JwtAuthenticationConverter converter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwt -> {
      Set<org.springframework.security.core.GrantedAuthority> authorities = new HashSet<>();
      Object realm = jwt.getClaim("realm_access");
      if (realm instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> roles)
        for (Object role : roles)
          authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
      Object resources = jwt.getClaim("resource_access");
      if (resources instanceof Map<?, ?> clients && clients.get("wyntermarket-project") instanceof Map<?, ?> app
          && app.get("roles") instanceof Collection<?> roles)
        for (Object role : roles)
          authorities.add(new SimpleGrantedAuthority(role.toString()));
      return authorities;
    });
    return converter;
  }
}
