package com.example.usuario.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.example.usuario.dto.LoginRequest;
import com.example.usuario.dto.UsuarioResponse;
import com.example.usuario.dto.UsuarioResumenResponse;
import com.example.usuario.entity.Usuario;
import com.example.usuario.service.UsuarioService;
import com.example.usuario.enums.TipoUsuario;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {
    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {

        List<UsuarioResponse> usuarios = usuarioService.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();

        if (usuarios.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse> buscar(
            @PathVariable("idUsuario") Long idUsuario
    ) {
        return usuarioService.findById(idUsuario)
                .map(this::convertirAResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {

        if (usuario.getTipoUsuario() == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("El tipo de usuario es obligatorio");
        }

        if (usuario.getTipoUsuario() == TipoUsuario.EMPLEADO) {

            String correo = usuario.getCorreo();

            if (correo == null ||
                    !correo.trim()
                            .toLowerCase()
                            .endsWith("@frutossecos.cl")) {

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(
                            "El correo de un empleado debe terminar en @frutossecos.cl"
                        );
            }
        }

        try {
            Usuario nuevoUsuario = usuarioService.create(usuario);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(convertirAResponse(nuevoUsuario));

        } catch (IllegalArgumentException ex) {

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    @PutMapping("/{idUsuario}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable("idUsuario") Long idUsuario, @RequestBody Usuario usuario){
        try{
            Usuario actualizado = usuarioService.update(idUsuario, usuario);
            return ResponseEntity.ok(convertirAResponse(actualizado));
        }catch(IllegalArgumentException ex){
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<?> eliminar(@PathVariable("idUsuario") Long idUsuario){
        try {
            usuarioService.deleteById(idUsuario);
            return ResponseEntity.ok("Usuario eliminado correctamente");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

    //post para login por correo y password
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(
            @RequestBody LoginRequest request
    ) {
        String correo =
                request.getCorreo().trim().toLowerCase();

        String password =
                request.getPassword();

        return usuarioService.findByCorreo(correo)
                .filter(u -> u.getPassword().equals(password))
                .map(this::convertirAResponse)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .status(HttpStatus.UNAUTHORIZED)
                                .build()
                );
    }

    @GetMapping("/{idUsuario}/resumen")
    public ResponseEntity<UsuarioResumenResponse> obtenerResumen(
            @PathVariable("idUsuario") Long idUsuario
    ) {
        return usuarioService.findById(idUsuario)
                .map(usuario ->
                        ResponseEntity.ok(
                                new UsuarioResumenResponse(
                                        usuario.getIdUsuario(),
                                        usuario.getNombre()
                                )
                        )
                )
                .orElse(ResponseEntity.notFound().build());
    }
    //método privado, no entrega información sensible de usuario
    private UsuarioResponse convertirAResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getCorreo(),
                usuario.getTipoUsuario()
        );
    }
}
