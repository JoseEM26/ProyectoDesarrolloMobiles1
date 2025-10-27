package com.computronica.webapp.controller;

import com.computronica.webapp.dto.AuthResponse;
import com.computronica.webapp.dto.LoginRequest;
import com.computronica.webapp.dto.RegisterRequest;
import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.model.TipoUsuario;
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
            // Validar si el correo ya existe en Firebase Auth
            try {
                firebaseAuth.getUserByEmail(request.getCorreoInstitucional());
                return ResponseEntity.badRequest().body("El correo ya está registrado");
            } catch (FirebaseAuthException e) {
                if (!e.getErrorCode().equals("user-not-found")) {
                    throw e; // Re-lanzar si no es "user-not-found"
                }
            }

            // Crear usuario en Firebase Auth
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getCorreoInstitucional())
                    .setPassword(request.getContrasena())
                    .setDisplayName(request.getNombre() + " " + request.getApellido());

            UserRecord user = firebaseAuth.createUser(createRequest);

            // Crear usuario en Firestore
            Usuario usuario = new Usuario();
            usuario.setId(user.getUid());
            usuario.setCodigoInstitucional(request.getCodigoInstitucional());
            usuario.setSede(request.getSede());
            usuario.setNombre(request.getNombre());
            usuario.setApellido(request.getApellido());
            usuario.setCorreoInstitucional(request.getCorreoInstitucional());
            usuario.setContrasena(request.getContrasena());
            usuario.setTipo(request.getTipo() != null ? request.getTipo() : TipoUsuario.estudiante);
            usuario.setEstado(true);
            usuario.setCreatedAt(Timestamp.now());
            usuario.setUpdatedAt(Timestamp.now());

            usuarioService.save(usuario);

            // Generar custom token
            String customToken = firebaseAuth.createCustomToken(user.getUid());

            return ResponseEntity.ok(buildResponse(usuario, customToken));

        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body("Error en registro: " + e.getMessage());
        }
    }

    // ====================
    // 2. LOGIN
    // ====================
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            // Verificar el ID token
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(request.getIdToken());
            String uid = decodedToken.getUid();

            // Buscar usuario en Firestore
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

        // Handle missing or invalid Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Sesión cerrada exitosamente");
        }

        try {
            // Extract and verify token
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken);
            String uid = decodedToken.getUid();

            // Revoke refresh tokens
            firebaseAuth.revokeRefreshTokens(uid);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Sesión cerrada exitosamente");

        } catch (FirebaseAuthException e) {
            // Log error but return success to ensure client can proceed
            System.err.println("Error verifying token during logout: " + e.getMessage());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body("Sesión cerrada exitosamente");
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
        res.setTipo(usuario.getTipo());
        res.setEstado(usuario.isEstado());
        res.setToken(token);
        return res;
    }
}