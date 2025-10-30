package com.computronica.webapp.controller;

import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.service.FirestoreService;
import com.computronica.webapp.service.UsuarioService;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired private FirestoreService firestoreService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private Firestore firestore;

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
        return usuario != null ? ResponseEntity.ok(usuario) : ResponseEntity.notFound().build();
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Usuario>> getAll() throws Exception {
        List<Usuario> usuarios = firestoreService.getAll("usuarios", Usuario.class);
        return ResponseEntity.ok(usuarios);
    }

    // FILTER BY TIPO
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<Usuario>> getByTipo(@PathVariable String tipo) throws Exception {
        Set<String> tiposValidos = Set.of("estudiante", "profesor", "administrativo");
        if (!tiposValidos.contains(tipo)) {
            return ResponseEntity.badRequest().build();
        }

        List<Usuario> usuarios = firestoreService.filterByField("usuarios", "tipo", tipo, Usuario.class);
        return ResponseEntity.ok(usuarios);
    }

    // UPDATE (PATCH PARCIAL)
    @PutMapping("/{id}")
    public ResponseEntity<Usuario> update(@PathVariable String id, @RequestBody Map<String, Object> updates) throws Exception {
        Usuario usuario = firestoreService.getById("usuarios", id, Usuario.class);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }

        if (updates.containsKey("nombre")) usuario.setNombre((String) updates.get("nombre"));
        if (updates.containsKey("apellido")) usuario.setApellido((String) updates.get("apellido"));
        if (updates.containsKey("codigoInstitucional")) usuario.setCodigoInstitucional((String) updates.get("codigoInstitucional"));
        if (updates.containsKey("sede")) usuario.setSede((String) updates.get("sede"));
        if (updates.containsKey("tipo")) {
            String tipo = (String) updates.get("tipo");
            if (Set.of("estudiante", "profesor", "administrativo").contains(tipo)) {
                usuario.setTipo(tipo);
            }
        }

        usuario.setUpdatedAt(Timestamp.now());
        firestore.collection("usuarios").document(id).set(usuario).get();

        return ResponseEntity.ok(usuario);
    }

    // TOGGLE ESTADO
    @PatchMapping("/{id}/toggle-estado")
    public ResponseEntity<Usuario> toggleEstado(@PathVariable String id) {
        try {
            Usuario usuario = usuarioService.toggleEstadoUsuario(id);
            return ResponseEntity.ok(usuario);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // INHABILITAR
    @PatchMapping("/{id}/inhabilitar")
    public ResponseEntity<Void> inhabilitar(@PathVariable String id) {
        try {
            usuarioService.inhabilitarUsuario(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // HABILITAR
    @PatchMapping("/{id}/habilitar")
    public ResponseEntity<Void> habilitar(@PathVariable String id) {
        try {
            usuarioService.habilitarUsuario(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        firestoreService.delete("usuarios", id);
        return ResponseEntity.ok().build();
    }

    // BUSCAR POR EMAIL
    @GetMapping("/search")
    public ResponseEntity<List<Usuario>> searchByEmail(@RequestParam String email) throws Exception {
        List<Usuario> usuarios = firestoreService.filterByField("usuarios", "correoInstitucional", email, Usuario.class);
        return ResponseEntity.ok(usuarios);
    }

    // ESTADÍSTICAS DEL USUARIO
    @GetMapping("/{id}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String id) throws Exception {
        Map<String, Object> stats = new HashMap<>();

        // Asignaturas matriculadas
        List<Map<String, Object>> asignaturas = getAsignaturasByEstudiante(id);
        stats.put("enrolledCourses", asignaturas.size());

        // Calificaciones
        List<Map<String, Object>> calificaciones = getCalificacionesByEstudiante(id);
        stats.put("gradesCount", calificaciones.size());

        double promedio = calificaciones.stream()
                .filter(c -> c.get("nota") instanceof Number)
                .mapToDouble(c -> ((Number) c.get("nota")).doubleValue())
                .average()
                .orElse(0.0);
        stats.put("academicAverage", String.format("%.2f", promedio));

        return ResponseEntity.ok(stats);
    }

    // ACTIVIDADES RECIENTES
    @GetMapping("/{id}/activities")
    public ResponseEntity<List<Map<String, Object>>> getUserActivities(@PathVariable String id) throws Exception {
        List<Map<String, Object>> activities = new ArrayList<>();

        // Asignaturas
        List<Map<String, Object>> asignaturas = getAsignaturasByEstudiante(id);
        for (Map<String, Object> a : asignaturas) {
            activities.add(Map.of(
                    "type", "enrollment",
                    "title", "Matriculado en " + a.get("nombre"),
                    "date", a.getOrDefault("createdAt", Timestamp.now())
            ));
        }

        // Calificaciones
        List<Map<String, Object>> calificaciones = getCalificacionesByEstudiante(id);
        for (Map<String, Object> c : calificaciones) {
            activities.add(Map.of(
                    "type", "grade",
                    "title", "Nota " + c.get("nota") + " en " + c.get("asignaturaNombre"),
                    "date", c.getOrDefault("fecha", Timestamp.now())
            ));
        }

        // Ordenar por fecha
        activities.sort((a, b) -> {
            Timestamp d1 = (Timestamp) a.get("date");
            Timestamp d2 = (Timestamp) b.get("date");
            return d2.compareTo(d1);
        });

        return ResponseEntity.ok(activities);
    }

    // === MÉTODOS AUXILIARES ===
    private List<Map<String, Object>> getAsignaturasByEstudiante(String estudianteId) throws Exception {
        Query query = firestore.collection("asignaturas")
                .whereArrayContains("estudiantes", estudianteId);
        return query.get().get().getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    data.put("id", doc.getId());
                    return data;
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getCalificacionesByEstudiante(String estudianteId) throws Exception {
        Query query = firestore.collection("calificaciones")
                .whereEqualTo("estudianteId", estudianteId)
                .orderBy("fecha", Query.Direction.DESCENDING);
        return query.get().get().getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> data = doc.getData();
                    data.put("id", doc.getId());
                    // Resolver nombre de asignatura
                    String asignaturaId = (String) data.get("asignaturaId");
                    try {
                        DocumentSnapshot asignaturaDoc = firestore.collection("asignaturas")
                                .document(asignaturaId).get().get();
                        data.put("asignaturaNombre", asignaturaDoc.getString("nombre"));
                    } catch (Exception e) {
                        data.put("asignaturaNombre", "Desconocida");
                    }
                    return data;
                })
                .collect(Collectors.toList());
    }
}