package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Adapter.AsignaturasAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityAsignaturaBinding
import com.example.computronica.databinding.FormAsignaturaBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class AsignaturaActivity : Fragment() {

    private var _b: ActivityAsignaturaBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listenerReg: ListenerRegistration? = null

    private val adapter = AsignaturasAdapter(
        mutableListOf(),
        onEdit = { asignatura -> showEditDialog(asignatura) },
        onDelete = { asignatura -> eliminarAsignatura(asignatura) }
    )

    private val profesorNombres = mutableListOf<String>()
    private val profesorIds = mutableListOf<String>()

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
        listarAsignaturas()
    }

    private fun listarAsignaturas() {
        listenerReg?.remove()
        listenerReg = db.collection("asignaturas")
            .orderBy("nombre", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    b.tvEmpty.text = "Error al cargar asignaturas"
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
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) {
                    val usuario = doc.toObject(Usuario::class.java)
                    if (usuario != null) {
                        profesorNombres.add(usuario.nombre)
                        profesorIds.add(doc.id)
                    }
                }
                onLoaded()
            }
    }

    private fun showCreateDialog() {
        val dialogBinding = FormAsignaturaBinding.inflate(layoutInflater)

        cargarProfesores {
            dialogBinding.spnProfesor.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                profesorNombres
            )
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

            val nombre = dialogBinding.etNombre.text.toString().trim()
            val codigo = dialogBinding.etCodigoAsignatura.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()
            val creditos = dialogBinding.etCreditos.text.toString().toIntOrNull()
            val profesorIndex = dialogBinding.spnProfesor.selectedItemPosition

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

            if (!valid) return@setOnClickListener

            val asignatura = Asignatura(
                id = db.collection("asignaturas").document().id,
                codigoAsignatura = codigo,
                nombre = nombre,
                descripcion = descripcion,
                creditos = creditos!!,
                profesorId = profesorIds.getOrNull(profesorIndex)
            )

            db.collection("asignaturas")
                .add(asignatura)
                .addOnSuccessListener { dialog.dismiss() }
                .addOnFailureListener {
                    dialogBinding.tilNombre.error = "Error: ${it.message}"
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
            dialogBinding.spnProfesor.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                profesorNombres
            )
            val selectedIndex = profesorIds.indexOf(asignatura.profesorId)
            if (selectedIndex >= 0) dialogBinding.spnProfesor.setSelection(selectedIndex)
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

            val nombre = dialogBinding.etNombre.text.toString().trim()
            val codigo = dialogBinding.etCodigoAsignatura.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()
            val creditos = dialogBinding.etCreditos.text.toString().toIntOrNull()
            val profesorIndex = dialogBinding.spnProfesor.selectedItemPosition

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

            if (!valid) return@setOnClickListener

            val updates = mapOf(
                "nombre" to nombre,
                "codigoAsignatura" to codigo,
                "descripcion" to descripcion,
                "creditos" to creditos!!,
                "profesorId" to profesorIds.getOrNull(profesorIndex)
            )

            db.collection("asignaturas").document(asignatura.id)
                .update(updates)
                .addOnSuccessListener { dialog.dismiss() }
                .addOnFailureListener {
                    dialogBinding.tilNombre.error = "Error: ${it.message}"
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
