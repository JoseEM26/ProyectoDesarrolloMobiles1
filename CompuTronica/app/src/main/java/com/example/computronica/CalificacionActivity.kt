package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Adapter.CalificacionAdapter
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.Asignatura
import com.example.computronica.databinding.ActivityCalificacionBinding
import com.example.computronica.databinding.FormCalificacionesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class CalificacionActivity : Fragment() {

    private var _b: ActivityCalificacionBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listenerReg: ListenerRegistration? = null

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
        listarCalificaciones()
    }

    private fun listarCalificaciones() {
        listenerReg?.remove()
        listenerReg = db.collection("calificaciones")
            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Calificaciones::class.java)?.copy(id = doc.id)
                }.orEmpty()

                adapter.replaceAll(list)
                b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun cargarAsignaturas(onLoaded: () -> Unit) {
        asignaturaNombres.clear()
        asignaturaIds.clear()
        db.collection("asignaturas")
            .orderBy("nombre", Query.Direction.ASCENDING)
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
            .addOnFailureListener { onLoaded() }
    }

    private fun cargarEstudiantes(onLoaded: () -> Unit) {
        estudianteNombres.clear()
        estudianteIds.clear()
        db.collection("usuarios")
            .whereEqualTo("tipo", "estudiante")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    estudianteNombres.add("No hay estudiantes")
                } else {
                    for (doc in result.documents) {
                        val nombre = doc.getString("nombre") ?: "Sin nombre"
                        estudianteNombres.add(nombre)
                        estudianteIds.add(doc.id)
                    }
                }
                onLoaded()
            }
            .addOnFailureListener { onLoaded() }
    }

    private fun showCreateDialog() {
        val dialogBinding = FormCalificacionesBinding.inflate(layoutInflater)

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

        // Cargar asignaturas
        cargarAsignaturas {
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

                val nota = dialogBinding.etNota.text.toString().toDouble()
                val evaluacion = dialogBinding.spnEvaluacion.selectedItem?.toString() ?: ""
                val asignaturaIndex = dialogBinding.spnAsignatura.selectedItemPosition
                val estudianteIndex = dialogBinding.spnEstudiante.selectedItemPosition

                var isValid = true

                // Validar nota
                if (nota == null || nota < 0.0 || nota > 20.0) {
                    dialogBinding.titNota.error = "Ingrese una nota válida (0-20)"
                    isValid = false
                } else dialogBinding.titNota.error = null

                // Validar evaluación
                if (evaluacion == "Seleccionar...") {
                    dialogBinding.tvEvaluacionLabel.error = "Seleccione una evaluación válida"
                    isValid = false
                } else dialogBinding.tvEvaluacionLabel.error = null

                // Validar estudiante
                if (estudianteNombres.isEmpty() || estudianteNombres[0] == "No hay estudiantes") {
                    isValid = false
                } else if (estudianteIndex < 0) {
                    isValid = false
                }

                if (!isValid) return@setOnClickListener

                val estudianteId = estudianteIds[estudianteIndex]
                val asignaturaId = asignaturaIds[asignaturaIndex]

                db.collection("calificaciones")
                    .whereEqualTo("estudianteId", estudianteId)
                    .whereEqualTo("asignaturaId", asignaturaId)
                    .whereEqualTo("evaluacion", evaluacion)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            Toast.makeText(
                                requireContext(),
                                "Ya existe una calificacion de '$evaluacion' para este estudiante en esta asignatura",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val calificacion = Calificaciones(
                                id = db.collection("calificaciones").document().id,
                                asignaturaId = asignaturaIds[asignaturaIndex],
                                nota = nota,
                                evaluacion = evaluacion,
                                fechaRegistro = sdf.format(Date()),
                                estudianteId = estudianteId
                            )

                            db.collection("calificaciones").document(calificacion.id)
                                .set(calificacion)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Error al guardar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            requireContext(),
                            "Error al validar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }

        alertDialog.show()
    }

    private fun showEditDialog(calificacion: Calificaciones) {
        val dialogBinding = FormCalificacionesBinding.inflate(layoutInflater)

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

        // Cargar asignaturas
        cargarAsignaturas {
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

                val nota = dialogBinding.etNota.text.toString().toDoubleOrNull()
                val evaluacion = dialogBinding.spnEvaluacion.selectedItem?.toString() ?: ""
                val asignaturaIndex = dialogBinding.spnAsignatura.selectedItemPosition
                val estudianteIndex = dialogBinding.spnEstudiante.selectedItemPosition

                var isValid = true

                if (nota == null || nota < 0.0 || nota > 20.0) {
                    dialogBinding.titNota.error = "Ingrese una nota válida (0-20)"
                    isValid = false
                } else dialogBinding.titNota.error = null

                if (evaluacion == "Seleccionar...") {
                    dialogBinding.tvEvaluacionLabel.error = "Seleccione una evaluación válida"
                    isValid = false
                } else dialogBinding.tvEvaluacionLabel.error = null

                if (estudianteNombres.isEmpty() || estudianteNombres[0] == "No hay estudiantes") {
                    isValid = false
                } else if (estudianteIndex < 0) {
                    isValid = false
                }

                if (!isValid) return@setOnClickListener

                val estudianteId = estudianteIds[estudianteIndex]
                val updates = mapOf(
                    "nota" to nota,
                    "evaluacion" to evaluacion,
                    "asignaturaId" to asignaturaIds[asignaturaIndex],
                    "estudianteId" to estudianteId
                )

                db.collection("calificaciones").document(calificacion.id)
                    .update(updates)
                    .addOnSuccessListener { alertDialog.dismiss() }
            }
        }

        alertDialog.show()
    }

    private fun eliminarCalificacion(calificacion: Calificaciones) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Calificación")
            .setMessage("¿Deseas eliminar esta calificación?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("calificaciones").document(calificacion.id)
                    .delete()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _b = null
    }
}
