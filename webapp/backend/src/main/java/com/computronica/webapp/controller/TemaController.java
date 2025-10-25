// src/main/java/com/computronica/webapp/controller/TemaController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.model.Tema;
import com.computronica.webapp.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/temas")
public class TemaController {

    @Autowired
    private FirestoreService firestoreService;

    // CREATE
    @PostMapping
    public ResponseEntity<String> create(@RequestBody Tema tema) throws Exception {
        String id = firestoreService.create("temas", tema);
        return ResponseEntity.ok(id);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Tema> getById(@PathVariable String id) throws Exception {
        Tema tema = firestoreService.getById("temas", id, Tema.class);
        return ResponseEntity.ok(tema);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Tema>> getAll() throws Exception {
        List<Tema> temas = firestoreService.getAll("temas", Tema.class);
        return ResponseEntity.ok(temas);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody Tema tema) throws Exception {
        firestoreService.update("temas", id, tema);
        return ResponseEntity.ok().build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        firestoreService.delete("temas", id);
        return ResponseEntity.ok().build();
    }

    // FILTRAR POR ASIGNATURA
    @GetMapping("/asignatura/{asignaturaId}")
    public ResponseEntity<List<Tema>> filterByAsignatura(@PathVariable String asignaturaId) throws Exception {
        List<Tema> temas = firestoreService.filterByField("temas", "asignaturaId", asignaturaId, Tema.class);
        return ResponseEntity.ok(temas);
    }
}