package com.example.usuario.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.usuario.entity.Empleado;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long>{

}
