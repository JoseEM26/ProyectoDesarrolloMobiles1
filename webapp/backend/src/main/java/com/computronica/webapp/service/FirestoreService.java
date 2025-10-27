// src/main/java/com/computronica/webapp/service/FirestoreService.java
package com.computronica.webapp.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class FirestoreService {

    @Autowired
    private Firestore firestore;

    public <T> String create(String collection, T entity) throws Exception {
        setDefaultStringDates(entity);
        ApiFuture<DocumentReference> future = firestore.collection(collection).add(entity);
        return future.get().getId();
    }

    public <T> void update(String collection, String id, T entity) throws Exception {
        setDefaultStringDates(entity);
        firestore.collection(collection).document(id).set(entity).get();
    }

    public <T> T getById(String collection, String id, Class<T> clazz) throws Exception {
        DocumentSnapshot snapshot = firestore.collection(collection).document(id).get().get();
        System.out.println("Firestore document: " + snapshot.getData()); // Debug log
        return snapshot.exists() ? snapshot.toObject(clazz) : null;
    }

    public <T> List<T> getAll(String collection, Class<T> clazz) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection(collection).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        documents.forEach(doc -> System.out.println("Firestore document: " + doc.getData())); // Debug log
        return documents.stream().map(doc -> doc.toObject(clazz)).toList();
    }

    public <T> List<T> filterByField(String collection, String field, String value, Class<T> clazz) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection(collection).whereEqualTo(field, value).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        documents.forEach(doc -> System.out.println("Firestore document: " + doc.getData())); // Debug log
        return documents.stream().map(doc -> doc.toObject(clazz)).toList();
    }

    public void delete(String collection, String id) throws Exception {
        firestore.collection(collection).document(id).delete().get();
    }

    private <T> void setDefaultStringDates(T entity) throws Exception {
        if (entity == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = dateFormat.format(new Date());

        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().equals(String.class) && field.getName().equals("fechaRegistro")) {
                field.setAccessible(true);
                if (field.get(entity) == null) {
                    System.out.println("Setting fechaRegistro to " + currentDate + " for entity: " + entity); // Debug log
                    field.set(entity, currentDate);
                }
            }
        }
    }

    public void fixFechaField(String collection) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = dateFormat.format(new Date());

        List<QueryDocumentSnapshot> docs = firestore.collection(collection).get().get().getDocuments();
        for (QueryDocumentSnapshot doc : docs) {
            // Handle fecha -> fechaRegistro renaming
            if (doc.contains("fecha")) {
                Object fecha = doc.get("fecha");
                String fechaString = fecha instanceof Timestamp
                        ? dateFormat.format(((Timestamp) fecha).toDate())
                        : fecha.toString();
                doc.getReference().update("fechaRegistro", fechaString).get();
                doc.getReference().update("fecha", FieldValue.delete()).get();
            }
            // Ensure fechaRegistro is a string in dd/MM/yyyy format
            if (doc.contains("fechaRegistro")) {
                Object fechaRegistroObj = doc.get("fechaRegistro");
                if (!(fechaRegistroObj instanceof String)) {
                    String fechaString = fechaRegistroObj instanceof Timestamp
                            ? dateFormat.format(((Timestamp) fechaRegistroObj).toDate())
                            : fechaRegistroObj.toString();
                    doc.getReference().update("fechaRegistro", fechaString).get();
                } else {
                    String fechaString = (String) fechaRegistroObj;
                    try {
                        dateFormat.parse(fechaString); // Validate format
                    } catch (Exception e) {
                        System.out.println("Invalid fechaRegistro format for doc " + doc.getId() + ": " + fechaString);
                        doc.getReference().update("fechaRegistro", currentDate).get();
                    }
                }
            } else {
                doc.getReference().update("fechaRegistro", currentDate).get();
            }
        }
        System.out.println("Fixed fecha fields in collection: " + collection); // Debug log
    }
}