package com.example.resenna.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VentaClient {

    private final RestClient restClient;

    public VentaClient() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:8383/api/ventas")
                .build();
    }

    public boolean clienteComproProductoEnVenta(
            Long clienteId,
            Long ventaId,
            Long productoId
    ) {
        if (clienteId == null || ventaId == null || productoId == null) {
            return false;
        }

        try {
            Boolean respuesta = restClient.get()
                    .uri(
                            "/{ventaId}/cliente/{clienteId}/producto/{productoId}/existe",
                            ventaId,
                            clienteId,
                            productoId
                    )
                    .retrieve()
                    .body(Boolean.class);

            return Boolean.TRUE.equals(respuesta);

        } catch (RestClientException ex) {
            System.out.println(
                    "No fue posible validar la compra: "
                            + ex.getMessage()
            );

            return false;
        }
    }
}
