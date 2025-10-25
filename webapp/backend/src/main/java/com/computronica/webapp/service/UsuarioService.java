// src/main/java/com/computronica/webapp/service/UsuarioService.java
package com.computronica.webapp.service;

import com.computronica.webapp.model.Usuario;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UsuarioService {

    private final Firestore db;

    public UsuarioService(FirebaseApp firebaseApp) {
        this.db = FirestoreClient.getFirestore(firebaseApp);
    }

    public Usuario findByCorreoInstitucional(String correo) {
        try {
            var query = db.collection("usuarios")
                    .whereEqualTo("correoInstitucional", correo)
                    .limit(1)
                    .get()
                    .get();

            if (!query.isEmpty()) {
                Usuario usuario = query.getDocuments().get(0).toObject(Usuario.class);
                usuario.setId(query.getDocuments().get(0).getId());
                return usuario;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void save(Usuario usuario) {
        try {
            Timestamp now = Timestamp.now();
            if (usuario.getCreatedAt() == null) {
                usuario.setCreatedAt(now);
            }
            usuario.setUpdatedAt(now);

            // NO GUARDAR CONTRASEÑA
            usuario.setContrasena(null);

            String id = usuario.getId();
            if (id == null || id.isEmpty()) {
                id = db.collection("usuarios").document().getId();
                usuario.setId(id);
            }

            db.collection("usuarios").document(id).set(usuario).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}