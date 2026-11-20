
package com.example.usuario.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class JwtConfig {

    // ==========================================
    // CLAVE PRIVADA RSA
    // ==========================================

    @Bean
    public RSAPrivateKey jwtPrivateKey(
            @Value("${app.jwt.private-key-path}") String ruta) throws Exception {

        String pem = Files.readString(
                Path.of(ruta),
                StandardCharsets.UTF_8);

        String contenido = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] bytes = Base64.getDecoder()
                .decode(contenido);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);

        return (RSAPrivateKey) KeyFactory
                .getInstance("RSA")
                .generatePrivate(spec);
    }

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
    // FIRMAR JWT
    // ==========================================

    @Bean
    public JwtEncoder jwtEncoder(
            RSAPublicKey publicKey,
            RSAPrivateKey privateKey) {

        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);

        return new NimbusJwtEncoder(jwkSource);
    }

    // ==========================================
    // VALIDAR JWT
    // ==========================================

    @Bean
    public JwtDecoder jwtDecoder(
            RSAPublicKey publicKey) {

        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
    }
}
