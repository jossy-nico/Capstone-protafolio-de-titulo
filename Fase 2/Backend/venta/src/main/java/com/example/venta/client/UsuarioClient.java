package com.example.venta.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.venta.dto.UsuarioResponse;

@Service
public class UsuarioClient {

    private final RestTemplate restTemplate;

    @Value("${usuario.api.base-url}")
    private String baseUrl;

    public UsuarioClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UsuarioResponse obtenerUsuario(Long usuarioId) {

        if (usuarioId == null) {
            throw new IllegalArgumentException(
                    "usuarioId no puede ser nulo.");
        }

        try {

            String url = baseUrl
                    + "/api/usuarios/"
                    + usuarioId;

            UsuarioResponse usuario = restTemplate.getForObject(
                    url,
                    UsuarioResponse.class);

            if (usuario == null) {
                throw new RuntimeException(
                        "No se encontró el usuario "
                                + usuarioId);
            }

            return usuario;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Error al obtener el usuario "
                            + usuarioId,
                    e);
        }
    }
}