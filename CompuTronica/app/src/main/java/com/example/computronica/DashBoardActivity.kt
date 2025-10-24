package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityDashBoardBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class DashBoardActivity : Fragment() {

    private var _binding: ActivityDashBoardBinding? = null
    private val binding get() = _binding!!
    private val db by lazy { FirebaseFirestore.getInstance() }
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
        showLoadingState()
        loadUserData()
    }

    private fun updateTextColors() {
        binding.tvWelcome.setTextColor(ContextCompat.getColor(requireContext(), R.color.azul_oscuro))
        binding.tvEstudiantesCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.azul_profundo))
        binding.tvCalificacionesCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.azul_suave))
        binding.tvAsignaturasCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.azul_verdoso))
    }

    private fun showLoadingState() {
        binding.tvWelcome.text = getString(R.string.loading)
        binding.tvEstudiantesCount.text = "..."
        binding.tvCalificacionesCount.text = "..."
        binding.tvAsignaturasCount.text = "..."
        binding.llPromedios.removeAllViews()
        binding.llRecientes.removeAllViews()
        updateTextColors()
    }

    private fun clearLoadingState() {
        binding.tvEstudiantesCount.text = "-"
        binding.tvCalificacionesCount.text = "-"
        binding.tvAsignaturasCount.text = "-"
        binding.llPromedios.removeAllViews()
        binding.llRecientes.removeAllViews()
        updateTextColors()
    }

    private fun loadUserData() {
        val currentUser = SessionManager.currentUser
        if (currentUser == null) {
            Log.e("DashBoardActivity", "SessionManager.currentUser is null")
            binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
            clearLoadingState()
            return
        }

        scope.launch {
            try {
                val doc = db.collection("usuarios").document(currentUser.id).get().await()
                val user = doc.toObject(Usuario::class.java)
                if (user != null && user.estado) {
                    binding.tvWelcome.text = getString(R.string.welcome_user, user.nombre)
                    loadDashboardData(user)
                } else {
                    binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
                    clearLoadingState()
                }
            } catch (e: Exception) {
                Log.e("DashBoardActivity", "Error loading user data: ${e.message}", e)
                binding.tvWelcome.text = getString(R.string.welcome_default, "Usuario")
                clearLoadingState()
            }
        }
    }

    private fun loadDashboardData(user: Usuario) {
        // Role-based visibility and labels
        when (user.tipo) {
            TipoUsuario.estudiante -> {
                binding.cardEstudiantes.visibility = View.GONE
                binding.cardCalificaciones.visibility = View.VISIBLE
                binding.cardAsignaturas.visibility = View.VISIBLE
                binding.tvEstudiantesLabel.text = getString(R.string.students)
                binding.tvCalificacionesLabel.text = getString(R.string.my_grades)
                binding.tvAsignaturasLabel.text = getString(R.string.my_subjects)
                binding.tvPromediosLabel.text = getString(R.string.my_average_by_subject)
            }
            TipoUsuario.profesor -> {
                binding.cardEstudiantes.visibility = View.VISIBLE
                binding.cardCalificaciones.visibility = View.VISIBLE
                binding.cardAsignaturas.visibility = View.VISIBLE
                binding.tvEstudiantesLabel.text = getString(R.string.total_students)
                binding.tvCalificacionesLabel.text = getString(R.string.assigned_grades)
                binding.tvAsignaturasLabel.text = getString(R.string.teaching_subjects)
                binding.tvPromediosLabel.text = getString(R.string.class_average_by_subject)
            }
            TipoUsuario.administrativo -> {
                binding.cardEstudiantes.visibility = View.VISIBLE
                binding.cardCalificaciones.visibility = View.VISIBLE
                binding.cardAsignaturas.visibility = View.VISIBLE
                binding.tvEstudiantesLabel.text = getString(R.string.total_students)
                binding.tvCalificacionesLabel.text = getString(R.string.total_grades)
                binding.tvAsignaturasLabel.text = getString(R.string.total_subjects)
                binding.tvPromediosLabel.text = getString(R.string.average_by_subject)
            }
        }

        // Load student count
        if (user.tipo == TipoUsuario.administrativo || user.tipo == TipoUsuario.profesor) {
            scope.launch {
                try {
                    if (user.tipo == TipoUsuario.administrativo) {
                        val snapshot = db.collection("usuarios")
                            .whereEqualTo("tipo", "estudiante")
                            .whereEqualTo("estado", true)
                            .get().await()
                        binding.tvEstudiantesCount.text = snapshot.size().toString()
                    } else { // Profesor
                        val asignaturas = getProfessorAsignaturas(user.id)
                        if (asignaturas.isEmpty()) {
                            binding.tvEstudiantesCount.text = "0"
                            return@launch
                        }
                        val estudiantes = mutableSetOf<String>()
                        db.collection("asignaturas")
                            .whereIn("id", asignaturas)
                            .get().await()
                            .documents.forEach { doc ->
                                val estudiantesList = doc.get("estudiantes") as? List<String> ?: emptyList()
                                estudiantes.addAll(estudiantesList)
                            }
                        binding.tvEstudiantesCount.text = estudiantes.size.toString()
                    }
                } catch (e: Exception) {
                    binding.tvEstudiantesCount.text = "-"
                    Log.e("DashBoardActivity", "Error loading estudiantes count: ${e.message}", e)
                }
            }
        }

        // Load subjects count
        scope.launch {
            try {
                val query = when (user.tipo) {
                    TipoUsuario.estudiante -> db.collection("asignaturas")
                        .whereArrayContains("estudiantes", user.id)
                    TipoUsuario.profesor -> db.collection("asignaturas")
                        .whereArrayContains("profesores", user.id)
                    TipoUsuario.administrativo -> db.collection("asignaturas")
                }
                val snapshot = query.get().await()
                binding.tvAsignaturasCount.text = snapshot.size().toString()
            } catch (e: Exception) {
                binding.tvAsignaturasCount.text = "-"
                Log.e("DashBoardActivity", "Error loading asignaturas count: ${e.message}", e)
            }
        }

        // Load grades count
        scope.launch {
            try {
                val query = when (user.tipo) {
                    TipoUsuario.estudiante -> db.collection("calificaciones")
                        .whereEqualTo("estudianteId", user.id)
                    TipoUsuario.profesor -> db.collection("calificaciones")
                        .whereIn("asignaturaId", getProfessorAsignaturas(user.id))
                    TipoUsuario.administrativo -> db.collection("calificaciones")
                }
                val snapshot = query.get().await()
                binding.tvCalificacionesCount.text = snapshot.size().toString()
            } catch (e: Exception) {
                binding.tvCalificacionesCount.text = "-"
                Log.e("DashBoardActivity", "Error loading calificaciones count: ${e.message}", e)
            }
        }

        loadSubjectAverages(user)
        loadRecentGrades(user)
    }

    private suspend fun getProfessorAsignaturas(professorId: String): List<String> {
        return try {
            val snapshot = db.collection("asignaturas")
                .whereArrayContains("profesores", professorId)
                .get().await()
            snapshot.documents.map { it.id }
        } catch (e: Exception) {
            Log.e("DashBoardActivity", "Error fetching professor asignaturas: ${e.message}", e)
            emptyList()
        }
    }

    private fun loadSubjectAverages(user: Usuario) {
        scope.launch {
            try {
                binding.llPromedios.removeAllViews()
                val asignaturasQuery = when (user.tipo) {
                    TipoUsuario.estudiante -> db.collection("asignaturas")
                        .whereArrayContains("estudiantes", user.id)
                    TipoUsuario.profesor -> db.collection("asignaturas")
                        .whereArrayContains("profesores", user.id)
                    TipoUsuario.administrativo -> db.collection("asignaturas")
                }
                val asignaturasSnapshot = asignaturasQuery.get().await()
                if (asignaturasSnapshot.isEmpty) {
                    val noDataText = TextView(requireContext()).apply {
                        text = "No hay asignaturas disponibles"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gris_oscuro))
                        textSize = 14f
                        setPadding(16, 8, 16, 8)
                    }
                    binding.llPromedios.addView(noDataText)
                    return@launch
                }

                for (asignaturaDoc in asignaturasSnapshot.documents) {
                    val asignaturaId = asignaturaDoc.id
                    val asignaturaNombre = asignaturaDoc.getString("nombre") ?: "Desconocida"

                    val calificacionesQuery = when (user.tipo) {
                        TipoUsuario.estudiante -> db.collection("calificaciones")
                            .whereEqualTo("estudianteId", user.id)
                            .whereEqualTo("asignaturaId", asignaturaId)
                        TipoUsuario.profesor -> db.collection("calificaciones")
                            .whereEqualTo("asignaturaId", asignaturaId)
                        TipoUsuario.administrativo -> db.collection("calificaciones")
                            .whereEqualTo("asignaturaId", asignaturaId)
                    }
                    val calificacionesSnapshot = calificacionesQuery.get().await()
                    val grades = calificacionesSnapshot.documents.mapNotNull { it.getDouble("nota") }
                    val average = if (grades.isNotEmpty()) grades.average() else 0.0

                    // Create view for each subject average
                    val averageView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_subject_average, binding.llPromedios, false) as LinearLayout
                    val tvAsignatura = averageView.findViewById<TextView>(R.id.tvAsignaturaNombre)
                    val tvPromedio = averageView.findViewById<TextView>(R.id.tvPromedioValor)

                    tvAsignatura.text = asignaturaNombre
                    tvPromedio.text = if (grades.isNotEmpty()) String.format("%.2f", average) else "-"
                    tvPromedio.setTextColor(ContextCompat.getColor(requireContext(),
                        if (grades.isNotEmpty() && average >= 10.5) R.color.verde_exito else R.color.rojo_error))

                    binding.llPromedios.addView(averageView)
                }
            } catch (e: Exception) {
                Log.e("DashBoardActivity", "Error loading subject averages: ${e.message}", e)
                binding.llPromedios.removeAllViews()
                val errorText = TextView(requireContext()).apply {
                    text = "Error al cargar promedios"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.rojo_error))
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                }
                binding.llPromedios.addView(errorText)
            } finally {
                updateTextColors()
            }
        }
    }

    private fun loadRecentGrades(user: Usuario) {
        scope.launch {
            try {
                val query = when (user.tipo) {
                    TipoUsuario.estudiante -> db.collection("calificaciones")
                        .whereEqualTo("estudianteId", user.id)
                        .orderBy("fecha", Query.Direction.DESCENDING)
                        .limit(5)
                    TipoUsuario.profesor -> db.collection("calificaciones")
                        .whereIn("asignaturaId", getProfessorAsignaturas(user.id))
                        .orderBy("fecha", Query.Direction.DESCENDING)
                        .limit(5)
                    TipoUsuario.administrativo -> db.collection("calificaciones")
                        .orderBy("fecha", Query.Direction.DESCENDING)
                        .limit(5)
                }
                val snapshot = query.get().await()
                binding.llRecientes.removeAllViews()

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

                for (doc in snapshot.documents) {
                    val calificacion = doc.toObject(Calificaciones::class.java) ?: continue
                    val asignaturaDoc = db.collection("asignaturas")
                        .document(calificacion.asignaturaId).get().await()
                    val asignaturaNombre = asignaturaDoc.getString("nombre") ?: "Desconocida"

                    val gradeView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_recent_grade, binding.llRecientes, false) as LinearLayout
                    val tvAsignatura = gradeView.findViewById<TextView>(R.id.tvAsignatura)
                    val tvNota = gradeView.findViewById<TextView>(R.id.tvNota)
                    val tvFecha = gradeView.findViewById<TextView>(R.id.tvFecha)

                    tvAsignatura.text = asignaturaNombre
                    tvNota.text = String.format("%.2f", calificacion.nota)
                    tvFecha.text = calificacion.fecha?.toDate()?.let { dateFormat.format(it) } ?: "-"

                    binding.llRecientes.addView(gradeView)
                }

                if (snapshot.isEmpty) {
                    val noGradesText = TextView(requireContext()).apply {
                        text = "No hay calificaciones recientes"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gris_oscuro))
                        textSize = 14f
                        setPadding(16, 8, 16, 8)
                    }
                    binding.llRecientes.addView(noGradesText)
                }
            } catch (e: Exception) {
                Log.e("DashBoardActivity", "Error loading recent grades: ${e.message}", e)
                binding.llRecientes.removeAllViews()
                val errorText = TextView(requireContext()).apply {
                    text = "Error al cargar calificaciones"
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.rojo_error))
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                }
                binding.llRecientes.addView(errorText)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scope.cancel()
        _binding = null
    }
}