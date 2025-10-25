// src/main/java/com/computronica/webapp/service/FirestoreService.java
package com.computronica.webapp.service;

import com.computronica.webapp.model.Asignatura;
import com.computronica.webapp.model.Calificaciones;
import com.computronica.webapp.model.Tema;
import com.computronica.webapp.model.Usuario;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    public <T> T getById(String collection, String id, Class<T> clazz) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = db().collection(collection).document(id).get().get();
        if (doc.exists()) {
            T obj = doc.toObject(clazz);
            if (obj instanceof Usuario) {
                ((Usuario) obj).setId(id);
            } else if (obj instanceof Asignatura) {
                ((Asignatura) obj).setId(id);
            } else if (obj instanceof Tema) {
                ((Tema) obj).setId(id);
            } else if (obj instanceof Calificaciones) {
                ((Calificaciones) obj).setId(id);
            }
            return obj;
        }
        return null;
    }

    public <T> List<T> getAll(String collection, Class<T> clazz) throws ExecutionException, InterruptedException {
        List<T> list = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db().collection(collection).get();
        for (DocumentSnapshot doc : future.get().getDocuments()) {
            T obj = doc.toObject(clazz);
            if (obj instanceof Usuario) ((Usuario) obj).setId(doc.getId());
            else if (obj instanceof Asignatura) ((Asignatura) obj).setId(doc.getId());
            else if (obj instanceof Tema) ((Tema) obj).setId(doc.getId());
            else if (obj instanceof Calificaciones) ((Calificaciones) obj).setId(doc.getId());
            list.add(obj);
        }
        return list;
    }
}