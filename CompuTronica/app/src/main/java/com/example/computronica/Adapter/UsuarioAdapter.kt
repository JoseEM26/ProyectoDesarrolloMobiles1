package com.example.computronica.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Usuario
import com.example.computronica.R
import com.example.computronica.databinding.ItemUsuariosBinding
import com.google.firebase.firestore.FirebaseFirestore

class UsuarioAdapter(
    private val items: MutableList<Usuario>,
    private val onEdit: (Usuario) -> Unit,
    private val onDelete: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.VH>() {

    inner class VH(val binding: ItemUsuariosBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUsuariosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.binding.root.context
        val u = items[position]
        val b = holder.binding

        b.txtUsuariosNombre.text = "${u.nombre} ${u.apellido}"
        b.txtUsuariosSede.text = u.sede
        b.txtUsuariosActivo.text = if (u.estado) "Activo" else "Inactivo"
        b.txtUsuariosTipo.text=u.tipo.toString()

        // Color dinámico para estado
        val colorRes = if (u.estado) R.color.verde else R.color.rojo
        b.txtUsuariosActivo.setTextColor(context.getColor(colorRes))

        // Mostrar PopupMenu al presionar el botón
        b.btnMenu.setOnClickListener { view ->
            val popup = PopupMenu(context, view)
            popup.menuInflater.inflate(R.menu.menu_usuario, popup.menu)

            // 🔹 Fuerza a mostrar íconos en el PopupMenu
            try {
                val fields = popup.javaClass.getDeclaredField("mPopup")
                fields.isAccessible = true
                val menuHelper = fields.get(popup)
                val setForceIcons = menuHelper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                setForceIcons.invoke(menuHelper, true)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_editar -> onEdit(u)
                    R.id.action_eliminar -> {
                        AlertDialog.Builder(context)
                            .setTitle("Eliminar usuario")
                            .setMessage("¿Seguro que deseas eliminar a ${u.nombre}?")
                            .setPositiveButton("Eliminar") { _, _ ->
                                // Aquí puedes agregar la lógica de eliminación
                                FirebaseFirestore.getInstance()
                                    .collection("usuarios")
                                    .document(u.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "✅ Usuario eliminado", Toast.LENGTH_SHORT).show()
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

    fun replaceAll(newUsuarios: List<Usuario>) {
        items.clear()
        items.addAll(newUsuarios)
        notifyDataSetChanged()
    }
}
