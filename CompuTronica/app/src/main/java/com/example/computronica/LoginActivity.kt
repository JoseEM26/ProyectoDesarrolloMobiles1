package com.example.computronica

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()

        b.btnLogin.setOnClickListener {
            val email = b.etUsuario.text.toString().trim()
            val password = b.etContrasena.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Complete todos los campos")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid ?: ""
                    if (uid.isNotEmpty()) {
                        db.collection("usuarios").document(uid).get()
                            .addOnSuccessListener { doc ->
                                val usuario = doc.toObject(Usuario::class.java)
                                if (usuario != null) {
                                    usuario.id = doc.id // Asignar id
                                    // Check if user is active
                                    if (!usuario.estado) {
                                        auth.signOut() // Log out inactive user
                                        toast("❌ Su cuenta está inactiva. Contacte al administrador.")
                                        return@addOnSuccessListener
                                    }
                                    SessionManager.currentUser = usuario
                                    SessionManager.userId = uid
                                    toast("Bienvenido ${usuario.nombre}")
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                } else {
                                    toast("No se pudo cargar el usuario")
                                }
                            }
                            .addOnFailureListener { e ->
                                toast("Error: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener {
                    toast("Error de login: ${it.message ?: "No se pudo iniciar sesión"}")
                }
        }
    }

    override fun onStart() {
        super.onStart()
        val usuario = SessionManager.currentUser
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (usuario != null && firebaseUser != null) {
            // Verify user is still active
            db.collection("usuarios").document(firebaseUser.uid).get()
                .addOnSuccessListener { doc ->
                    val updatedUsuario = doc.toObject(Usuario::class.java)
                    if (updatedUsuario != null && updatedUsuario.estado) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        SessionManager.currentUser = null
                        SessionManager.userId = null
                        toast("❌ Su cuenta está inactiva. Contacte al administrador.")
                    }
                }
                .addOnFailureListener {
                    toast("Error al verificar usuario")
                }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}