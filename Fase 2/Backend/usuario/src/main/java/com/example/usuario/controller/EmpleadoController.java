package com.example.usuario.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.usuario.dto.EmpleadoCreateRequest;
import com.example.usuario.dto.EmpleadoResponse;
import com.example.usuario.entity.Empleado;
import com.example.usuario.service.EmpleadoService;

@RestController
@RequestMapping("/api/empleados")
@CrossOrigin(origins = "*")
public class EmpleadoController {

    private final EmpleadoService empleadoService;

    public EmpleadoController(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;
    }

    // LISTAR TODOS LOS EMPLEADOS
    @GetMapping
    public ResponseEntity<List<EmpleadoResponse>> listar() {

        List<EmpleadoResponse> empleados = empleadoService
                .findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();

        return ResponseEntity.ok(empleados);
    }

    // BUSCAR EMPLEADO POR ID
    @GetMapping("/{usuarioId}")
    public ResponseEntity<EmpleadoResponse> buscar(
            @PathVariable("usuarioId") Long usuarioId) {

        return empleadoService
                .findById(usuarioId)
                .map(this::convertirAResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREACIÓN ANTIGUA
    // Se mantiene temporalmente mientras terminamos la migración.
    @PostMapping
    public ResponseEntity<?> crear(
            @RequestBody Empleado empleado) {

        try {

            Empleado nuevoEmpleado = empleadoService.create(empleado);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(convertirAResponse(nuevoEmpleado));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    // CREAR USUARIO + EMPLEADO EN UNA SOLA OPERACIÓN
    @PostMapping("/completo")
    public ResponseEntity<?> crearCompleto(
            @RequestBody EmpleadoCreateRequest request) {

        try {

            Empleado nuevoEmpleado = empleadoService.crearCompleto(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(convertirAResponse(nuevoEmpleado));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    // ACTUALIZAR DATOS LABORALES
    @PutMapping("/{usuarioId}")
    public ResponseEntity<?> actualizar(
            @PathVariable("usuarioId") Long usuarioId,
            @RequestBody Empleado empleado) {

        try {

            Empleado actualizado = empleadoService.update(usuarioId, empleado);

            return ResponseEntity.ok(
                    convertirAResponse(actualizado));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());
        }
    }

    // ELIMINACIÓN DE FICHA DE EMPLEADO
    // Se mantiene por ahora; para suspensión usaremos Usuario.
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<?> eliminar(
            @PathVariable("usuarioId") Long usuarioId) {

        try {

            empleadoService.deleteById(usuarioId);

            return ResponseEntity.ok(
                    "Empleado eliminado correctamente");

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());
        }
    }

    // CONVERSIÓN SEGURA: NUNCA EXPONE PASSWORD
    private EmpleadoResponse convertirAResponse(
            Empleado empleado) {

        return new EmpleadoResponse(
                empleado.getUsuarioId(),
                empleado.getUsuario().getNombre(),
                empleado.getUsuario().getApellido(),
                empleado.getUsuario().getCorreo(),
                empleado.getUsuario().getTipoUsuario(),
                empleado.getUsuario().getEstado(),
                empleado.getUsuario().getDebeCambiarPassword(),
                empleado.getPuesto(),
                empleado.getFechaContrato(),
                empleado.getSueldo());
    }
}