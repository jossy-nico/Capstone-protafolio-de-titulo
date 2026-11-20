
package com.example.usuario.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.jdbc.core.JdbcTemplate;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        // CONTRASEÑAS BCrypt
        // ==========================================

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        // ==========================================
        // ROLES DESDE EL JWT
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
        // ESTADO REAL DE LA CUENTA EN POSTGRESQL
        // ==========================================

        private record CuentaActual(
                        String rol,
                        String estado,
                        boolean debeCambiarPassword) {
        }

        private CuentaActual consultarCuenta(
                        Jwt jwt,
                        JdbcTemplate jdbcTemplate) {

                if (jwt == null || jwt.getSubject() == null) {
                        return null;
                }

                final long idUsuario;

                try {
                        idUsuario = Long.parseLong(jwt.getSubject());
                } catch (NumberFormatException ex) {
                        return null;
                }

                List<CuentaActual> cuentas = jdbcTemplate.query(
                                """
                                                SELECT
                                                    tipo_usuario,
                                                    estado,
                                                    debe_cambiar_password
                                                FROM usuarios
                                                WHERE id_usuario = ?
                                                """,
                                (rs, fila) -> new CuentaActual(
                                                rs.getString("tipo_usuario"),
                                                rs.getString("estado"),
                                                rs.getBoolean("debe_cambiar_password")),
                                idUsuario);

                return cuentas.isEmpty() ? null : cuentas.get(0);
        }

        // ==========================================
        // VALIDAR CUENTA ACTIVA
        // ==========================================

        private boolean cuentaActiva(
                        Jwt jwt,
                        JdbcTemplate jdbcTemplate) {

                CuentaActual cuenta = consultarCuenta(
                                jwt,
                                jdbcTemplate);

                return cuenta != null
                                && "ACTIVO".equals(cuenta.estado());
        }

        // ==========================================
        // VALIDAR ADMINISTRADOR
        // ==========================================

        private boolean administradorValido(
                        Jwt jwt,
                        JdbcTemplate jdbcTemplate) {

                CuentaActual cuenta = consultarCuenta(
                                jwt,
                                jdbcTemplate);

                if (cuenta == null) {
                        return false;
                }

                return "ACTIVO".equals(cuenta.estado())
                                && "ADMIN".equals(cuenta.rol())
                                && "ADMIN".equals(jwt.getClaimAsString("rol"))
                                && !cuenta.debeCambiarPassword();
        }

        // ==========================================
        // SEGURIDAD DE ENDPOINTS
        // ==========================================

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationConverter jwtConverter,
                        JdbcTemplate jdbcTemplate) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())

                                .cors(Customizer.withDefaults())

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                // ==================================
                                                // LOGIN PÚBLICO
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/usuarios/login",
                                                                "/api/usuarios/login-token")
                                                .permitAll()

                                                // ==================================
                                                // SESIÓN PERSISTENTE - REFRESH TOKEN
                                                // ==================================
                                                //
                                                // No requiere access token JWT.
                                                // El controlador y el servicio deberán
                                                // validar el refresh token recibido.
                                                //
                                                // La renovación solo se permitirá
                                                // para cuentas activas.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/usuarios/login-token-persistente",
                                                                "/api/usuarios/refresh-token",
                                                                "/api/usuarios/logout-token")
                                                .permitAll()

                                                // ==================================
                                                // REGISTRO PÚBLICO DE CLIENTES
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/usuarios")
                                                .permitAll()

                                                // ==================================
                                                // CAMBIO DE CONTRASEÑA
                                                // SOLO TITULAR DE CUENTA ACTIVA
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/usuarios/{idUsuario}/cambiar-password")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        if (!(autenticacion.getPrincipal() instanceof Jwt jwt)) {

                                                                return new AuthorizationDecision(false);
                                                        }

                                                        String idSolicitado = context
                                                                        .getVariables()
                                                                        .get("idUsuario");

                                                        boolean esTitular = jwt.getSubject() != null
                                                                        && jwt.getSubject().equals(idSolicitado);

                                                        boolean permitido = esTitular
                                                                        && cuentaActiva(jwt, jdbcTemplate);

                                                        // Se permite el cambio incluso cuando
                                                        // debeCambiarPassword es true.

                                                        return new AuthorizationDecision(permitido);
                                                })

                                                // ==================================
                                                // PERFIL RESUMIDO
                                                // TITULAR ACTIVO O ADMINISTRADOR
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/usuarios/{idUsuario}/resumen")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        if (!(autenticacion.getPrincipal() instanceof Jwt jwt)) {

                                                                return new AuthorizationDecision(false);
                                                        }

                                                        String idSolicitado = context
                                                                        .getVariables()
                                                                        .get("idUsuario");

                                                        boolean esTitular = jwt.getSubject() != null
                                                                        && jwt.getSubject().equals(idSolicitado)
                                                                        && cuentaActiva(jwt, jdbcTemplate);

                                                        boolean esAdmin = administradorValido(jwt, jdbcTemplate);

                                                        return new AuthorizationDecision(
                                                                        esTitular || esAdmin);
                                                })

                                                // ==================================
                                                // CLIENTES - CREAR PERFIL CON JWT
                                                // ==================================
                                                //
                                                // No es una ruta pública.
                                                // ClienteController deberá comprobar
                                                // que el usuario asociado al perfil
                                                // sea el titular del JWT.
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/clientes/mi-perfil")
                                                .authenticated()

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/clientes")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        boolean permitido = autenticacion
                                                                        .getPrincipal() instanceof Jwt jwt
                                                                        && "CLIENTE".equals(
                                                                                        jwt.getClaimAsString("rol"))
                                                                        && cuentaActiva(jwt, jdbcTemplate);

                                                        return new AuthorizationDecision(permitido);
                                                })

                                                // ==================================
                                                // CLIENTES - CONSULTAR PERFIL PROPIO
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/clientes/{usuarioId}")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        if (!(autenticacion.getPrincipal() instanceof Jwt jwt)) {
                                                                return new AuthorizationDecision(false);
                                                        }

                                                        boolean esTitular = jwt.getSubject() != null
                                                                        && jwt.getSubject().equals(
                                                                                        context.getVariables().get(
                                                                                                        "usuarioId"))
                                                                        && "CLIENTE".equals(
                                                                                        jwt.getClaimAsString("rol"))
                                                                        && cuentaActiva(jwt, jdbcTemplate);

                                                        boolean esAdmin = administradorValido(jwt, jdbcTemplate);

                                                        return new AuthorizationDecision(
                                                                        esTitular || esAdmin);
                                                })

                                                // ==================================
                                                // CLIENTES - ACTUALIZAR PERFIL PROPIO
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/clientes/{usuarioId}")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        if (!(autenticacion.getPrincipal() instanceof Jwt jwt)) {
                                                                return new AuthorizationDecision(false);
                                                        }

                                                        boolean esTitular = jwt.getSubject() != null
                                                                        && jwt.getSubject().equals(
                                                                                        context.getVariables().get(
                                                                                                        "usuarioId"))
                                                                        && "CLIENTE".equals(
                                                                                        jwt.getClaimAsString("rol"))
                                                                        && cuentaActiva(jwt, jdbcTemplate);

                                                        return new AuthorizationDecision(esTitular);
                                                })

                                                // ==================================
                                                // GESTIÓN DE EMPLEADOS
                                                // SOLO ADMIN ACTIVO Y HABILITADO
                                                // ==================================

                                                .requestMatchers(
                                                                "/api/empleados",
                                                                "/api/empleados/**")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        boolean permitido = autenticacion
                                                                        .getPrincipal() instanceof Jwt jwt
                                                                        && administradorValido(
                                                                                        jwt,
                                                                                        jdbcTemplate);

                                                        return new AuthorizationDecision(permitido);
                                                })

                                                // ==================================
                                                // GESTIÓN DE USUARIOS
                                                // SOLO ADMIN ACTIVO Y HABILITADO
                                                // ==================================

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/usuarios/mi-perfil")
                                                .authenticated()

                                                .requestMatchers(
                                                                "/api/usuarios",
                                                                "/api/usuarios/**")
                                                .access((authentication, context) -> {

                                                        var autenticacion = authentication.get();

                                                        boolean permitido = autenticacion
                                                                        .getPrincipal() instanceof Jwt jwt
                                                                        && administradorValido(
                                                                                        jwt,
                                                                                        jdbcTemplate);

                                                        return new AuthorizationDecision(permitido);
                                                })

                                                // ==================================
                                                // OTRAS RUTAS
                                                // ==================================

                                                .anyRequest().denyAll())

                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(
                                                jwtConverter)));

                return http.build();
        }

        // ==========================================
        // CORS PARA REACT LOCAL
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
