
package com.example.venta.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

        // ==========================================
        // NATUR COSECHA - ROLES JWT
        // ==========================================

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {

                JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();

                roles.setAuthoritiesClaimName("rol");
                roles.setAuthorityPrefix("ROLE_");

                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

                converter.setJwtGrantedAuthoritiesConverter(roles);

                return converter;
        }

        // ==========================================
        // VALIDAR IDENTIDAD DEL CLIENTE
        // ==========================================

        private boolean clienteValido(Jwt jwt) {

                if (jwt == null || jwt.getSubject() == null) {
                        return false;
                }

                // Solo se permite el rol CLIENTE.
                if (!"CLIENTE".equals(
                                jwt.getClaimAsString("rol"))) {
                        return false;
                }

                // Las cuentas con cambio obligatorio
                // pendiente no acceden a sus compras.
                if (!Boolean.FALSE.equals(
                                jwt.getClaim("debeCambiarPassword"))) {
                        return false;
                }

                // Verificar que el subject sea un ID.
                try {
                        long id = Long.parseLong(jwt.getSubject());
                        return id > 0;
                } catch (NumberFormatException ex) {
                        return false;
                }
        }

        // ==========================================
        // VALIDAR ADMINISTRADOR
        // ==========================================

        private boolean administradorValido(Jwt jwt) {

                if (jwt == null || jwt.getSubject() == null) {
                        return false;
                }

                if (!"ADMIN".equals(
                                jwt.getClaimAsString("rol"))) {
                        return false;
                }

                if (!Boolean.FALSE.equals(
                                jwt.getClaim("debeCambiarPassword"))) {
                        return false;
                }

                try {
                        return Long.parseLong(jwt.getSubject()) > 0;
                } catch (NumberFormatException ex) {
                        return false;
                }
        }

        // ==========================================
        // SEGURIDAD DE VENTA-API
        // ==========================================

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtConverter) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())

                                .cors(Customizer.withDefaults())

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                // ==================================
                                                // WEBPAY - FLUJO PÚBLICO EXISTENTE
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/webpay/iniciar")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/webpay/retorno")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/webpay/retorno")
                                                .permitAll()

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/webpay/estado/*")
                                                .permitAll()

                                                // ==================================
                                                // WEBPAY - RECUPERAR COMPRA AUTORIZADA
                                                // ==================================
                                                //
                                                // No requiere JWT.
                                                // El controlador exige el token Webpay,
                                                // valida el pago y limita la recuperación
                                                // a 15 minutos.
                                                //
                                                // No habilitar /api/ventas/** públicamente.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/webpay/compra/recuperar")
                                                .permitAll()

                                                // ==================================
                                                // WEBPAY - COMPROBANTE PDF INVITADO
                                                // ==================================
                                                //
                                                // La ruta no requiere JWT.
                                                // WebpayController valida el token
                                                // de compra, el pago autorizado y
                                                // el plazo de acceso de 15 minutos.
                                                //
                                                // NO habilitar /api/ventas/**.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/webpay/compra/comprobante/pdf")
                                                .permitAll()

                                                // ==================================
                                                // HU-32: SEGUIMIENTO DE INVITADOS
                                                // ==================================
                                                // Consulta pública con código privado.
                                                // La validación del código se realiza
                                                // en VentaController y VentaServiceImpl.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/ventas/invitados/seguimiento")
                                                .permitAll()

                                                // ==================================
                                                // CLIENTE - MIS COMPRAS
                                                // ==================================
                                                //
                                                // Debe estar ANTES de la regla
                                                // general /api/ventas/**
                                                //
                                                // El cliente no proporciona un ID.
                                                // VentaController lo obtiene del JWT.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/ventas/mis-compras")
                                                .access((authentication, context) -> {

                                                        var usuario = authentication.get();

                                                        boolean permitido = usuario.getPrincipal() instanceof Jwt jwt
                                                                        && clienteValido(jwt);

                                                        return new AuthorizationDecision(
                                                                        permitido);
                                                })

                                                // ==================================
                                                // CLIENTE - DETALLES DE MI COMPRA
                                                // ==================================
                                                //
                                                // Exige JWT CLIENTE válido.
                                                //
                                                // VentaController comprueba además
                                                // que venta.clienteId coincida
                                                // con el subject del JWT.
                                                //
                                                // No permite consultar ventas ajenas.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/ventas/mis-compras/{idVenta}/detalles")
                                                .access((authentication, context) -> {

                                                        var usuario = authentication.get();

                                                        boolean permitido = usuario.getPrincipal() instanceof Jwt jwt
                                                                        && clienteValido(jwt);

                                                        return new AuthorizationDecision(
                                                                        permitido);
                                                })

                                                // ==================================
                                                // CLIENTE JWT - PDF DE MI COMPRA
                                                // ==================================
                                                //
                                                // Solo CLIENTE con JWT válido.
                                                //
                                                // ComprobanteCompraController verifica
                                                // que venta.clienteId coincida con
                                                // el subject del JWT.
                                                //
                                                // No permite descargar PDF ajenos.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/ventas/mis-compras/{ventaId}/comprobante/pdf")
                                                .access((authentication, context) -> {

                                                        var usuario = authentication.get();

                                                        boolean permitido = usuario.getPrincipal() instanceof Jwt jwt
                                                                        && clienteValido(jwt);

                                                        return new AuthorizationDecision(
                                                                        permitido);
                                                })

                                                // ==================================
                                                // HU-32: CLIENTE - SEGUIMIENTO JWT
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/ventas/mis-compras/{idVenta}/seguimiento")
                                                .access((authentication, context) -> {

                                                        var usuario = authentication.get();

                                                        boolean permitido = usuario.getPrincipal() instanceof Jwt jwt
                                                                        && clienteValido(jwt);

                                                        return new AuthorizationDecision(
                                                                        permitido);
                                                })

                                                // ==================================
                                                // ADMIN - GESTIÓN DE VENTAS
                                                // ==================================
                                                //
                                                // Incluye:
                                                // - Listado general
                                                // - Detalles
                                                // - Estados
                                                // - Despachos
                                                // - Comprobantes PDF
                                                // ==================================

                                                .requestMatchers(
                                                                "/api/ventas",
                                                                "/api/ventas/**")
                                                .access((authentication, context) -> {

                                                        var usuario = authentication.get();

                                                        boolean permitido = usuario.getPrincipal() instanceof Jwt jwt
                                                                        && administradorValido(jwt);

                                                        return new AuthorizationDecision(
                                                                        permitido);
                                                })

                                                // ==================================
                                                // RESTO DE RUTAS
                                                // ==================================

                                                .anyRequest().denyAll())

                                // ==================================
                                // VERIFICACIÓN JWT RSA
                                // ==================================

                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                jwtConverter)));

                return http.build();
        }

        // ==========================================
        // CORS - WEB ADMIN LOCAL
        // ==========================================

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {

                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOrigins(List.of(
                                "http://localhost:5173",
                                "http://127.0.0.1:5173"));

                configuration.setAllowedMethods(List.of(
                                "GET",
                                "POST",
                                "PUT",
                                "PATCH",
                                "DELETE",
                                "OPTIONS"));

                configuration.setAllowedHeaders(List.of(
                                "Authorization",
                                "Content-Type"));

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

                source.registerCorsConfiguration(
                                "/**",
                                configuration);

                return source;
        }
}
