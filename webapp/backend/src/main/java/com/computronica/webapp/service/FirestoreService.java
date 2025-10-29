// src/main/java/com/computronica/webapp/service/FirestoreService.java


package com.computronica.webapp.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.cloud.firestore.SetOptions;

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

    // CREATE: devuelve ID y lo asigna al objeto
    public <T> String create(String collection, T entity) throws Exception {
        setDefaultStringDates(entity);
        ApiFuture<DocumentReference> future = firestore.collection(collection).add(entity);
        DocumentReference docRef = future.get();
        String id = docRef.getId();

        // Asignar ID al objeto usando reflexión
        assignIdToEntity(entity, id);

        return id;
    }

    // UPDATE: usa merge para no borrar campos
    public <T> void update(String collection, String id, T entity) throws Exception {
        setDefaultStringDates(entity);
        assignIdToEntity(entity, id); // Asegura que el ID esté en el objeto

        firestore.collection(collection)
                .document(id)
                .set(entity, SetOptions.merge()) // merge() → no sobrescribe todo
                .get();
    }

    // READ BY ID: asigna ID al objeto
    public <T> T getById(String collection, String id, Class<T> clazz) throws Exception {
        DocumentSnapshot snapshot = firestore.collection(collection).document(id).get().get();
        System.out.println("Firestore document: " + snapshot.getData());

        if (!snapshot.exists()) return null;

        T entity = snapshot.toObject(clazz);
        assignIdToEntity(entity, id); // Asigna ID
        return entity;
    }

    // READ ALL: asigna ID a cada objeto
    public <T> List<T> getAll(String collection, Class<T> clazz) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection(collection).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        documents.forEach(doc -> System.out.println("Firestore document: " + doc.getData()));

        return documents.stream().map(doc -> {
            T entity = doc.toObject(clazz);
            assignIdToEntity(entity, doc.getId());
            return entity;
        }).toList();
    }

    // FILTER: asigna ID
    public <T> List<T> filterByField(String collection, String field, String value, Class<T> clazz) throws Exception {
        ApiFuture<QuerySnapshot> future = firestore.collection(collection).whereEqualTo(field, value).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        documents.forEach(doc -> System.out.println("Firestore document: " + doc.getData()));

        return documents.stream().map(doc -> {
            T entity = doc.toObject(clazz);
            assignIdToEntity(entity, doc.getId());
            return entity;
        }).toList();
    }

    // DELETE: ya está bien

    public void delete(String collection, String id) throws Exception {
        firestore.collection(collection).document(id).delete().get();
    }

    // === UTILIDADES (no tocar) ===

    private <T> void setDefaultStringDates(T entity) throws Exception {
        if (entity == null) return;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = dateFormat.format(new Date());

        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().equals(String.class) && field.getName().equals("fechaRegistro")) {
                field.setAccessible(true);
                if (field.get(entity) == null) {
                    System.out.println("Setting fechaRegistro to " + currentDate + " for entity: " + entity);

                    field.set(entity, currentDate);
                }
            }
        }
    }

    // NUEVO: asigna ID al objeto usando reflexión
    private <T> void assignIdToEntity(T entity, String id) {
        if (entity == null || id == null) return;
        try {
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (NoSuchFieldException e) {
            // El modelo no tiene campo 'id' → ignorar
        } catch (Exception e) {
            System.err.println("Error asignando ID: " + e.getMessage());
        }
    }

    // fixFechaField → no tocar, está bien

    public void fixFechaField(String collection) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String currentDate = dateFormat.format(new Date());

        List<QueryDocumentSnapshot> docs = firestore.collection(collection).get().get().getDocuments();
        for (QueryDocumentSnapshot doc : docs) {

            if (doc.contains("fecha")) {
                Object fecha = doc.get("fecha");
                String fechaString = fecha instanceof Timestamp
                        ? dateFormat.format(((Timestamp) fecha).toDate())
                        : fecha.toString();
                doc.getReference().update("fechaRegistro", fechaString).get();
                doc.getReference().update("fecha", FieldValue.delete()).get();
            }

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
                        dateFormat.parse(fechaString);

                    } catch (Exception e) {
                        System.out.println("Invalid fechaRegistro format for doc " + doc.getId() + ": " + fechaString);
                        doc.getReference().update("fechaRegistro", currentDate).get();
                    }
                }
            } else {
                doc.getReference().update("fechaRegistro", currentDate).get();
            }
        }
        System.out.println("Fixed fecha fields in collection: " + collection);

    }
}