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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class CalificacionActivity : Fragment() {
//
//    private var _b: ActivityCalificacionBinding? = null
//    private val b get() = _b!!
//    private val db by lazy { FirebaseFirestore.getInstance() }
//    private var listenerReg: ListenerRegistration? = null
//    private var formatDay=SimpleDateFormat("dd/MM/yyyy hh:mm")
//
//    private val adapter = CalificacionAdapter(
//        mutableListOf(),
//        onEdit = { calificacion -> showEditDialog(calificacion) },
//        onDelete = { calificacion -> eliminarCalificacion(calificacion) }
//    )
//
//    private val asignaturaNombres = mutableListOf<String>()
//    private val asignaturaIds = mutableListOf<String>()
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _b = ActivityCalificacionBinding.inflate(inflater, container, false)
//
//        b.rvCalificaciones.layoutManager = LinearLayoutManager(requireContext())
//        b.rvCalificaciones.adapter = adapter
//        b.rvCalificaciones.addItemDecoration(
//            DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
//        )
//
//        b.btnCalificaionesCreate.setOnClickListener { showCreateDialog() }
//
//        return b.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        listarCalificaciones()
//    }
//
//    private fun listarCalificaciones() {
//        listenerReg?.remove()
//        listenerReg = db.collection("calificaciones")
//            .orderBy("fechaRegistro", Query.Direction.DESCENDING)
//            .addSnapshotListener { snapshot, e ->
//                if (e != null) {
//                    toast("Error al cargar calificaciones: ${e.message}")
//                    return@addSnapshotListener
//                }
//
//                val list = snapshot?.documents?.mapNotNull { doc ->
//                    doc.toObject(Calificaciones::class.java)?.copy(id = doc.id)
//                }.orEmpty()
//
//                adapter.replaceAll(list)
//                b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
//            }
//    }
//
//    private fun cargarAsignaturas(onLoaded: () -> Unit) {
//        asignaturaNombres.clear()
//        asignaturaIds.clear()
//
//        db.collection("asignaturas")
//            .orderBy("nombre", Query.Direction.ASCENDING)
//            .get()
//            .addOnSuccessListener { result ->
//                for (doc in result.documents) {
//                    val asig = doc.toObject(Asignatura::class.java)
//                    if (asig != null) {
//                        asignaturaNombres.add(asig.nombre)
//                        asignaturaIds.add(doc.id)
//                    }
//                }
//                toast("✅ Asignaturas cargadas: ${asignaturaNombres.size}")
//                onLoaded()
//            }
//            .addOnFailureListener {
//                toast("❌ Error al cargar asignaturas: ${it.message}")
//            }
//    }
//
//    private fun showCreateDialog() {
//        val dialogBinding = FormCalificacionesBinding.inflate(layoutInflater)
//
//        cargarAsignaturas {
//            dialogBinding.spnAsignatura.adapter = ArrayAdapter(
//                requireContext(),
//                android.R.layout.simple_spinner_dropdown_item,
//                asignaturaNombres
//            )
//        }
//
//        AlertDialog.Builder(requireContext())
//            .setTitle("Registrar Calificación")
//            .setView(dialogBinding.root)
//            .setPositiveButton("Guardar") { _, _ ->
//                val nota = dialogBinding.etNota.text.toString().toDoubleOrNull()
//                val evaluacion = dialogBinding.spnEvaluacion.selectedItemPosition
//                val asignaturaIndex = dialogBinding.spnAsignatura.selectedItemPosition
//
//                if (nota == null || evaluacion < 0 || asignaturaIndex < 0) {
//                    toast("⚠️ Completa todos los campos correctamente")
//                    return@setPositiveButton
//                }
//
//                val calificacion = Calificaciones(
//                    id = db.collection("calificaciones").document().id,
//                    asignaturaId = asignaturaIds[asignaturaIndex],
//                    nota = nota,
//                    evaluacion = evaluacion.toString(),
//                    fechaRegistro = formatDay.format(System.currentTimeMillis()),
//                    estudianteId=db.collection("usuarios").where()
//                )
//
//                db.collection("calificaciones")
//                    .add(calificacion)
//                    .addOnSuccessListener { toast("✅ Calificación registrada") }
//                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
//            }
//            .setNegativeButton("Cancelar", null)
//            .show()
//    }
//
//    private fun showEditDialog(calificacion: Calificaciones) {
//        val dialogBinding = FormCalificacionesBinding.inflate(layoutInflater)
//
//        dialogBinding.etNota.setText(calificacion.nota.toString())
//        dialogBinding.spnEvaluacion.isSelected(calificacion.evaluacion)
//
//        cargarAsignaturas {
//            dialogBinding.spnAsignatura.adapter = ArrayAdapter(
//                requireContext(),
//                android.R.layout.simple_spinner_dropdown_item,
//                asignaturaNombres
//            )
//            val selectedIndex = asignaturaIds.indexOf(calificacion.asignaturaId)
//            if (selectedIndex >= 0) dialogBinding.spnAsignatura.setSelection(selectedIndex)
//        }
//
//        AlertDialog.Builder(requireContext())
//            .setTitle("Editar Calificación")
//            .setView(dialogBinding.root)
//            .setPositiveButton("Actualizar") { _, _ ->
//                val nota = dialogBinding.etNota.text.toString().toDoubleOrNull()
//                val evaluacion = dialogBinding.spnEvaluacion.isSelected
//                val asignaturaIndex = dialogBinding.spnAsignatura.selectedItemPosition
//
//                if (nota == null || evaluacion< 0 || asignaturaIndex < 0) {
//                    toast("⚠️ Completa todos los campos correctamente")
//                    return@setPositiveButton
//                }
//
//                val updates = mapOf(
//                    "nota" to nota,
//                    "evaluacion" to evaluacion,
//                    "asignaturaId" to asignaturaIds[asignaturaIndex]
//                )
//
//                db.collection("calificaciones").document(calificacion.id)
//                    .update(updates)
//                    .addOnSuccessListener { toast("✅ Calificación actualizada") }
//                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
//            }
//            .setNegativeButton("Cancelar", null)
//            .show()
//    }
//
//    private fun eliminarCalificacion(calificacion: Calificaciones) {
//        AlertDialog.Builder(requireContext())
//            .setTitle("Eliminar Calificación")
//            .setMessage("¿Deseas eliminar esta calificación?")
//            .setPositiveButton("Eliminar") { _, _ ->
//                db.collection("calificaciones").document(calificacion.id)
//                    .delete()
//                    .addOnSuccessListener { toast("🗑️ Calificación eliminada") }
//                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
//            }
//            .setNegativeButton("Cancelar", null)
//            .show()
//    }
//
//    private fun toast(msg: String) =
//        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        listenerReg?.remove()
//        _b = null
//    }
}
