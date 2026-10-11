package com.example.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.usuario.entity.Cliente;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>{

}
