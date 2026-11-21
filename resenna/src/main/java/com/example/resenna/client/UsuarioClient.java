package com.example.resenna.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.example.resenna.dto.UsuarioResumenResponse;

@Component
public class UsuarioClient {

    private final RestClient restClient;

    public UsuarioClient(
            @Value("${usuario.api.base-url:http://localhost:8181/api/usuarios}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public String obtenerNombreUsuario(Long clienteId) {

        if (clienteId == null) {
            return "Cliente";
        }

        try {
            UsuarioResumenResponse usuario =
                    restClient.get()
                            .uri("/{idUsuario}/resumen", clienteId)
                            .retrieve()
                            .body(UsuarioResumenResponse.class);

            if (usuario == null
                    || usuario.getNombre() == null
                    || usuario.getNombre().isBlank()) {

                return "Cliente";
            }

            return usuario.getNombre().trim();

        } catch (RestClientException ex) {
            System.out.println(
                    "No fue posible obtener el nombre del usuario "
                    + clienteId
                    + ": "
                    + ex.getMessage()
            );

            return "Cliente";
        }
    }
}
