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
    public ResponseEntity<Calificaciones> create(@RequestBody Calificaciones calificacion) {
        try {
            System.out.println("=== [CREATE CALIFICACIÓN] Recibido ===");
            System.out.println("Entrada: " + calificacion);

            if (calificacion == null) {
                System.err.println("ERROR: Calificación es null");
                return ResponseEntity.badRequest().body(null);
            }

            // VALIDACIONES
            if (calificacion.getAsignaturaId() == null || calificacion.getAsignaturaId().trim().isEmpty()) {
                System.err.println("ERROR: asignaturaId requerido");
                return ResponseEntity.badRequest().body(null);
            }
            if (calificacion.getEstudianteId() == null || calificacion.getEstudianteId().trim().isEmpty()) {
                System.err.println("ERROR: estudianteId requerido");
                return ResponseEntity.badRequest().body(null);
            }
            if (calificacion.getEvaluacion() == null || calificacion.getEvaluacion().trim().isEmpty()) {
                System.err.println("ERROR: evaluacion requerida");
                return ResponseEntity.badRequest().body(null);
            }

            // NO TOQUES EL ID AQUÍ
            // calificacion.setId(null); // ← ¡NO HAGAS ESTO!

            System.out.println("Llamando a FirestoreService.create()...");
            String generatedId = firestoreService.create("calificaciones", calificacion);

            // ASIGNAR SOLO PARA LA RESPUESTA
            calificacion.setId(generatedId);

            System.out.println("=== [ÉXITO] ID generado: " + generatedId);
            System.out.println("Objeto final: " + calificacion);

            return ResponseEntity.ok(calificacion);

        } catch (Exception e) {
            System.err.println("=== [ERROR] Fallo al crear calificación ===");
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
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
    public ResponseEntity<Calificaciones> update(@PathVariable String id, @RequestBody Calificaciones calificacion) throws Exception {
        firestoreService.update("calificaciones", id, calificacion);
        calificacion.setId(id);
        return ResponseEntity.ok(calificacion);

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