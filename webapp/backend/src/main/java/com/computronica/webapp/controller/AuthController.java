// src/main/java/com/computronica/webapp/controller/AuthController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.dto.AuthResponse;
import com.computronica.webapp.dto.LoginRequest;
import com.computronica.webapp.dto.RegisterRequest;
import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.service.UsuarioService;
import com.google.cloud.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    @Autowired
    private FirebaseAuth firebaseAuth;

    @Autowired
    private UsuarioService usuarioService;

    // ====================
    // 1. REGISTRO
    // ====================
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@Valid @RequestBody RegisterRequest request) {
        try {
            // 1. VERIFICAR SI EL CORREO YA EXISTE
            try {
                firebaseAuth.getUserByEmail(request.getCorreoInstitucional());
                return ResponseEntity.badRequest().body("El correo ya está registrado");
            } catch (FirebaseAuthException e) {

            }

            // 2. CREAR EN FIREBASE AUTH
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getCorreoInstitucional())
                    .setPassword(request.getContrasena())
                    .setDisplayName(request.getNombre() + " " + request.getApellido());

            UserRecord user = firebaseAuth.createUser(createRequest);

            // 3. CREAR EN FIRESTORE
            Usuario usuario = new Usuario();
            usuario.setId(user.getUid());
            usuario.setCodigoInstitucional(request.getCodigoInstitucional());
            usuario.setSede(request.getSede());
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setCorreoInstitucional(request.getCorreoInstitucional());
            usuario.setContrasena(request.getContrasena());

            // TIPO COMO STRING
            String tipo = request.getTipo();
            if (tipo == null || tipo.trim().isEmpty()) {
                tipo = "estudiante";
            } else {
                tipo = tipo.trim().toLowerCase();
                if (!tipo.matches("^(estudiante|profesor|administrativo)$")) {
                    tipo = "estudiante"; // fallback
                }
            }
            usuario.setTipo(tipo);

            usuario.setEstado(true);
            usuario.setCreatedAt(Timestamp.now());
            usuario.setUpdatedAt(Timestamp.now());

            usuarioService.save(usuario);

            // 4. GENERAR CUSTOM TOKEN
            String customToken = firebaseAuth.createCustomToken(user.getUid());
            return ResponseEntity.ok(buildResponse(usuario, customToken));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body("Error en Firebase: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno del servidor: " + e.getMessage());
        }
    }

    // ====================
    // 2. LOGIN
    // ====================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(request.getIdToken());
            String uid = decodedToken.getUid();

            Usuario usuario = usuarioService.findById(uid);
            if (usuario == null) {
                return ResponseEntity.status(404).body("Usuario no encontrado en sistema");
            }
            if (!usuario.isEstado()) {
                return ResponseEntity.status(403).body("Cuenta desactivada");
            }

            return ResponseEntity.ok(buildResponse(usuario, request.getIdToken()));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Token inválido: " + e.getMessage());
        }
    }

    // ====================
    // 3. LOGOUT
    // ====================
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok().headers(headers).body("Sesión cerrada exitosamente");
        }

        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();
            firebaseAuth.revokeRefreshTokens(uid);
            return ResponseEntity.ok().headers(headers).body("Sesión cerrada exitosamente");
        } catch (FirebaseAuthException e) {
            System.err.println("Error en logout: " + e.getMessage());
            return ResponseEntity.ok().headers(headers).body("Sesión cerrada exitosamente");
        }
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
        res.setTipo(usuario.getTipo());  // ← String
        res.setEstado(usuario.isEstado());
        res.setToken(token);
        return res;
    }
}