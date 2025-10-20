package com.example.computronica.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Asignatura
import com.example.computronica.R
import com.example.computronica.databinding.ItemAsignaturaBinding
import com.google.firebase.firestore.FirebaseFirestore

class AsignaturasAdapter(
    private val items: MutableList<Asignatura>,
    private val onEdit: (Asignatura) -> Unit,
    private val onDelete: (Asignatura) -> Unit
) : RecyclerView.Adapter<AsignaturasAdapter.VH>() {

    inner class VH(val binding: ItemAsignaturaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAsignaturaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.binding.root.context
        val asignatura = items[position]
        val b = holder.binding

        // Mostrar datos básicos
        b.txtNombreAsig.text = asignatura.nombre
        b.txtProfesorCodigoAsig.text =
            "${asignatura.profesorId ?: "Sin profesor"} - ${asignatura.codigoAsignatura}"

        // Mostrar PopupMenu al presionar el botón
        b.btnMenuAsig.setOnClickListener { view ->
            val popup = PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.menu_usuario, popup.menu)

            // Forzar mostrar íconos
            try {
                val fields = popup.javaClass.getDeclaredField("mPopup")
                fields.isAccessible = true
                val menuHelper = fields.get(popup)
                val setForceIcons =
                    menuHelper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                setForceIcons.invoke(menuHelper, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_editar -> onEdit(asignatura)
                    R.id.action_eliminar -> {
                        AlertDialog.Builder(context)
                            .setTitle("Eliminar asignatura")
                            .setMessage("¿Seguro que deseas eliminar la asignatura \"${asignatura.nombre}\"?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                FirebaseFirestore.getInstance()
                                    .collection("asignaturas")
                                    .document(asignatura.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "✅ Asignatura eliminada", Toast.LENGTH_SHORT).show()
                                        onDelete(asignatura)
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

    fun replaceAll(newAsignaturas: List<Asignatura>) {
        items.clear()
        items.addAll(newAsignaturas)
        notifyDataSetChanged()
    }
}
