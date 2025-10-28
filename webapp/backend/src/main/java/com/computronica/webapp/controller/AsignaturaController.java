package com.computronica.webapp.controller;

import com.computronica.webapp.model.Asignatura;
import com.computronica.webapp.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignaturas")
@CrossOrigin(origins = "http://localhost:4200") // Permite Angular
public class AsignaturaController {

    @Autowired
    private FirestoreService firestoreService;

    // CREATE → Devuelve la asignatura con ID
    @PostMapping
    public ResponseEntity<Asignatura> create(@RequestBody Asignatura asignatura) throws Exception {
        String id = firestoreService.create("asignaturas", asignatura);
        asignatura.setId(id); // Asigna el ID generado
        return ResponseEntity.ok(asignatura);
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

    // UPDATE → Devuelve la asignatura actualizada
    @PutMapping("/{id}")
    public ResponseEntity<Asignatura> update(@PathVariable String id, @RequestBody Asignatura asignatura) throws Exception {
        asignatura.setId(id); // Asegura que el ID esté presente
        firestoreService.update("asignaturas", id, asignatura);
        return ResponseEntity.ok(asignatura);
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