package com.example.usuario.dto;

import com.example.usuario.enums.TipoUsuario;
import com.example.usuario.enums.EstadoUsuario;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {

    private Long idUsuario;
    private String nombre;
    private String apellido;
    private String correo;
    private TipoUsuario tipoUsuario;
    private EstadoUsuario estado;
    private Boolean debeCambiarPassword;
}
