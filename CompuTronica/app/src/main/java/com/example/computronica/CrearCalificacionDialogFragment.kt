package com.example.computronica

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.computronica.databinding.FormCalificacionesBinding
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Usuario
import com.example.computronica.Model.TipoUsuario
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CrearCalificacionDialogFragment : DialogFragment() {

    private var _binding: FormCalificacionesBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    private var asignaturas = mutableListOf<Asignatura>()
    private var estudiantes = mutableListOf<Usuario>()
    private var asignaturasAdapter: ArrayAdapter<String>? = null
    private var estudiantesAdapter: ArrayAdapter<String>? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FormCalificacionesBinding.inflate(LayoutInflater.from(requireContext()))

        setupSpinners()
        cargarDatos()

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .setTitle("Agregar Nueva Calificación")
            .setPositiveButton("Guardar", null) // ← Poner null aquí para manejar manualmente
            .setNegativeButton("Cancelar") { dialog, which ->
                dialog.dismiss()
            }
            .create()

        // Manejar el click del botón positivo manualmente
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                guardarCalificacion()
            }
        }

        return dialog
    }

    private fun setupSpinners() {
        // Configurar spinner de tipos de evaluación
        val tiposEvaluacion = resources.getStringArray(R.array.tipoAsignatura)
        val tiposAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tiposEvaluacion)
        tiposAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnEvaluacion.adapter = tiposAdapter

        // Configurar spinner de asignaturas
        asignaturasAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf("Cargando..."))
        asignaturasAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnAsignatura.adapter = asignaturasAdapter

        // Configurar spinner de estudiantes
        estudiantesAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf("Cargando..."))
        estudiantesAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnEstudiante.adapter = estudiantesAdapter

        // Cuando se selecciona una asignatura, cargar sus estudiantes
        binding.spnAsignatura.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (position >= 0 && position < asignaturas.size) {
                    val asignaturaSeleccionada = asignaturas[position]
                    scope.launch {
                        try {
                            cargarEstudiantesDeAsignatura(asignaturaSeleccionada.id)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al cargar estudiantes", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun cargarDatos() {
        val currentUser = SessionManager.currentUser

        scope.launch {
            try {
                binding.spnAsignatura.isEnabled = false
                binding.spnEstudiante.isEnabled = false
                binding.spnEvaluacion.isEnabled = false

                when (currentUser?.tipo) {
                    TipoUsuario.profesor -> {
                        cargarCursosDelProfesor(currentUser.id)
                    }
                    TipoUsuario.administrativo -> {
                        cargarTodosLosCursos()
                    }
                    else -> {
                        Toast.makeText(requireContext(), "No tienes permisos para agregar calificaciones", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }

                binding.spnAsignatura.isEnabled = true
                binding.spnEstudiante.isEnabled = true
                binding.spnEvaluacion.isEnabled = true

            } catch (e: Exception) {
                Log.e("CrearCalificacion", "Error cargando datos: ${e.message}", e)
                Toast.makeText(requireContext(), "Error al cargar datos", Toast.LENGTH_SHORT).show()
                binding.spnAsignatura.isEnabled = true
                binding.spnEstudiante.isEnabled = true
                binding.spnEvaluacion.isEnabled = true
            }
        }
    }

    private suspend fun cargarCursosDelProfesor(profesorId: String) {
        try {
            val snapshot = db.collection("asignaturas")
                .whereArrayContains("profesores", profesorId)
                .get()
                .await()

            asignaturas = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Asignatura::class.java)?.copy(id = doc.id)
            }.toMutableList()

            actualizarSpinnerAsignaturas()

            // Cargar estudiantes de la primera asignatura si existe
            if (asignaturas.isNotEmpty()) {
                cargarEstudiantesDeAsignatura(asignaturas.first().id)
            } else {
                estudiantesAdapter?.clear()
                estudiantesAdapter?.add("No hay asignaturas disponibles")
                estudiantesAdapter?.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun cargarTodosLosCursos() {
        try {
            val snapshot = db.collection("asignaturas")
                .get()
                .await()

            asignaturas = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Asignatura::class.java)?.copy(id = doc.id)
            }.toMutableList()

            actualizarSpinnerAsignaturas()

            if (asignaturas.isNotEmpty()) {
                cargarEstudiantesDeAsignatura(asignaturas.first().id)
            } else {
                estudiantesAdapter?.clear()
                estudiantesAdapter?.add("No hay asignaturas disponibles")
                estudiantesAdapter?.notifyDataSetChanged()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun cargarEstudiantesDeAsignatura(asignaturaId: String) {
        try {
            val asignaturaDoc = db.collection("asignaturas")
                .document(asignaturaId)
                .get()
                .await()

            val asignatura = asignaturaDoc.toObject(Asignatura::class.java)
            val estudiantesIds = asignatura?.estudiantes ?: emptyList()

            estudiantes.clear()
            for (estudianteId in estudiantesIds) {
                val estudianteDoc = db.collection("usuarios")
                    .document(estudianteId)
                    .get()
                    .await()

                val estudiante = estudianteDoc.toObject(Usuario::class.java)?.copy(id = estudianteDoc.id)
                if (estudiante != null) {
                    estudiantes.add(estudiante)
                }
            }

            actualizarSpinnerEstudiantes()
        } catch (e: Exception) {
            throw e
        }
    }

    private fun actualizarSpinnerAsignaturas() {
        val nombresAsignaturas = if (asignaturas.isNotEmpty()) {
            asignaturas.map { it.nombre }
        } else {
            listOf("No hay asignaturas disponibles")
        }

        asignaturasAdapter?.clear()
        asignaturasAdapter?.addAll(nombresAsignaturas)
        asignaturasAdapter?.notifyDataSetChanged()
    }

    private fun actualizarSpinnerEstudiantes() {
        val nombresEstudiantes = if (estudiantes.isNotEmpty()) {
            estudiantes.map { "${it.nombre} ${it.apellido} - ${it.codigoInstitucional}" }
        } else {
            listOf("No hay estudiantes en esta asignatura")
        }

        estudiantesAdapter?.clear()
        estudiantesAdapter?.addAll(nombresEstudiantes)
        estudiantesAdapter?.notifyDataSetChanged()
    }

    private fun guardarCalificacion() {
        try {
            val asignaturaPos = binding.spnAsignatura.selectedItemPosition
            val estudiantePos = binding.spnEstudiante.selectedItemPosition
            val evaluacion = binding.spnEvaluacion.selectedItem.toString()
            val notaText = binding.etNota.text.toString()

            // Validaciones
            if (asignaturas.isEmpty() || estudiantes.isEmpty()) {
                Toast.makeText(requireContext(), "No hay datos disponibles para guardar", Toast.LENGTH_SHORT).show()
                return
            }

            if (asignaturaPos < 0 || estudiantePos < 0 ||
                asignaturaPos >= asignaturas.size || estudiantePos >= estudiantes.size) {
                Toast.makeText(requireContext(), "Por favor seleccione una asignatura y un estudiante válidos", Toast.LENGTH_SHORT).show()
                return
            }

            if (evaluacion == "Seleccionar...") {
                Toast.makeText(requireContext(), "Por favor seleccione un tipo de evaluación", Toast.LENGTH_SHORT).show()
                return
            }

            if (notaText.isEmpty()) {
                Toast.makeText(requireContext(), "Por favor ingrese una nota", Toast.LENGTH_SHORT).show()
                return
            }

            val nota = notaText.toDoubleOrNull()
            if (nota == null || nota < 0 || nota > 20) {
                Toast.makeText(requireContext(), "La nota debe ser un número entre 0 y 20", Toast.LENGTH_SHORT).show()
                return
            }

            val asignaturaSeleccionada = asignaturas[asignaturaPos]
            val estudianteSeleccionado = estudiantes[estudiantePos]

            // Crear objeto calificación
            val calificacion = hashMapOf(
                "estudianteId" to estudianteSeleccionado.id,
                "asignaturaId" to asignaturaSeleccionada.id,
                "evaluacion" to evaluacion,
                "nota" to nota,
                "fecha" to Timestamp.now()
            )

            // Guardar en Firestore
            db.collection("calificaciones")
                .add(calificacion)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "✅ Calificación guardada exitosamente", Toast.LENGTH_SHORT).show()
                    // Cerrar el diálogo de forma segura
                    dismissAllowingStateLoss()
                }
                .addOnFailureListener { e ->
                    Log.e("CrearCalificacion", "Error al guardar calificación: ${e.message}", e)
                    Toast.makeText(requireContext(), "❌ Error al guardar calificación", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e("CrearCalificacion", "Error inesperado: ${e.message}", e)
            Toast.makeText(requireContext(), "Error inesperado al guardar", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}