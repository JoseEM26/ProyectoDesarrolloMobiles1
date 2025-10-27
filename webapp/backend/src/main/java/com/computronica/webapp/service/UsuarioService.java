// src/main/java/com/computronica/webapp/service/UsuarioService.java
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

    /**
     * Busca un usuario por su correo institucional en Firestore.
     *
     * @param correo Correo institucional del usuario.
     * @return Usuario encontrado o null si no existe.
     * @throws RuntimeException si ocurre un error al consultar Firestore.
     */
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
                DocumentSnapshot document = query.getDocuments().get(0);
                Usuario usuario = document.toObject(Usuario.class);
                usuario.setId(document.getId());
                return usuario;
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error al buscar usuario por correo: " + e.getMessage(), e);
        }
    }

    /**
     * Busca un usuario por su ID (UID de Firebase Auth) en Firestore.
     *
     * @param id ID del usuario (UID).
     * @return Usuario encontrado o null si no existe.
     * @throws RuntimeException si ocurre un error al consultar Firestore.
     */
    public Usuario findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }

        try {
            DocumentSnapshot document = db.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get();

            if (document.exists()) {
                Usuario usuario = document.toObject(Usuario.class);
                usuario.setId(document.getId());
                return usuario;
            }
            return null;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error al buscar usuario por ID: " + e.getMessage(), e);
        }
    }

    /**
     * Guarda un usuario en Firestore, actualizando los timestamps y asegurando compatibilidad.
     *
     * @param usuario Objeto Usuario a guardar.
     * @throws IllegalArgumentException si el usuario tiene datos inválidos.
     * @throws RuntimeException si ocurre un error al guardar en Firestore.
     */
    public void save(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo");
        }
        if (usuario.getCorreoInstitucional() == null || usuario.getCorreoInstitucional().trim().isEmpty()) {
            throw new IllegalArgumentException("El correo institucional es obligatorio");
        }
        if (usuario.getNombre() == null || usuario.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (usuario.getApellido() == null || usuario.getApellido().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }

        try {
            Timestamp now = Timestamp.now();
            if (usuario.getCreatedAt() == null) {
                usuario.setCreatedAt(now);
            }
            usuario.setUpdatedAt(now);

            // NO GUARDAR CONTRASEÑA (manteniendo tu lógica actual)
            usuario.setContrasena(null);

            String id = usuario.getId();
            if (id == null || id.trim().isEmpty()) {
                id = db.collection(COLLECTION_NAME).document().getId();
                usuario.setId(id);
            }

            db.collection(COLLECTION_NAME).document(id).set(usuario).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error al guardar usuario: " + e.getMessage(), e);
        }
    }
}