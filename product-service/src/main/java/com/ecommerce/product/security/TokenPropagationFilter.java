package com.ecommerce.product.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TokenPropagationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenPropagationFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/v1/product/")) {

            String token = request.getHeader(HttpHeaders.AUTHORIZATION);

            log.info(
                    "[TOKEN PROPAGATION] Product Service received JWT: {}",
                    token != null && token.startsWith("Bearer "));
        }

        filterChain.doFilter(request, response);
    }
}