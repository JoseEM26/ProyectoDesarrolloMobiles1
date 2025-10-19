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

    // Para almacenar los profesores disponibles (nombre y ID)
    private val profesorNombres = mutableListOf<String>()
    private val profesorIds = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityAsignaturaBinding.inflate(inflater, container, false)

        b.rvAsginaturas.layoutManager = LinearLayoutManager(requireContext())
        b.rvAsginaturas.adapter = adapter
        b.rvAsginaturas.addItemDecoration(
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
                    toast("Error al cargar asignaturas: ${e.message}")
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
            .whereEqualTo("tipo", "profesor") // asegúrate que coincida con Firestore
            .get()
            .addOnSuccessListener { result ->
                for (doc in result.documents) { // ✅ usa .documents
                    val usuario = doc.toObject(Usuario::class.java)
                    if (usuario != null) {
                        profesorNombres.add(usuario.nombre)
                        profesorIds.add(doc.id)
                    }
                }
                toast("✅ Profesores cargados: ${profesorNombres.size}")
                onLoaded()
            }
            .addOnFailureListener {
                toast("❌ Error al cargar profesores: ${it.message}")
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

        AlertDialog.Builder(requireContext())
            .setTitle("Registrar Asignatura")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.etNombre.text.toString().trim()
                val codigo = dialogBinding.etCodigoAsignatura.text.toString().trim()
                val descripcion = dialogBinding.etDescripcion.text.toString().trim()
                val creditos = dialogBinding.etCreditos.text.toString().toIntOrNull() ?: 3
                val profesorIndex = dialogBinding.spnProfesor.selectedItemPosition

                if (nombre.isEmpty() || codigo.isEmpty()) {
                    toast("⚠️ Completa los campos obligatorios")
                    return@setPositiveButton
                }

                val asignatura = Asignatura(
                    id=db.collection("asignaturas").document().id,
                    codigoAsignatura = codigo,
                    nombre = nombre,
                    descripcion = descripcion,
                    creditos = creditos,
                    profesorId = profesorIds.getOrNull(profesorIndex)
                )

                db.collection("asignaturas")
                    .add(asignatura)
                    .addOnSuccessListener { toast("✅ Asignatura registrada") }
                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Asignatura")
            .setView(dialogBinding.root)
            .setPositiveButton("Actualizar") { _, _ ->
                val updates = mapOf(
                    "nombre" to dialogBinding.etNombre.text.toString().trim(),
                    "codigoAsignatura" to dialogBinding.etCodigoAsignatura.text.toString().trim(),
                    "descripcion" to dialogBinding.etDescripcion.text.toString().trim(),
                    "creditos" to (dialogBinding.etCreditos.text.toString().toIntOrNull() ?: 3),
                    "profesorId" to profesorIds.getOrNull(dialogBinding.spnProfesor.selectedItemPosition)
                )

                db.collection("asignaturas").document(asignatura.id)
                    .update(updates)
                    .addOnSuccessListener { toast("✅ Asignatura actualizada") }
                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarAsignatura(asignatura: Asignatura) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Asignatura")
            .setMessage("¿Deseas eliminar la asignatura ${asignatura.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("asignaturas").document(asignatura.id)
                    .delete()
                    .addOnSuccessListener { toast("🗑️ Asignatura eliminada") }
                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _b = null
    }
}
