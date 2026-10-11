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

import com.example.usuario.dto.ClienteResponse;
import com.example.usuario.entity.Cliente;
import com.example.usuario.service.ClienteService;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<Cliente>> listar(){
        List<Cliente> clientes = clienteService.findAll();
        if (clientes.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<>(clientes, HttpStatus.OK);
    }

    @GetMapping("/{usuarioId}")
    public ResponseEntity<Cliente> buscar(@PathVariable("usuarioId") Long usuarioId){
        return clienteService.findById(usuarioId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
    }

   @PostMapping
    public ResponseEntity<?> crear(@RequestBody Cliente cliente) {
        try {
            Cliente nuevoCliente = clienteService.create(cliente);

            ClienteResponse response = new ClienteResponse(
                    nuevoCliente.getUsuarioId(),
                    nuevoCliente.getUsuario().getNombre(),
                    nuevoCliente.getUsuario().getApellido(),
                    nuevoCliente.getUsuario().getCorreo(),
                    nuevoCliente.getTelefono(),
                    nuevoCliente.getDireccion()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ex.getMessage());
        }
    }

    @PutMapping("/{usuarioId}")
    public ResponseEntity<Cliente> actualizar(@PathVariable("usuarioId") Long usuarioId, @RequestBody Cliente cliente){
        try {
            Cliente actualizado = clienteService.update(usuarioId, cliente);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<?> eliminar(@PathVariable("usuarioId") Long usuarioId){
        try {
            clienteService.deleteById(usuarioId);
            return ResponseEntity.ok("cliente eliminado correctamente");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }

}
