package com.computronica.webapp.service;

import com.computronica.webapp.model.Usuario;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
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
            Query query = db.collection(COLLECTION_NAME)
                    .whereEqualTo("correoInstitucional", correo)
                    .limit(1);

            QuerySnapshot snapshot = query.get().get();
            if (!snapshot.isEmpty()) {
                DocumentSnapshot doc = snapshot.getDocuments().get(0);
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

            db.collection(COLLECTION_NAME)
                    .document(usuario.getId())
                    .set(usuario)
                    .get();

        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error al guardar usuario: " + e.getMessage(), e);
        }
    }

    // ====================
    // INHABILITAR / HABILITAR
    // ====================
    public void inhabilitarUsuario(String id) {
        cambiarEstadoUsuario(id, false);
    }

    public void habilitarUsuario(String id) {
        cambiarEstadoUsuario(id, true);
    }

    public Usuario toggleEstadoUsuario(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }

        try {
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(id);
            DocumentSnapshot doc = docRef.get().get();

            if (!doc.exists()) {
                throw new IllegalArgumentException("Usuario no encontrado con ID: " + id);
            }

            boolean currentEstado = doc.getBoolean("estado") != null ? doc.getBoolean("estado") : true;
            boolean nuevoEstado = !currentEstado;
            Timestamp now = Timestamp.now();

            Map<String, Object> updates = new HashMap<>();
            updates.put("estado", nuevoEstado);
            updates.put("updatedAt", now);

            docRef.update(updates).get();

            // Devolver usuario actualizado
            Usuario usuario = doc.toObject(Usuario.class);
            usuario.setId(doc.getId());
            usuario.setEstado(nuevoEstado);
            usuario.setUpdatedAt(now);

            return usuario;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error al cambiar estado: " + e.getMessage(), e);
        }
    }

    private void cambiarEstadoUsuario(String id, boolean nuevoEstado) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }

        try {
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(id);
            DocumentSnapshot doc = docRef.get().get();

            if (!doc.exists()) {
                throw new IllegalArgumentException("Usuario no encontrado con ID: " + id);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("estado", nuevoEstado);
            updates.put("updatedAt", Timestamp.now());

            docRef.update(updates).get();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Operación interrumpida", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error al actualizar estado: " + e.getMessage(), e);
        }
    }
}