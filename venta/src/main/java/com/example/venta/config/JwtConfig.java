
package com.example.venta.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    // ==========================================
    // CLAVE PÚBLICA RSA
    // ==========================================

    @Bean
    public RSAPublicKey jwtPublicKey(
            @Value("${app.jwt.public-key-path}") String ruta) throws Exception {

        String pem = Files.readString(
                Path.of(ruta),
                StandardCharsets.UTF_8);

        String contenido = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] bytes = Base64.getDecoder()
                .decode(contenido);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);

        return (RSAPublicKey) KeyFactory
                .getInstance("RSA")
                .generatePublic(spec);
    }

    // ==========================================
    // VALIDAR JWT EMITIDO POR USUARIO-API
    // ==========================================

    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.audience}") String audience) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();

        // Verifica vencimiento, fechas y emisor.
        OAuth2TokenValidator<Jwt> validadorBase = JwtValidators.createDefaultWithIssuer(issuer);

        // Verifica que el token esté destinado
        // a las API de Natur Cosecha.
        OAuth2TokenValidator<Jwt> validadorAudience = token -> {

            if (token.getAudience()
                    .contains(audience)) {

                return OAuth2TokenValidatorResult
                        .success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "El JWT no está destinado a esta API.",
                    null);

            return OAuth2TokenValidatorResult
                    .failure(error);
        };

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        validadorBase,
                        validadorAudience));

        return decoder;
    }
}
