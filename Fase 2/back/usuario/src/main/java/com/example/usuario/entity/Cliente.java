package com.example.usuario.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(name = "clientes")
public class Cliente {
    @Id
    private Long usuarioId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "usuarioId")
    private Usuario usuario;

    @Column(nullable = false, length = 9)
    private String telefono;

    @Column(nullable = false, length = 200)
    private String direccion;

}
