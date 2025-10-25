// src/main/java/com/computronica/webapp/controller/AuthController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.dto.AuthResponse;
import com.computronica.webapp.dto.LoginRequest;
import com.computronica.webapp.dto.RegisterRequest;
import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.model.TipoUsuario;
import com.computronica.webapp.service.UsuarioService;
import com.google.firebase.auth.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Autowired
    private UsuarioService usuarioService;

    // ====================
    // 1. REGISTRO
    // ====================
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody RegisterRequest request) {
        try {
            // Crear en Firebase Auth
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getCorreoInstitucional())
                    .setPassword(request.getContrasena())
                    .setDisplayName(request.getNombre() + " " + request.getApellido());

            UserRecord user = firebaseAuth.createUser(createRequest);

            // Crear en Firestore
            Usuario usuario = new Usuario();
            usuario.setId(user.getUid());
            usuario.setCodigoInstitucional(request.getCodigoInstitucional());
            usuario.setSede(request.getSede());
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setCorreoInstitucional(request.getCorreoInstitucional());
            usuario.setTipo(request.getTipo() != null ? request.getTipo() : TipoUsuario.estudiante);
            usuario.setEstado(true );

            usuarioService.save(usuario);

            return ResponseEntity.ok(buildResponse(usuario, "Registro exitoso (sin token)"));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ====================
    // 2. LOGIN (SIN TOKEN - SOLO EMAIL + PASS)
    // ====================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // 1. Buscar usuario por email
            UserRecord user = firebaseAuth.getUserByEmail(request.getCorreoInstitucional());

            // 2. Firebase Admin NO puede validar contraseña directamente
            // → SOLUCIÓN: Usar Custom Token (pero necesita frontend)
            // → OPCIÓN INSEGURA: Asumir que si existe → login OK (solo para pruebas)

            // BUSCAR EN FIRESTORE
            Usuario usuario = usuarioService.findByCorreoInstitucional(request.getCorreoInstitucional());
            if (usuario == null) {
                return ResponseEntity.status(404).body("Usuario no encontrado en sistema");
            }

            // GENERAR UN TOKEN SIMPLE (NO SEGURO)
            String fakeToken = "FAKE-TOKEN-" + user.getUid();

            return ResponseEntity.ok(buildResponse(usuario, fakeToken));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Credenciales inválidas");
        }
    }

    // ====================
    // 3. LOGOUT
    // ====================
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok("Sesión cerrada (simulada)");
    }

    // ====================
    // UTILIDAD
    // ====================
    private AuthResponse buildResponse(Usuario usuario, String token) {
        AuthResponse res = new AuthResponse();
        res.setId(usuario.getId());
        res.setCodigoInstitucional(usuario.getCodigoInstitucional());
        res.setSede(usuario.getSede());
        res.setNombre(usuario.getNombre());
        res.setApellido(usuario.getApellido());
        res.setCorreoInstitucional(usuario.getCorreoInstitucional());
        res.setTipo(usuario.getTipo());
        res.setEstado(usuario.isEstado());
        res.setToken(token); // Token falso o vacío
        return res;
    }
}