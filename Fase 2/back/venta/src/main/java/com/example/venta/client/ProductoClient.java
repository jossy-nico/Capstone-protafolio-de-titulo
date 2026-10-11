package com.example.venta.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ProductoClient {

     private final RestTemplate restTemplate;

    @Value("${producto.api.base-url}")
    private String baseUrl;

    public ProductoClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    //Descontar stock (gramos)
    public void descontarStock(Integer productoId, Integer gramos) {
        try {
            String url = baseUrl + "/api/productos/" + productoId
                    + "/stock/descontar?gramos=" + gramos;

            restTemplate.postForEntity(url, null, String.class);

        } catch (Exception e) {
            throw new RuntimeException(
                "Error al descontar stock del producto " + productoId, e
            );
        }
    }


    //Sumar stock (compensación)
    public void sumarStock(Integer productoId, Integer gramos) {
        String url = baseUrl
                + "/api/productos/"
                + productoId
                + "/stock/sumar?gramos="
                + gramos;

        restTemplate.postForEntity(url, null, String.class);
    }
}
