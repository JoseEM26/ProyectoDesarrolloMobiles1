// src/main/java/com/computronica/webapp/controller/AuthController.java
package com.computronica.webapp.controller;

import com.computronica.webapp.model.Usuario;
import com.computronica.webapp.service.UsuarioService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @Autowired
    private UsuarioService usuarioService;

    // ====================
    // MOSTRAR LOGIN
    // ====================
    @GetMapping({"/", "/login"})
    public String showLogin(@RequestParam(required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas.");
        }
        return "login";
    }

    // ====================
    // PROCESAR LOGIN CON FIREBASE
    // ====================
    @PostMapping("/login")
    public String loginWithFirebase(@RequestParam String idToken, HttpSession session, Model model) {
        try {
            // Verificar token de Firebase
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String email = decodedToken.getEmail();
            String uid = decodedToken.getUid();
            String nombre = decodedToken.getName();

            // Buscar usuario en Firestore
            Usuario usuario = usuarioService.findByEmail(email);
            if (usuario == null) {
                usuario = new Usuario();
                usuario.setEmail(email);
                usuario.setNombre(nombre != null ? nombre : email.split("@")[0]);
                usuario.setUid(uid);
                usuarioService.save(usuario); // Guarda en Firestore
            }

            // Guardar en sesión
            session.setAttribute("usuario", usuario);
            return "redirect:/home";

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Error de autenticación.");
            return "login";
        }
    }
    @GetMapping("/home")
    public String home(HttpSession session, Model model, HttpServletRequest request) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        model.addAttribute("title", "Inicio");
        model.addAttribute("currentPath", request.getRequestURI());
        model.addAttribute("usuario", usuario);

        return "home"; // ← PERFECTO
    }
    // ====================
    // CERRAR SESIÓN
    // ====================
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}