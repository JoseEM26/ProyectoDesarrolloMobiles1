package com.computronica.webapp.controller;

import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.service.FirestoreService;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private FirestoreService firestoreService;

    @Autowired
    private Firestore firestore;

    // CREATE
    @PostMapping
    public ResponseEntity<String> create(@RequestBody Usuario usuario) throws Exception {
        String id = firestoreService.create("usuarios", usuario);
        return ResponseEntity.ok(id);
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Usuario> getById(@PathVariable String id) throws Exception {
        Usuario usuario = firestoreService.getById("usuarios", id, Usuario.class);
        return ResponseEntity.ok(usuario);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Usuario>> getAll() throws Exception {
        List<Usuario> usuarios = firestoreService.getAll("usuarios", Usuario.class);
        return ResponseEntity.ok(usuarios);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody Map<String, Object> updates) throws Exception {
        DocumentReference docRef = firestore.collection("usuarios").document(id);
        Map<String, Object> updateData = new HashMap<>();

        // Only include provided fields
        if (updates.containsKey("nombre")) updateData.put("nombre", updates.get("nombre"));
        if (updates.containsKey("apellido")) updateData.put("apellido", updates.get("apellido"));
        if (updates.containsKey("codigoInstitucional")) updateData.put("codigoInstitucional", updates.get("codigoInstitucional"));
        if (updates.containsKey("sede")) updateData.put("sede", updates.get("sede"));
        updateData.put("updatedAt", com.google.cloud.Timestamp.now());

        WriteResult result = docRef.update(updateData).get();
        return ResponseEntity.ok().build();
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        firestoreService.delete("usuarios", id);
        return ResponseEntity.ok().build();
    }

    // FILTRAR POR EMAIL
    @GetMapping("/search")
    public ResponseEntity<List<Usuario>> filterByEmail(@RequestParam String email) throws Exception {
        List<Usuario> usuarios = firestoreService.filterByField("usuarios", "correoInstitucional", email, Usuario.class);
        return ResponseEntity.ok(usuarios);
    }

    // GET USER STATS
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String id) throws Exception {
        Map<String, Object> stats = new HashMap<>();
        // Fetch enrolled courses
        List<Map> asignaturas = firestoreService.filterByField("asignaturas", "estudiantes", id, Map.class);
        stats.put("enrolledCourses", asignaturas.size());
        // Fetch activities (courses + grades)
        List<Map> calificaciones = firestoreService.filterByField("calificaciones", "estudianteId", id, Map.class);
        stats.put("recentActivities", asignaturas.size() + calificaciones.size());
        // Calculate academic average
        double average = calificaciones.stream()
                .filter(c -> c.get("nota") instanceof Number)
                .mapToDouble(c -> ((Number) c.get("nota")).doubleValue())
                .average()
                .orElse(0.0);
        stats.put("academicAverage", average);
        return ResponseEntity.ok(stats);
    }

    // GET USER ACTIVITIES
    @GetMapping("/{id}/activities")
    public ResponseEntity<List<Map<String, Object>>> getUserActivities(@PathVariable String id) throws Exception {
        List<Map<String, Object>> activities = new ArrayList<>();
        // Fetch course enrollments
        List<Map> asignaturas = firestoreService.filterByField("asignaturas", "estudiantes", id, Map.class);
        for (Map asignatura : asignaturas) {
            activities.add(new HashMap<String, Object>() {{
                put("id", UUID.randomUUID().toString());
                put("type", "course");
                put("description", "Matriculado en " + asignatura.get("nombre"));
                put("date", asignatura.getOrDefault("createdAt", com.google.cloud.Timestamp.now()).toString());
            }});
        }
        // Fetch grades
        List<Map> calificaciones = firestoreService.filterByField("calificaciones", "estudianteId", id, Map.class);
        for (Map calificacion : calificaciones) {
            activities.add(new HashMap<String, Object>() {{
                put("id", UUID.randomUUID().toString());
                put("type", "grade");
                put("description", "Calificación " + calificacion.get("nota") + " en " + calificacion.get("asignaturaNombre"));
                put("date", calificacion.getOrDefault("fechaRegistro", com.google.cloud.Timestamp.now()).toString());
            }});
        }
        // Sort activities by date (descending)
        activities.sort((a, b) -> {
            String dateA = (String) a.get("date");
            String dateB = (String) b.get("date");
            return dateB.compareTo(dateA);
        });
        return ResponseEntity.ok(activities);
    }
}