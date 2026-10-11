package com.example.usuario.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.usuario.entity.Usuario;
import com.example.usuario.enums.TipoUsuario;
import com.example.usuario.repository.UsuarioRepository;
import com.example.usuario.service.UsuarioService;


@Service
@Transactional
public class UsuarioServiceImpl implements UsuarioService{
    private final UsuarioRepository usuarioRepository;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> findAll(){
        return usuarioRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> findById(Long idUsuario){
        return usuarioRepository.findById(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> findByCorreo(String correo){
        return usuarioRepository.findByCorreo(correo);
    }

    @Override
    public Usuario create(Usuario u){

        // 1. Validar que venga un usuario
        if (u == null) {
            throw new IllegalArgumentException(
                    "El usuario no puede ser nulo"
            );
        }

        // 2. Validar que tenga tipo de usuario
        if (u.getTipoUsuario() == null) {
            throw new IllegalArgumentException(
                    "El tipo de usuario es obligatorio"
            );
        }

        // 3. Validar formato y dominio del correo
        validarCorreo(
                u.getCorreo(),
                u.getTipoUsuario()
        );

        // 4. Normalizar correo
        String correoNormalizado =
                normalizarCorreo(u.getCorreo());

        u.setCorreo(correoNormalizado);

        // 5. Verificar que el correo no exista
        if (usuarioRepository
                .findByCorreo(correoNormalizado)
                .isPresent()) {

            throw new IllegalArgumentException(
                    "Ya existe un usuario con este correo"
            );
        }

        // 6. Guardar
        return usuarioRepository.save(u);
    }

    @Override
    public Usuario update(Long idUsuario, Usuario u){
        Usuario existe = usuarioRepository.findById(idUsuario)
        .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        existe.setNombre(u.getNombre());
        existe.setApellido(u.getApellido());
        existe.setCorreo((u.getCorreo()));
        existe.setPassword(u.getPassword());
        existe.setTipoUsuario(u.getTipoUsuario());

        return usuarioRepository.save(existe);
    }

    @Override
    public void deleteById(Long idUsuario){
        if (!usuarioRepository.existsById(idUsuario)) {
            throw new IllegalArgumentException("Usuario no encontrado con ID: "+ idUsuario);
        }
        usuarioRepository.deleteById(idUsuario);
    }
    //normalizaicón de correo
    private String normalizarCorreo(String correo) {
        if (correo == null) {
            return null;
        }

        return correo.trim().toLowerCase();
    }
    //Validación de correo
    private void validarCorreo(String correo, TipoUsuario tipoUsuario) {

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo es obligatorio"
            );
        }

        String correoLimpio = normalizarCorreo(correo);

        String regex =
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if (!correoLimpio.matches(regex)) {
            throw new IllegalArgumentException(
                    "El correo no tiene un formato válido"
            );
        }

        if (tipoUsuario == TipoUsuario.EMPLEADO &&
                !correoLimpio.endsWith("@frutossecos.cl")) {

            throw new IllegalArgumentException(
                    "El correo de un empleado debe terminar en @frutossecos.cl"
            );
        }

        validarDominioConocido(correoLimpio);
    
    }
    private void validarDominioConocido(String correo) {

        String dominio = correo.substring(
                correo.indexOf("@") + 1
        );

        switch (dominio) {

            case "gmail.co":
                throw new IllegalArgumentException(
                        "El dominio gmail.co no es válido. ¿Quiso escribir gmail.com?"
                );

            case "hotmail.co":
                throw new IllegalArgumentException(
                        "El dominio hotmail.co no es válido. ¿Quiso escribir hotmail.com?"
                );

            case "outlook.co":
                throw new IllegalArgumentException(
                        "El dominio outlook.co no es válido. ¿Quiso escribir outlook.com?"
                );
        }
    }
    
}
