package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Adapter.AsignaturasAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityAsignaturaBinding
import com.example.computronica.databinding.FormAsignaturaBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AsignaturaActivity : Fragment() {

    private var _b: ActivityAsignaturaBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listenerReg: ListenerRegistration? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val adapter = AsignaturasAdapter(
        mutableListOf(),
        onEdit = { asignatura -> showEditDialog(asignatura) },
        onDelete = { asignatura -> eliminarAsignatura(asignatura) }
    )

    private val profesorNombres = mutableListOf<String>()
    private val profesorIds = mutableListOf<String>()
    private val estudianteNombres = mutableListOf<String>()
    private val estudianteIds = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityAsignaturaBinding.inflate(inflater, container, false)

        b.rvAsignaturas.layoutManager = LinearLayoutManager(requireContext())
        b.rvAsignaturas.adapter = adapter
        b.rvAsignaturas.addItemDecoration(
            DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        )

        b.btnCalificaionesCreate.setOnClickListener { showCreateDialog() }

        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val usuario: Usuario? = SessionManager.currentUser

        b.btnCalificaionesCreate.isVisible = usuario?.tipo == TipoUsuario.administrativo
        listarAsignaturas()
    }

    private fun listarAsignaturas() {
        listenerReg?.remove()
        val currentUser = SessionManager.currentUser
        if (currentUser == null) {
            Log.e("AsignaturaActivity", "SessionManager.currentUser is null")
            b.tvEmpty.text = "Error: No hay usuario autenticado"
            b.tvEmpty.visibility = View.VISIBLE
            return
        }

        val query = when (currentUser.tipo) {
            TipoUsuario.estudiante -> db.collection("asignaturas")
                .whereArrayContains("estudiantes", currentUser.id)
                .orderBy("nombre", Query.Direction.ASCENDING)
            TipoUsuario.profesor -> db.collection("asignaturas")
                .whereArrayContains("profesores", currentUser.id)
                .orderBy("nombre", Query.Direction.ASCENDING)
            TipoUsuario.administrativo -> db.collection("asignaturas")
                .orderBy("nombre", Query.Direction.ASCENDING)
        }

        listenerReg = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("AsignaturaActivity", "Error loading asignaturas: ${e.message}", e)
                b.tvEmpty.text = "Error al cargar asignaturas: ${e.message}"
                b.tvEmpty.visibility = View.VISIBLE
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Asignatura::class.java)?.copy(id = doc.id)
            }.orEmpty()

            adapter.replaceAll(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun cargarProfesores(onLoaded: () -> Unit) {
        profesorNombres.clear()
        profesorIds.clear()

        db.collection("usuarios")
            .whereEqualTo("tipo", "profesor")
            .whereEqualTo("estado", true)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) {
                    val usuario = doc.toObject(Usuario::class.java)
                    if (usuario != null) {
                        profesorNombres.add("${usuario.nombre} ${usuario.apellido}".trim())
                        profesorIds.add(doc.id)
                    }
                }
                onLoaded()
            }
            .addOnFailureListener { e ->
                Log.e("AsignaturaActivity", "Error loading profesores: ${e.message}", e)
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
                for (doc in result.documents) {
                    val usuario = doc.toObject(Usuario::class.java)
                    if (usuario != null) {
                        estudianteNombres.add("${usuario.nombre} ${usuario.apellido}".trim())
                        estudianteIds.add(doc.id)
                    }
                }
                onLoaded()
            }
            .addOnFailureListener { e ->
                Log.e("AsignaturaActivity", "Error loading estudiantes: ${e.message}", e)
            }
    }

    private suspend fun isCodigoAsignaturaUnique(codigo: String, excludeId: String? = null): Boolean {
        val query = db.collection("asignaturas").whereEqualTo("codigoAsignatura", codigo)
        val snapshot = query.get().await()
        return snapshot.documents.all { it.id != excludeId }
    }

    private suspend fun areUsersValid(ids: List<String>, tipo: String): Boolean {
        if (ids.isEmpty()) return true
        val snapshot = db.collection("usuarios")
            .whereIn("id", ids)
            .whereEqualTo("tipo", tipo)
            .whereEqualTo("estado", true)
            .get()
            .await()
        return snapshot.documents.size == ids.size
    }

    private fun showCreateDialog() {
        val dialogBinding = FormAsignaturaBinding.inflate(layoutInflater)

        cargarProfesores {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                profesorNombres
            )
            dialogBinding.etProfesores.setAdapter(adapter)
            dialogBinding.etProfesores.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
            dialogBinding.etProfesores.threshold = 1 // Show suggestions after 1 character
        }

        cargarEstudiantes {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                estudianteNombres
            )
            dialogBinding.etEstudiantes.setAdapter(adapter)
            dialogBinding.etEstudiantes.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
            dialogBinding.etEstudiantes.threshold = 1 // Show suggestions after 1 character
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Registrar Asignatura")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Limpiar errores
            dialogBinding.tilNombre.error = null
            dialogBinding.tilCodigoAsignatura.error = null
            dialogBinding.tilDescripcion.error = null
            dialogBinding.tilCreditos.error = null
            dialogBinding.tilProfesores.error = null
            dialogBinding.tilEstudiantes.error = null

            val nombre = dialogBinding.etNombre.text.toString().trim()
            val codigo = dialogBinding.etCodigoAsignatura.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()
            val creditos = dialogBinding.etCreditos.text.toString().toIntOrNull()
            val profesoresInput = dialogBinding.etProfesores.text.toString().trim()
            val profesoresSeleccionados = profesoresInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val profesoresIds = profesoresSeleccionados.mapNotNull { nombre ->
                val index = profesorNombres.indexOf(nombre)
                if (index >= 0) profesorIds[index] else null
            }
            val estudiantesInput = dialogBinding.etEstudiantes.text.toString().trim()
            val estudiantesSeleccionados = estudiantesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val estudiantesIds = estudiantesSeleccionados.mapNotNull { nombre ->
                val index = estudianteNombres.indexOf(nombre)
                if (index >= 0) estudianteIds[index] else null
            }

            var valid = true

            if (nombre.isEmpty()) {
                dialogBinding.tilNombre.error = "Ingrese el nombre"
                valid = false
            }
            if (codigo.isEmpty()) {
                dialogBinding.tilCodigoAsignatura.error = "Ingrese el código"
                valid = false
            }
            if (descripcion.isEmpty()) {
                dialogBinding.tilDescripcion.error = "Ingrese una descripción"
                valid = false
            }
            if (creditos == null || creditos <= 0) {
                dialogBinding.tilCreditos.error = "Ingrese un número válido de créditos"
                valid = false
            }
            if (profesoresSeleccionados.isEmpty()) {
                dialogBinding.tilProfesores.error = "Seleccione al menos un profesor"
                valid = false
            }
            if (estudiantesSeleccionados.isEmpty()) {
                dialogBinding.tilEstudiantes.error = "Seleccione al menos un estudiante"
                valid = false
            }

            if (!valid) return@setOnClickListener

            scope.launch {
                try {
                    // Validate unique codigoAsignatura
                    if (!isCodigoAsignaturaUnique(codigo)) {
                        dialogBinding.tilCodigoAsignatura.error = "El código ya existe"
                        return@launch
                    }

                    // Validate profesores and estudiantes exist and are active
                    if (!areUsersValid(profesoresIds, "profesor")) {
                        dialogBinding.tilProfesores.error = "Uno o más profesores no son válidos"
                        return@launch
                    }
                    if (!areUsersValid(estudiantesIds, "estudiante")) {
                        dialogBinding.tilEstudiantes.error = "Uno o más estudiantes no son válidos"
                        return@launch
                    }

                    // Create Asignatura in a transaction
                    val asignatura = Asignatura(
                        id = db.collection("asignaturas").document().id,
                        codigoAsignatura = codigo,
                        nombre = nombre,
                        descripcion = descripcion,
                        creditos = creditos!!,
                        profesores = profesoresIds,
                        estudiantes = estudiantesIds
                    )

                    db.runTransaction { transaction ->
                        val asignaturaRef = db.collection("asignaturas").document(asignatura.id)
                        transaction.set(asignaturaRef, asignatura)
                    }.await()

                    dialog.dismiss()
                } catch (e: Exception) {
                    dialogBinding.tilNombre.error = "Error al guardar: ${e.message}"
                    Log.e("AsignaturaActivity", "Error saving asignatura: ${e.message}", e)
                }
            }
        }
    }

    private fun showEditDialog(asignatura: Asignatura) {
        val dialogBinding = FormAsignaturaBinding.inflate(layoutInflater)

        dialogBinding.etNombre.setText(asignatura.nombre)
        dialogBinding.etCodigoAsignatura.setText(asignatura.codigoAsignatura)
        dialogBinding.etDescripcion.setText(asignatura.descripcion)
        dialogBinding.etCreditos.setText(asignatura.creditos.toString())

        cargarProfesores {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                profesorNombres
            )
            dialogBinding.etProfesores.setAdapter(adapter)
            dialogBinding.etProfesores.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
            dialogBinding.etProfesores.threshold = 1 // Show suggestions after 1 character

            // Pre-fill selected professors
            val profesoresNombres = asignatura.profesores.mapNotNull { id ->
                val index = profesorIds.indexOf(id)
                if (index >= 0) profesorNombres[index] else null
            }
            dialogBinding.etProfesores.setText(profesoresNombres.joinToString(", "))
        }

        cargarEstudiantes {
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                estudianteNombres
            )
            dialogBinding.etEstudiantes.setAdapter(adapter)
            dialogBinding.etEstudiantes.setTokenizer(MultiAutoCompleteTextView.CommaTokenizer())
            dialogBinding.etEstudiantes.threshold = 1 // Show suggestions after 1 character

            // Pre-fill selected students
            val estudiantesNombres = asignatura.estudiantes.mapNotNull { id ->
                val index = estudianteIds.indexOf(id)
                if (index >= 0) estudianteNombres[index] else null
            }
            dialogBinding.etEstudiantes.setText(estudiantesNombres.joinToString(", "))
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Editar Asignatura")
            .setView(dialogBinding.root)
            .setPositiveButton("Actualizar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Limpiar errores
            dialogBinding.tilNombre.error = null
            dialogBinding.tilCodigoAsignatura.error = null
            dialogBinding.tilDescripcion.error = null
            dialogBinding.tilCreditos.error = null
            dialogBinding.tilProfesores.error = null
            dialogBinding.tilEstudiantes.error = null

            val nombre = dialogBinding.etNombre.text.toString().trim()
            val codigo = dialogBinding.etCodigoAsignatura.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()
            val creditos = dialogBinding.etCreditos.text.toString().toIntOrNull()
            val profesoresInput = dialogBinding.etProfesores.text.toString().trim()
            val profesoresSeleccionados = profesoresInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val profesoresIds = profesoresSeleccionados.mapNotNull { nombre ->
                val index = profesorNombres.indexOf(nombre)
                if (index >= 0) profesorIds[index] else null
            }
            val estudiantesInput = dialogBinding.etEstudiantes.text.toString().trim()
            val estudiantesSeleccionados = estudiantesInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val estudiantesIds = estudiantesSeleccionados.mapNotNull { nombre ->
                val index = estudianteNombres.indexOf(nombre)
                if (index >= 0) estudianteIds[index] else null
            }

            var valid = true

            if (nombre.isEmpty()) {
                dialogBinding.tilNombre.error = "Ingrese el nombre"
                valid = false
            }
            if (codigo.isEmpty()) {
                dialogBinding.tilCodigoAsignatura.error = "Ingrese el código"
                valid = false
            }
            if (descripcion.isEmpty()) {
                dialogBinding.tilDescripcion.error = "Ingrese una descripción"
                valid = false
            }
            if (creditos == null || creditos <= 0) {
                dialogBinding.tilCreditos.error = "Ingrese un número válido de créditos"
                valid = false
            }
            if (profesoresSeleccionados.isEmpty()) {
                dialogBinding.tilProfesores.error = "Seleccione al menos un profesor"
                valid = false
            }
            if (estudiantesSeleccionados.isEmpty()) {
                dialogBinding.tilEstudiantes.error = "Seleccione al menos un estudiante"
                valid = false
            }

            if (!valid) return@setOnClickListener

            scope.launch {
                try {
                    // Validate unique codigoAsignatura, but allow the current code
                    if (codigo != asignatura.codigoAsignatura && !isCodigoAsignaturaUnique(codigo, asignatura.id)) {
                        dialogBinding.tilCodigoAsignatura.error = "El código ya existe"
                        return@launch
                    }

                    // Validate profesores and estudiantes exist and are active
                    if (!areUsersValid(profesoresIds, "profesor")) {
                        dialogBinding.tilProfesores.error = "Uno o más profesores no son válidos"
                        return@launch
                    }
                    if (!areUsersValid(estudiantesIds, "estudiante")) {
                        dialogBinding.tilEstudiantes.error = "Uno o más estudiantes no son válidos"
                        return@launch
                    }

                    // Update Asignatura in a transaction
                    val updates = mapOf(
                        "nombre" to nombre,
                        "codigoAsignatura" to codigo,
                        "descripcion" to descripcion,
                        "creditos" to creditos!!,
                        "profesores" to profesoresIds,
                        "estudiantes" to estudiantesIds
                    )

                    db.runTransaction { transaction ->
                        val asignaturaRef = db.collection("asignaturas").document(asignatura.id)
                        transaction.update(asignaturaRef, updates)
                    }.await()

                    dialog.dismiss()
                } catch (e: Exception) {
                    dialogBinding.tilNombre.error = "Error al actualizar: ${e.message}"
                    Log.e("AsignaturaActivity", "Error updating asignatura: ${e.message}", e)
                }
            }
        }
    }

    private fun eliminarAsignatura(asignatura: Asignatura) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Asignatura")
            .setMessage("¿Deseas eliminar la asignatura ${asignatura.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("asignaturas").document(asignatura.id)
                    .delete()
                    .addOnFailureListener { e ->
                        b.tvEmpty.text = "Error al eliminar: ${e.message}"
                        Log.e("AsignaturaActivity", "Error deleting asignatura: ${e.message}", e)
                    }
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