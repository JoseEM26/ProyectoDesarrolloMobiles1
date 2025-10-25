// src/main/java/com/computronica/webapp/service/UsuarioService.java
package com.computronica.webapp.service;

import com.computronica.webapp.model.Usuario;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
public class UsuarioService {

    public Usuario findByEmail(String email) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            QuerySnapshot query = db.collection("usuarios")
                    .whereEqualTo("email", email)
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
            Firestore db = FirestoreClient.getFirestore();
            String id = usuario.getId();
            if (id == null || id.isEmpty()) {
                id = usuario.getUid();
            }
            db.collection("usuarios").document(id).set(usuario).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}