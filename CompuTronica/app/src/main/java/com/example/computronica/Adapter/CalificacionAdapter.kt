package com.example.computronica.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Calificaciones
import com.example.computronica.R
import com.example.computronica.databinding.ItemCalificacionesBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class CalificacionAdapter(
    private val items: MutableList<Calificaciones>,
    private val onEdit: (Calificaciones) -> Unit,
    private val onDelete: (Calificaciones) -> Unit
) : RecyclerView.Adapter<CalificacionAdapter.VH>() {

    inner class VH(val binding: ItemCalificacionesBinding) : RecyclerView.ViewHolder(binding.root)
    private val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("es", "PE"))
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCalificacionesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.binding.root.context
        val u = items[position]
        val b = holder.binding

//        // Mostrar nota y tipo de evaluación con fecha
//        b.txtNotaCurso.text = u.nota.toString()
//        b.txtTipoAsignaturaFecha.text = "${u.evaluacion} - ${df.format(u.fechaRegistro)}"
//
//        // Mostrar color dependiendo de la nota
//        val colorRes = if (u.nota > 13) R.color.verde else R.color.rojo
//        b.txtNotaCurso.setTextColor(ContextCompat.getColor(context, colorRes))
//
//        // 🔹 Mostrar nombre de la asignatura (buscarlo en Firestore)
//        obtenerNombreAsignatura(u.asignaturaId) { nombre ->
//            b.txtNombreCurso.text = nombre ?: "Asignatura desconocida"
//        }
//
//        // 🔹 PopupMenu de opciones (editar / eliminar)
//        b.btnMenuCalificaicon.setOnClickListener { view ->
//            val popup = PopupMenu(context, view)
//            popup.menuInflater.inflate(R.menu.menu_usuario, popup.menu)
//
//            // Forzar íconos visibles en el menú
//            try {
//                val fields = popup.javaClass.getDeclaredField("mPopup")
//                fields.isAccessible = true
//                val menuHelper = fields.get(popup)
//                val setForceIcons = menuHelper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
//                setForceIcons.invoke(menuHelper, true)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//
//            popup.setOnMenuItemClickListener { item ->
//                when (item.itemId) {
//                    R.id.action_editar -> onEdit(u)
//                    R.id.action_eliminar -> {
//                        AlertDialog.Builder(context)
//                            .setTitle("Eliminar calificación")
//                            .setMessage("¿Seguro que deseas eliminar esta calificación de ${b.txtNombreCurso.text}?")
//                            .setPositiveButton("Eliminar") { _, _ ->
//                                db.collection("calificaciones")
//                                    .document(u.id)
//                                    .delete()
//                                    .addOnSuccessListener {
//                                        Toast.makeText(context, "✅ Calificación eliminada", Toast.LENGTH_SHORT).show()
//                                        onDelete(u)
//                                    }
//                                    .addOnFailureListener {
//                                        Toast.makeText(context, "❌ Error al eliminar", Toast.LENGTH_SHORT).show()
//                                    }
//                            }
//                            .setNegativeButton("Cancelar", null)
//                            .show()
//                    }
//                }
//                true
//            }
//            popup.show()
//        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newCalificaciones: List<Calificaciones>) {
        items.clear()
        items.addAll(newCalificaciones)
        notifyDataSetChanged()
    }

    private fun obtenerNombreAsignatura(asignaturaId: String, callback: (String?) -> Unit) {
        db.collection("asignaturas")
            .document(asignaturaId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val nombre = doc.getString("nombre") // asegúrate que el campo se llame "nombre"
                    callback(nombre)
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
