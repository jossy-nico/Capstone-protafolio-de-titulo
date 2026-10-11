package com.example.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.usuario.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long>{
    Optional<Usuario> findByCorreo(String correo);

}
