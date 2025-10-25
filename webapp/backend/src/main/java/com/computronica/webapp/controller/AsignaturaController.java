package com.computronica.webapp.controller;

import com.computronica.webapp.model.Asignatura;
import com.computronica.webapp.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaturas")
public class AsignaturaController {

    @Autowired
    private FirestoreService firestoreService;

    // CREATE
    @PostMapping
    public ResponseEntity<String> create(@RequestBody Asignatura asignatura) throws Exception {
        String id = firestoreService.create("asignaturas", asignatura);
        return ResponseEntity.ok(id);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Asignatura> getById(@PathVariable String id) throws Exception {
        Asignatura asignatura = firestoreService.getById("asignaturas", id, Asignatura.class);
        return ResponseEntity.ok(asignatura);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Asignatura>> getAll() throws Exception {
        List<Asignatura> asignaturas = firestoreService.getAll("asignaturas", Asignatura.class);
        return ResponseEntity.ok(asignaturas);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody Asignatura asignatura) throws Exception {
        firestoreService.update("asignaturas", id, asignatura);
        return ResponseEntity.ok().build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        firestoreService.delete("asignaturas", id);
        return ResponseEntity.ok().build();
    }

    // FILTRAR POR NOMBRE
    @GetMapping("/search")
    public ResponseEntity<List<Asignatura>> filterByNombre(@RequestParam String nombre) throws Exception {
        List<Asignatura> asignaturas = firestoreService.filterByField("asignaturas", "nombre", nombre, Asignatura.class);
        return ResponseEntity.ok(asignaturas);
    }
}