package com.computronica.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // === PERMITE TU FRONTEND ===
        config.addAllowedOrigin("http://localhost:4200"); // Angular
        // config.addAllowedOrigin("http://127.0.0.1:4200");
        // config.addAllowedOrigin("https://tu-dominio.com"); // Producción

        // === PERMITE TODO (métodos, headers, credenciales) ===
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");

        // === REGISTRA PARA TODAS LAS RUTAS ===
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}