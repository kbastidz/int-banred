package com.bolivariano.microservice.recbanred.core.configuration;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class HttpWebFilter implements WebFilter {


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().getHeaders().add("X-Content-Type-Options", "nosniff");
        exchange.getResponse().getHeaders().add("Content-Security-Policy", "default-src 'self'; script-src 'self'; object-src 'none'; frame-ancestors 'none'");
        exchange.getResponse().getHeaders().add("Referrer-Policy", "strict-origin");
        exchange.getResponse().getHeaders().add("Permissions-Policy", "accelerometer=(), autoplay=(), camera=(), display-capture=(), encrypted-media=(), fullscreen=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), picture-in-picture=(), usb=()");
        exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload");
        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate");
        exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponse().getHeaders().add(HttpHeaders.EXPIRES, "0");
        exchange.getResponse().getHeaders().add(HttpHeaders.DATE, ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME));
        exchange.getResponse().getHeaders().add(HttpHeaders.PRAGMA, "no-cache");
        exchange.getResponse().getHeaders().add("Transfer-Encoding", "chunked");

        return chain.filter(exchange);
    }

}

