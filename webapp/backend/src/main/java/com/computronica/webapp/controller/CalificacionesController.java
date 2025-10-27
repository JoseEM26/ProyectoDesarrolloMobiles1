// src/main/java/com/computronica/webapp/controller/CalificacionesController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.model.Calificaciones;
import com.computronica.webapp.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/calificaciones")
public class CalificacionesController {

    @Autowired
    private FirestoreService firestoreService;

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Calificaciones calificacion) throws Exception {
        String id = firestoreService.create("calificaciones", calificacion);
        return ResponseEntity.ok(id);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Calificaciones> getById(@PathVariable String id) throws Exception {
        Calificaciones calificacion = firestoreService.getById("calificaciones", id, Calificaciones.class);
        return calificacion != null ? ResponseEntity.ok(calificacion) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<Calificaciones>> getAll() throws Exception {
        List<Calificaciones> calificaciones = firestoreService.getAll("calificaciones", Calificaciones.class);
        return ResponseEntity.ok(calificaciones);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody Calificaciones calificacion) throws Exception {
        firestoreService.update("calificaciones", id, calificacion);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        firestoreService.delete("calificaciones", id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search/estudiante")
    public ResponseEntity<List<Calificaciones>> filterByEstudiante(@RequestParam String estudianteId) throws Exception {
        List<Calificaciones> calificaciones = firestoreService.filterByField("calificaciones", "estudianteId", estudianteId, Calificaciones.class);
        return ResponseEntity.ok(calificaciones);
    }

    @GetMapping("/search/asignatura")
    public ResponseEntity<List<Calificaciones>> filterByAsignatura(@RequestParam String asignaturaId) throws Exception {
        List<Calificaciones> calificaciones = firestoreService.filterByField("calificaciones", "asignaturaId", asignaturaId, Calificaciones.class);
        return ResponseEntity.ok(calificaciones);
    }

    @PostMapping("/fix-fecha")
    public ResponseEntity<String> fixFechaField() throws Exception {
        firestoreService.fixFechaField("calificaciones");
        return ResponseEntity.ok("Fecha fields fixed in Firestore");
    }
}