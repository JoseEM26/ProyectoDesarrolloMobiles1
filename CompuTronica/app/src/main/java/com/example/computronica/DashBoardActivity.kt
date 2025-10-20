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

class DashBoardActivity : Fragment() {

    private var _b: ActivityDashBoardBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private var userTipo: TipoUsuario = TipoUsuario.estudiante // default

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityDashBoardBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener tipo de usuario y nombre
        db.collection("usuarios").document(userId).get()
            .addOnSuccessListener { doc ->
                val user = doc.toObject(Usuario::class.java)
                if (user != null) {
                    userTipo = user.tipo
                    b.tvWelcome.text = "Bienvenido, ${user.nombre}"
                    cargarDatosDashboard()
                }
            }
            .addOnFailureListener {
                b.tvWelcome.text = "Bienvenido, Usuario"
            }
    }

    private fun cargarDatosDashboard() {
        cargarConteos()
        cargarPromedio()
        cargarUltimasCalificaciones()
    }

    private fun cargarConteos() {
        // Contar Estudiantes (solo admin)
        if (userTipo == TipoUsuario.administrativo) {
            db.collection("usuarios").whereEqualTo("tipo", TipoUsuario.estudiante.name).get()
                .addOnSuccessListener { snapshot ->
                    b.tvEstudiantesCount.text = snapshot.size().toString()
                }
        } else {
            b.tvEstudiantesCount.text = "-" // No mostrar a profesor o estudiante
        }

        // Contar Calificaciones
        val calificacionesQuery: Query = if (userTipo == TipoUsuario.estudiante) {
            db.collection("calificaciones").whereEqualTo("estudianteId", userId)
        } else {
            db.collection("calificaciones")
        }

        calificacionesQuery.get()
            .addOnSuccessListener { snapshot ->
                b.tvCalificacionesCount.text = snapshot.size().toString()
            }

        // Contar Asignaturas (solo admin/profesor)
        if (userTipo != TipoUsuario.estudiante) {
            db.collection("asignaturas").get()
                .addOnSuccessListener { snapshot ->
                    b.tvAsignaturasCount.text = snapshot.size().toString()
                }
        } else {
            b.tvAsignaturasCount.text = "-" // estudiantes no necesitan ver todas
        }
    }

    private fun cargarPromedio() {
        val calificacionesQuery: Query = if (userTipo == TipoUsuario.estudiante) {
            db.collection("calificaciones").whereEqualTo("estudianteId", userId)
        } else {
            db.collection("calificaciones")
        }

        calificacionesQuery.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val notas = snapshot.documents.mapNotNull { it.getDouble("nota") }
                    val promedio = notas.average()
                    b.tvPromedio.text = String.format("%.2f", promedio)
                } else {
                    b.tvPromedio.text = "-"
                }
            }
    }

    private fun cargarUltimasCalificaciones() {
        val calificacionesQuery: Query = if (userTipo == TipoUsuario.estudiante) {
            db.collection("calificaciones")
                .whereEqualTo("estudianteId", userId)
                .orderBy("fechaRegistro", Query.Direction.DESCENDING)
                .limit(5)
        } else {
            db.collection("calificaciones")
                .orderBy("fechaRegistro", Query.Direction.DESCENDING)
                .limit(5)
        }

        calificacionesQuery.get()
            .addOnSuccessListener { snapshot ->
                b.llRecientes.removeAllViews()
                if (!snapshot.isEmpty) {
                    for (doc in snapshot.documents) {
                        val cal = doc.toObject(Calificaciones::class.java)
                        if (cal != null) {
                            val tv = TextView(requireContext()).apply {
                                text = "${cal.estudianteId} - ${cal.asignaturaId} - ${cal.nota}"
                                setPadding(6, 6, 6, 6)
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.negro))
                            }
                            b.llRecientes.addView(tv)
                        }
                    }
                } else {
                    val tv = TextView(requireContext()).apply {
                        text = "No hay calificaciones"
                        setPadding(6, 6, 6, 6)
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.gris_claro))
                    }
                    b.llRecientes.addView(tv)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
