
package com.example.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    // Token firmado generado por usuario-api.
    private String token;

    // Tipo de autenticación.
    private String tokenType;

    // Duración del token en segundos.
    private Long expiresIn;

    // Información del usuario autenticado.
    private UsuarioResponse usuario;
}
