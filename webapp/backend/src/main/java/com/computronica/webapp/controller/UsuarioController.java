package com.computronica.webapp.controller;

import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.service.FirestoreService;
import com.computronica.webapp.service.UsuarioService;
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
    private UsuarioService usuarioService;

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
// src/main/java/com/computronica/webapp/controller/UsuarioController.java

    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Usuario>> getByTipo(@PathVariable String tipo) throws Exception {
        // Validar tipo permitido
        if (!List.of("estudiante", "profesor", "administrativo").contains(tipo)) {
            return ResponseEntity.badRequest().build();
        }

        List<Usuario> usuarios = firestoreService.filterByField(
                "usuarios", "tipo", tipo, Usuario.class
        );

        // Asignar ID a cada usuario
        usuarios.forEach(u -> {
            try {
                // Si no tiene ID, buscarlo por correo o UID
                if (u.getId() == null) {
                    Usuario found = usuarioService.findByCorreoInstitucional(u.getCorreoInstitucional());
                    if (found != null) u.setId(found.getId());
                }
            } catch (Exception e) {
                // Ignorar
            }
        });

        return ResponseEntity.ok(usuarios);
    }
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> update(@PathVariable String id, @RequestBody Map<String, Object> updates) throws Exception {
        DocumentReference docRef = firestore.collection("usuarios").document(id);

        // Obtener el usuario actual
        Usuario usuario = firestoreService.getById("usuarios", id, Usuario.class);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        // Aplicar actualizaciones
        if (updates.containsKey("nombre")) usuario.setNombre((String) updates.get("nombre"));
        if (updates.containsKey("apellido")) usuario.setApellido((String) updates.get("apellido"));
        if (updates.containsKey("codigoInstitucional")) usuario.setCodigoInstitucional((String) updates.get("codigoInstitucional"));
        if (updates.containsKey("sede")) usuario.setSede((String) updates.get("sede"));
        if (updates.containsKey("tipo")) usuario.setTipo((String) updates.get("tipo"));

        usuario.setUpdatedAt(com.google.cloud.Timestamp.now());

        // Guardar
        docRef.set(usuario).get();

        return ResponseEntity.ok(usuario); // ← DEVUELVE EL USUARIO
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