package com.example.computronica.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.R
import com.example.computronica.SessionManager
import com.example.computronica.databinding.ItemCalificacionesBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CalificacionAdapter(
    private val items: MutableList<Calificaciones>,
    private val onEdit: (Calificaciones) -> Unit,
    private val onDelete: (Calificaciones) -> Unit
) : RecyclerView.Adapter<CalificacionAdapter.VH>() {

    inner class VH(val binding: ItemCalificacionesBinding) : RecyclerView.ViewHolder(binding.root)

    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "PE"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCalificacionesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.binding.root.context
        val item = items[position]
        val b = holder.binding
        val usuario: Usuario? = SessionManager.currentUser
        b.btnMenuCalificaicon.isInvisible = usuario?.tipo == TipoUsuario.estudiante

        b.txtNotaCurso.text = item.nota.toString()

        val fechaTexto =item.fechaRegistro

        b.txtTipoAsignaturaFecha.text = "${item.evaluacion} - $fechaTexto"

        // 🔹 Cambiar color del texto según nota
        val colorRes = if (item.nota >= 14) R.color.verde else R.color.rojo
        b.txtNotaCurso.setTextColor(ContextCompat.getColor(context, colorRes))

        // 🔹 Obtener y mostrar el nombre de la asignatura
        obtenerNombreAsignatura(item.asignaturaId) { nombre ->
            b.txtNombreCurso.text = nombre ?: "Asignatura desconocida"
        }

        // 🔹 Menú contextual (editar / eliminar)
        b.btnMenuCalificaicon.setOnClickListener { view ->
            val popup = PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.menu_usuario, popup.menu)
            // Disable or hide delete option for non-admins
            if (usuario?.tipo != TipoUsuario.administrativo) {
                popup.menu.findItem(R.id.action_eliminar)?.isVisible = false
            }
            // Forzar íconos visibles
            try {
                val fields = popup.javaClass.getDeclaredField("mPopup")
                fields.isAccessible = true
                val menuHelper = fields.get(popup)
                val setForceIcons = menuHelper.javaClass.getDeclaredMethod(
                    "setForceShowIcon",
                    Boolean::class.javaPrimitiveType
                )
                setForceIcons.invoke(menuHelper, true)
            } catch (_: Exception) {}

            popup.setOnMenuItemClickListener { itemMenu ->
                when (itemMenu.itemId) {
                    R.id.action_editar -> onEdit(item)
                    R.id.action_eliminar -> {
                        AlertDialog.Builder(context)
                            .setTitle("Eliminar calificación")
                            .setMessage("¿Deseas eliminar la calificación de ${b.txtNombreCurso.text}?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                db.collection("calificaciones")
                                    .document(item.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "✅ Calificación eliminada", Toast.LENGTH_SHORT).show()
                                        onDelete(item)
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "❌ Error al eliminar", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .setNegativeButton("Cancelar", null)
                            .show()
                    }
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newItems: List<Calificaciones>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    private fun obtenerNombreAsignatura(asignaturaId: String, callback: (String?) -> Unit) {
        db.collection("asignaturas")
            .document(asignaturaId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) callback(doc.getString("nombre"))
                else callback(null)
            }
            .addOnFailureListener { callback(null) }
    }
}
