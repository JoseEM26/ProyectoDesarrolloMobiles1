package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityDashBoardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class DashBoardActivity : Fragment() {

    private var _binding: ActivityDashBoardBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val userId by lazy { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    private var userTipo: TipoUsuario = TipoUsuario.estudiante
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDashBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFirebase()
        loadUserData()
    }

    // Initialize Firebase and check for configuration issues
    private fun initializeFirebase() {
        try {
            FirebaseAuth.getInstance().currentUser ?: run {
                binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
                clearLoadingState()
            }
            db.firestoreSettings = db.firestoreSettings // Ensure Firestore is initialized
        } catch (e: Exception) {
            binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
            clearLoadingState()
        }
    }

    private fun showLoadingState() {
        binding.tvWelcome.text = getString(R.string.loading)
        binding.tvEstudiantesCount.text = "..."
        binding.tvCalificacionesCount.text = "..."
        binding.tvAsignaturasCount.text = "..."
        binding.tvPromedio.text = "..."
    }

    private fun clearLoadingState() {
        binding.tvEstudiantesCount.text = "-"
        binding.tvCalificacionesCount.text = "-"
        binding.tvAsignaturasCount.text = "-"
        binding.tvPromedio.text = "-"
    }

    private fun loadUserData() {
        if (userId.isEmpty()) {
            binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
            clearLoadingState()
            return
        }

        scope.launch {
            try {
                val doc = db.collection("usuarios").document(userId).get().await()
                val user = doc.toObject(Usuario::class.java)
                if (user != null) {
                    userTipo = user.tipo
                    binding.tvWelcome.text = getString(R.string.welcome_user, user.nombre)
                    loadDashboardData()
                } else {
                    binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
                    clearLoadingState()
                }
            } catch (e: Exception) {
                binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
                clearLoadingState()
            }
        }
    }

    private fun loadDashboardData() {
        loadCounts()
        loadAverageGrade()
    }

    private fun loadCounts() {
        if (userTipo == TipoUsuario.administrativo) {
            scope.launch {
                try {
                    val snapshot = db.collection("usuarios")
                        .whereEqualTo("tipo", TipoUsuario.estudiante.name)
                        .get().await()
                    binding.tvEstudiantesCount.text = snapshot.size().toString()
                } catch (e: Exception) {
                    binding.tvEstudiantesCount.text = "-"
                }
            }
        } else {
            binding.tvEstudiantesCount.text = "-"
        }

        scope.launch {
            try {
                val query = if (userTipo == TipoUsuario.estudiante) {
                    db.collection("calificaciones").whereEqualTo("estudianteId", userId)
                } else {
                    db.collection("calificaciones")
                }
                val snapshot = query.get().await()
                binding.tvCalificacionesCount.text = snapshot.size().toString()
            } catch (e: Exception) {
                binding.tvCalificacionesCount.text = "-"
            }
        }

        if (userTipo != TipoUsuario.estudiante) {
            scope.launch {
                try {
                    val snapshot = db.collection("asignaturas").get().await()
                    binding.tvAsignaturasCount.text = snapshot.size().toString()
                } catch (e: Exception) {
                    binding.tvAsignaturasCount.text = "-"
                }
            }
        } else {
            binding.tvAsignaturasCount.text = "-"
        }
    }

    private fun loadAverageGrade() {
        scope.launch {
            try {
                val query = if (userTipo == TipoUsuario.estudiante) {
                    db.collection("calificaciones").whereEqualTo("estudianteId", userId)
                } else {
                    db.collection("calificaciones")
                }
                val snapshot = query.get().await()
                if (!snapshot.isEmpty) {
                    val grades = snapshot.documents.mapNotNull { it.getDouble("nota") }
                    val average = grades.average()
                    binding.tvPromedio.text = String.format("%.2f", average)
                } else {
                    binding.tvPromedio.text = "-"
                }
            } catch (e: Exception) {
                binding.tvPromedio.text = "-"
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
}