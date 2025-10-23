package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Adapter.CalificacionAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityCalificacionBinding
import com.example.computronica.databinding.FormCalificacionesBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class CalificacionActivity : Fragment() {

    private var _b: ActivityCalificacionBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listenerReg: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val adapter = CalificacionAdapter(
        mutableListOf(),
        onEdit = { calificacion -> showEditDialog(calificacion) },
        onDelete = { calificacion -> eliminarCalificacion(calificacion) }
    )

    private val asignaturaNombres = mutableListOf<String>()
    private val asignaturaIds = mutableListOf<String>()
    private val estudianteNombres = mutableListOf<String>()
    private val estudianteIds = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityCalificacionBinding.inflate(inflater, container, false)

        b.rvCalificaciones.layoutManager = LinearLayoutManager(requireContext())
        b.rvCalificaciones.adapter = adapter
        b.rvCalificaciones.addItemDecoration(
            DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        )

        b.btnCalificaionesCreate.setOnClickListener { showCreateDialog() }

        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val usuario: Usuario? = SessionManager.currentUser

        b.btnCalificaionesCreate.isInvisible = usuario?.tipo == TipoUsuario.estudiante
        listarCalificaciones()
    }

    private fun listarCalificaciones() {
        listenerReg?.remove()
        val currentUser = SessionManager.currentUser
        if (currentUser == null) {
            Log.e("CalificacionActivity", "SessionManager.currentUser is null")
            b.tvEmpty.text = "Error: No hay usuario autenticado"
            b.tvEmpty.visibility = View.VISIBLE
            return
        }

        scope.launch {
            try {
                val query = when (currentUser.tipo) {
                    TipoUsuario.estudiante -> db.collection("calificaciones")
                        .whereEqualTo("estudianteId", currentUser.id)
                        .orderBy("fecha", Query.Direction.DESCENDING)
                    TipoUsuario.profesor -> {
                        val asignaturaIds = getProfessorAsignaturas(currentUser.id)
                        if (asignaturaIds.isEmpty()) {
                            b.tvEmpty.text = "No hay asignaturas asignadas"
                            b.tvEmpty.visibility = View.VISIBLE
                            return@launch
                        }
                        db.collection("calificaciones")
                            .whereIn("asignaturaId", asignaturaIds)
                            .orderBy("fecha", Query.Direction.DESCENDING)
                    }
                    TipoUsuario.administrativo -> db.collection("calificaciones")
                        .orderBy("fecha", Query.Direction.DESCENDING)
                }

                listenerReg = query.addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("CalificacionActivity", "Error loading calificaciones: ${e.message}", e)
                        b.tvEmpty.text = "Error al cargar calificaciones: ${e.message}"
                        b.tvEmpty.visibility = View.VISIBLE
                        return@addSnapshotListener
                    }

                    val list = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Calificaciones::class.java)?.copy(id = doc.id)
                    }.orEmpty()

                    adapter.replaceAll(list)
                    b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                Log.e("CalificacionActivity", "Error setting up calificaciones query: ${e.message}", e)
                b.tvEmpty.text = "Error al configurar la consulta: ${e.message}"
                b.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun getProfessorAsignaturas(professorId: String): List<String> {
        return try {
            val snapshot = db.collection("asignaturas")
                .whereArrayContains("profesores", professorId)
                .get().await()
            snapshot.documents.map { it.id }
        } catch (e: Exception) {
            Log.e("CalificacionActivity", "Error fetching professor asignaturas: ${e.message}", e)
            emptyList()
        }
    }

    private fun cargarAsignaturas(user: Usuario?, onLoaded: () -> Unit) {
        asignaturaNombres.clear()
        asignaturaIds.clear()
        val query = when (user?.tipo) {
            TipoUsuario.estudiante -> db.collection("asignaturas")
                .whereArrayContains("estudiantes", user.id)
            TipoUsuario.profesor -> db.collection("asignaturas")
                .whereArrayContains("profesores", user.id)
            else -> db.collection("asignaturas") // Admin or null user
        }

        query.orderBy("nombre", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) {
                    val asig = doc.toObject(Asignatura::class.java)
                    if (asig != null) {
                        asignaturaNombres.add(asig.nombre)
                        asignaturaIds.add(doc.id)
                    }
                }
                onLoaded()
            }
            .addOnFailureListener { e ->
                Log.e("CalificacionActivity", "Error loading asignaturas: ${e.message}", e)
                onLoaded()
            }
    }

    private fun cargarEstudiantes(onLoaded: () -> Unit) {
        estudianteNombres.clear()
        estudianteIds.clear()
        db.collection("usuarios")
            .whereEqualTo("tipo", "estudiante")
            .whereEqualTo("estado", true)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    estudianteNombres.add("No hay estudiantes")
                    estudianteIds.add("")
                } else {
                    for (doc in result.documents) {
                        val usuario = doc.toObject(Usuario::class.java)
                        if (usuario != null) {
                            estudianteNombres.add("${usuario.nombre} ${usuario.apellido}".trim())
                            estudianteIds.add(doc.id)
                        }
                    }
                }
                onLoaded()
            }
            .addOnFailureListener { e ->
                Log.e("CalificacionActivity", "Error loading estudiantes: ${e.message}", e)
                onLoaded()
            }
    }

    private fun showCreateDialog() {
        val dialogBinding = FormCalificacionesBinding.inflate(layoutInflater)
        val currentUser = SessionManager.currentUser

        // Spinner Evaluaciones
        val evaluacionesArray = resources.getStringArray(R.array.tipoAsignatura)
        dialogBinding.spnEvaluacion.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            evaluacionesArray
        )

        // Cargar estudiantes
        cargarEstudiantes {
            dialogBinding.spnEstudiante.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                estudianteNombres
            )
        }

        // Cargar asignaturas (role-based)
        cargarAsignaturas(currentUser) {
            dialogBinding.spnAsignatura.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                asignaturaNombres
            )
        }

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Registrar Calificación")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.titNota.error = null
                dialogBinding.tvEvaluacionLabel.error = null
                dialogBinding.tvEstudianteLabel.error = null
                dialogBinding.tvAsignaturaLabel.error = null

                val nota = dialogBinding.etNota.text.toString().toDoubleOrNull()
                val evaluacion = dialogBinding.spnEvaluacion.selectedItem?.toString() ?: ""
                val asignaturaIndex = dialogBinding.spnAsignatura.selectedItemPosition
                val estudianteIndex = dialogBinding.spnEstudiante.selectedItemPosition

                var isValid = true

                if (nota == null || nota < 0.0 || nota > 20.0) {
                    dialogBinding.titNota.error = "Ingrese una nota válida (0-20)"
                    isValid = false
                }
                if (evaluacion == "Seleccionar..." || evaluacion.isEmpty()) {
                    dialogBinding.tvEvaluacionLabel.error = "Seleccione una evaluación válida"
                    isValid = false
                }
                if (estudianteNombres.isEmpty() || estudianteNombres[0] == "No hay estudiantes" || estudianteIndex < 0) {
                    dialogBinding.tvEstudianteLabel.error = "Seleccione un estudiante válido"
                    isValid = false
                }
                if (asignaturaNombres.isEmpty() || asignaturaIndex < 0) {
                    dialogBinding.tvAsignaturaLabel.error = "Seleccione una asignatura válida"
                    isValid = false
                }

                if (!isValid) return@setOnClickListener

                scope.launch {
                    try {
                        val estudianteId = estudianteIds[estudianteIndex]
                        val asignaturaId = asignaturaIds[asignaturaIndex]
                        val calificacion = Calificaciones(
                            id = db.collection("calificaciones").document().id,
                            asignaturaId = asignaturaId,
                            nota = nota!!,
                            evaluacion = evaluacion,
                            fecha = com.google.firebase.Timestamp.now(),
                            estudianteId = estudianteId
                        )

                        db.collection("calificaciones").document(calificacion.id)
                            .set(calificacion)
                            .await()
                        alertDialog.dismiss()
                    } catch (e: Exception) {
                        Log.e("CalificacionActivity", "Error saving calificacion: ${e.message}", e)
                        dialogBinding.titNota.error = "Error al guardar: ${e.message}"
                    }
                }
            }
        }

        alertDialog.show()
    }

    private fun showEditDialog(calificacion: Calificaciones) {
        val dialogBinding = FormCalificacionesBinding.inflate(layoutInflater)
        val currentUser = SessionManager.currentUser

        dialogBinding.etNota.setText(calificacion.nota.toString())

        // Cargar estudiantes
        cargarEstudiantes {
            dialogBinding.spnEstudiante.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                estudianteNombres
            )
            val selectedIndex = estudianteIds.indexOf(calificacion.estudianteId)
            if (selectedIndex >= 0) dialogBinding.spnEstudiante.setSelection(selectedIndex)
        }

        // Cargar asignaturas (role-based)
        cargarAsignaturas(currentUser) {
            dialogBinding.spnAsignatura.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                asignaturaNombres
            )
            val selectedIndex = asignaturaIds.indexOf(calificacion.asignaturaId)
            if (selectedIndex >= 0) dialogBinding.spnAsignatura.setSelection(selectedIndex)
        }

        // Spinner Evaluaciones
        val evaluacionesArray = resources.getStringArray(R.array.tipoAsignatura)
        dialogBinding.spnEvaluacion.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            evaluacionesArray
        )
        val selectedEvalIndex = evaluacionesArray.indexOf(calificacion.evaluacion)
        if (selectedEvalIndex >= 0) dialogBinding.spnEvaluacion.setSelection(selectedEvalIndex)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Editar Calificación")
            .setView(dialogBinding.root)
            .setPositiveButton("Actualizar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.titNota.error = null
                dialogBinding.tvEvaluacionLabel.error = null
                dialogBinding.tvEstudianteLabel.error = null
                dialogBinding.tvAsignaturaLabel.error = null

                val nota = dialogBinding.etNota.text.toString().toDoubleOrNull()
                val evaluacion = dialogBinding.spnEvaluacion.selectedItem?.toString() ?: ""
                val asignaturaIndex = dialogBinding.spnAsignatura.selectedItemPosition
                val estudianteIndex = dialogBinding.spnEstudiante.selectedItemPosition

                var isValid = true

                if (nota == null || nota < 0.0 || nota > 20.0) {
                    dialogBinding.titNota.error = "Ingrese una nota válida (0-20)"
                    isValid = false
                }
                if (evaluacion == "Seleccionar..." || evaluacion.isEmpty()) {
                    dialogBinding.tvEvaluacionLabel.error = "Seleccione una evaluación válida"
                    isValid = false
                }
                if (estudianteNombres.isEmpty() || estudianteNombres[0] == "No hay estudiantes" || estudianteIndex < 0) {
                    dialogBinding.tvEstudianteLabel.error = "Seleccione un estudiante válido"
                    isValid = false
                }
                if (asignaturaNombres.isEmpty() || asignaturaIndex < 0) {
                    dialogBinding.tvAsignaturaLabel.error = "Seleccione una asignatura válida"
                    isValid = false
                }

                if (!isValid) return@setOnClickListener

                scope.launch {
                    try {
                        val estudianteId = estudianteIds[estudianteIndex]
                        val asignaturaId = asignaturaIds[asignaturaIndex]
                        val updates = mapOf(
                            "nota" to nota!!,
                            "evaluacion" to evaluacion,
                            "asignaturaId" to asignaturaId,
                            "estudianteId" to estudianteId,
                            "fecha" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("calificaciones").document(calificacion.id)
                            .update(updates)
                            .await()
                        alertDialog.dismiss()
                    } catch (e: Exception) {
                        Log.e("CalificacionActivity", "Error updating calificacion: ${e.message}", e)
                        dialogBinding.titNota.error = "Error al actualizar: ${e.message}"
                    }
                }
            }
        }

        alertDialog.show()
    }

    private fun eliminarCalificacion(calificacion: Calificaciones) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Calificación")
            .setMessage("¿Deseas eliminar esta calificación?")
            .setPositiveButton("Eliminar") { _, _ ->
                scope.launch {
                    try {
                        db.collection("calificaciones").document(calificacion.id)
                            .delete()
                            .await()
                    } catch (e: Exception) {
                        Log.e("CalificacionActivity", "Error deleting calificacion: ${e.message}", e)
                        b.tvEmpty.text = "Error al eliminar: ${e.message}"
                        b.tvEmpty.visibility = View.VISIBLE
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        scope.cancel()
        _b = null
    }
}