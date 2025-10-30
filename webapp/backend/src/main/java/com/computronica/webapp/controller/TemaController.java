// src/main/java/com/computronica/webapp/controller/TemaController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.model.Tema;
import com.computronica.webapp.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/temas")
public class TemaController {

    @Autowired
    private FirestoreService firestoreService;

    // CREATE → Devuelve JSON con { id: "..." }
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> create(@RequestBody Tema tema) throws Exception {
        String id = firestoreService.create("temas", tema);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // READ BY ID
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Tema> getById(@PathVariable String id) throws Exception {
        Tema tema = firestoreService.getById("temas", id, Tema.class);
        return ResponseEntity.ok(tema);
    }

    // READ ALL
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Tema>> getAll() throws Exception {
        List<Tema> temas = firestoreService.getAll("temas", Tema.class);
        return ResponseEntity.ok(temas);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates  // ← CAMBIA A MAP
    ) throws Exception {
        firestoreService.update("temas", id, updates);  // ← Pasa el map directamente
        return ResponseEntity.ok().build();
    }

    // DELETE
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        firestoreService.delete("temas", id);
        return ResponseEntity.ok().build();
    }

    // FILTRAR POR ASIGNATURA
    @GetMapping(value = "/asignatura/{asignaturaId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Tema>> filterByAsignatura(@PathVariable String asignaturaId) throws Exception {
        List<Tema> temas = firestoreService.filterByField("temas", "asignaturaId", asignaturaId, Tema.class);
        return ResponseEntity.ok(temas);
    }
}