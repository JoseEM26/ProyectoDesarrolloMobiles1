// src/main/java/com/computronica/webapp/controller/UsuarioController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.service.FirestoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private FirestoreService firestoreService;

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
    public ResponseEntity<Void> update(@PathVariable String id, @RequestBody Usuario usuario) throws Exception {
        firestoreService.update("usuarios", id, usuario);
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
}