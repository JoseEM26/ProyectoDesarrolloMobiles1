package com.computronica.webapp.service;

import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class FirestoreService {

    private final Firestore db;

    // Spring inyectará FirebaseApp automáticamente
    public FirestoreService(FirebaseApp firebaseApp) {
        this.db = FirestoreClient.getFirestore(firebaseApp);
    }

    // CREATE
    public <T> String create(String collection, T object) throws ExecutionException, InterruptedException {
        DocumentReference docRef = db.collection(collection).document();
        WriteResult result = docRef.set(object).get();
        log.info("Created document in {} with ID: {}", collection, docRef.getId());
        return docRef.getId();
    }

    // READ BY ID
    public <T> T getById(String collection, String id, Class<T> clazz) throws ExecutionException, InterruptedException {
        return db.collection(collection).document(id).get().get().toObject(clazz);
    }

    // READ ALL
    public <T> List<T> getAll(String collection, Class<T> clazz) throws ExecutionException, InterruptedException {
        return db.collection(collection).get().get().toObjects(clazz);
    }

    // UPDATE
    public <T> void update(String collection, String id, T object) throws ExecutionException, InterruptedException {
        db.collection(collection).document(id).set(object).get();
        log.info("Updated document in {} with ID: {}", collection, id);
    }

    // DELETE
    public void delete(String collection, String id) throws ExecutionException, InterruptedException {
        db.collection(collection).document(id).delete().get();
        log.info("Deleted document in {} with ID: {}", collection, id);
    }

    // FILTRAR
    public <T> List<T> filterByField(String collection, String field, Object value, Class<T> clazz) throws ExecutionException, InterruptedException {
        return db.collection(collection).whereEqualTo(field, value).get().get().toObjects(clazz);
    }
}