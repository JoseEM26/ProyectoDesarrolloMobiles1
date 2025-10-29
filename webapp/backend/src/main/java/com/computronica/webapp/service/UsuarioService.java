package com.computronica.webapp.service;

import com.computronica.webapp.model.Usuario;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UsuarioService {

    private static final String COLLECTION_NAME = "usuarios";
    private final Firestore db;

    public UsuarioService(FirebaseApp firebaseApp) {
        this.db = FirestoreClient.getFirestore(firebaseApp);
    }

    // ====================
    // BUSCAR POR CORREO
    // ====================

    public Usuario findByCorreoInstitucional(String correo) {
        if (correo == null || correo.trim().isEmpty()) {
            throw new IllegalArgumentException("El correo institucional no puede ser nulo o vacío");
        }

        try {
            var query = db.collection(COLLECTION_NAME)
                    .whereEqualTo("correoInstitucional", correo)
                    .limit(1)
                    .get()
                    .get();

            if (!query.isEmpty()) {
                DocumentSnapshot doc = query.getDocuments().get(0);
                Usuario usuario = doc.toObject(Usuario.class);
                usuario.setId(doc.getId());

                return usuario;
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al buscar por correo: " + e.getMessage(), e);
        }
    }

    // ====================
    // BUSCAR POR ID (UID)
    // ====================
    public Usuario findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo o vacío");
        }

        try {
            DocumentSnapshot doc = db.collection(COLLECTION_NAME)

                    .document(id)
                    .get()
                    .get();

            if (doc.exists()) {
                Usuario usuario = doc.toObject(Usuario.class);
                usuario.setId(doc.getId());

                return usuario;
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al buscar por ID: " + e.getMessage(), e);
        }
    }

    // ====================
    // GUARDAR / ACTUALIZAR
    // ====================

    public void save(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        if (usuario.getId() == null || usuario.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("El ID (UID) es obligatorio");
        }
        if (usuario.getCorreoInstitucional() == null || usuario.getCorreoInstitucional().trim().isEmpty()) {
            throw new IllegalArgumentException("El correo institucional es obligatorio");
        }


        try {
            Timestamp now = Timestamp.now();
            if (usuario.getCreatedAt() == null) {
                usuario.setCreatedAt(now);
            }
            usuario.setUpdatedAt(now);

            // NO BORRAR CONTRASEÑA → Firebase Auth ya la tiene
            // usuario.setContrasena(null); ← ELIMINAR ESTA LÍNEA

            String id = usuario.getId(); // ← USAR UID DE FIREBASE AUTH

            db.collection(COLLECTION_NAME)
                    .document(id)
                    .set(usuario)
                    .get();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException("Error al guardar usuario: " + e.getMessage(), e);
        }
    }
}