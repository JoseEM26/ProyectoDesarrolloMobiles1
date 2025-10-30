package com.example.computronica.Adapter

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.MainActivity
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.NotasActivity
import com.example.computronica.R
import com.example.computronica.SessionManager
import com.example.computronica.TemaActivity
import com.example.computronica.databinding.ItemAsignaturaBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AsignaturasAdapter(
    private val items: MutableList<Asignatura>,
    private val onEdit: (Asignatura) -> Unit = {},
    private val onDelete: (Asignatura) -> Unit = {},
    private val onItemClick: (Asignatura) -> Unit = {}, // Nuevo parámetro para click simple
    private val readOnlyMode: Boolean = false // Nuevo modo solo lectura
) : RecyclerView.Adapter<AsignaturasAdapter.VH>() {

    inner class VH(val binding: ItemAsignaturaBinding) : RecyclerView.ViewHolder(binding.root)

    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAsignaturaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val context = holder.binding.root.context
        val asignatura = items[position]
        val b = holder.binding
        val usuario: Usuario? = SessionManager.currentUser

        val mainActivity = context as? MainActivity ?: run {
            Log.e("AsignaturasAdapter", "Context is not MainActivity")
            return
        }

        // Mostrar datos básicos
        b.txtNombreAsig.text = asignatura.nombre
        b.txtProfesorCodigoAsig.text = asignatura.codigoAsignatura

        // Buscar nombres de los profesores en BD
        setupProfesorNames(b.txtProfesorCodigoAsig, asignatura.profesores, context)

        // Configurar visibilidad del botón de menú
        if (readOnlyMode) {
            // Modo solo lectura: ocultar menú y usar click simple
            b.btnMenuAsig.visibility = View.GONE
            b.root.setOnClickListener {
                onItemClick(asignatura)
            }
        } else {
            // Modo normal: comportamiento original
            b.btnMenuAsig.visibility = if (usuario?.tipo != null) View.VISIBLE else View.GONE

            // Handle item click to navigate to TemaFragment (comportamiento original)
            b.root.setOnClickListener {
                val fragment = TemaActivity().apply {
                    arguments = Bundle().apply {
                        putSerializable("asignatura", asignatura)
                    }
                }
                mainActivity.changeFrame(fragment)
                mainActivity.supportActionBar?.title = asignatura.nombre
            }

            // Configurar menú (comportamiento original)
            b.btnMenuAsig.setOnClickListener { view ->
                if (usuario?.tipo != null) {
                    showPopupMenu(view, asignatura, usuario, context)
                }
            }
        }

        // Mostrar créditos si están disponibles (mejora visual)
        if (asignatura.creditos > 0) {
            val currentText = b.txtProfesorCodigoAsig.text.toString()
            b.txtProfesorCodigoAsig.text = "$currentText • ${asignatura.creditos} créditos"
        }
    }

    private fun showPopupMenu(view: View, asignatura: Asignatura, usuario: Usuario, context: android.content.Context) {
        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.menu_asignatura, popup.menu)

        when (usuario.tipo) {
            TipoUsuario.estudiante -> {
                popup.menu.findItem(R.id.action_editar)?.isVisible = false
                popup.menu.findItem(R.id.action_eliminar)?.isVisible = false
            }
            TipoUsuario.profesor -> {
                popup.menu.findItem(R.id.action_editar)?.isVisible = false
                popup.menu.findItem(R.id.action_eliminar)?.isVisible = false
            }
            TipoUsuario.administrativo -> {
                popup.menu.findItem(R.id.action_editar)?.isVisible = true
                popup.menu.findItem(R.id.action_eliminar)?.isVisible = true
            }
        }

        try {
            val fields = popup.javaClass.getDeclaredField("mPopup")
            fields.isAccessible = true
            val menuHelper = fields.get(popup)
            val setForceIcons = menuHelper.javaClass.getDeclaredMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
            setForceIcons.invoke(menuHelper, true)
        } catch (e: Exception) {
            Log.e("AsignaturasAdapter", "Error forcing icons in PopupMenu: ${e.message}")
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_editar -> {
                    if (usuario.tipo == TipoUsuario.administrativo) {
                        onEdit(asignatura)
                        true
                    } else {
                        Toast.makeText(context, "No tienes permiso para editar", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.action_eliminar -> {
                    if (usuario.tipo == TipoUsuario.administrativo) {
                        showDeleteConfirmation(asignatura, context)
                        true
                    } else {
                        Toast.makeText(context, "Solo los administradores pueden eliminar asignaturas", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                R.id.action_notas -> {
                    // Permitir solo a profesores y administrativos
                    if (usuario.tipo == TipoUsuario.profesor || usuario.tipo == TipoUsuario.administrativo) {
                        val fragment = NotasActivity().apply {
                            arguments = Bundle().apply {
                                putSerializable("asignatura", asignatura)
                            }
                        }
                        // Asegúrate de que el contexto sea MainActivity
                        val mainActivity = context as? MainActivity
                        if (mainActivity != null) {
                            mainActivity.changeFrame(fragment)
                        } else {
                            Toast.makeText(context, "Error al abrir notas", Toast.LENGTH_SHORT).show()
                        }
                        true
                    } else {
                        Toast.makeText(context, "Solo profesores pueden gestionar notas", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showDeleteConfirmation(asignatura: Asignatura, context: android.content.Context) {
        AlertDialog.Builder(context)
            .setTitle("Eliminar asignatura")
            .setMessage("¿Seguro que deseas eliminar la asignatura ${asignatura.nombre}?")
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

    private fun setupProfesorNames(textView: TextView, profesorIds: List<String>, context: android.content.Context) {
        if (profesorIds.isEmpty()) {
            textView.text = "Sin profesores"
            textView.setTextColor(ContextCompat.getColor(context, R.color.gris_oscuro))
            return
        }

        scope.launch {
            try {
                val nombres = mutableListOf<String>()
                for (profesorId in profesorIds) {
                    val snapshot = db.collection("usuarios")
                        .document(profesorId)
                        .get()
                        .await()
                    if (snapshot.exists()) {
                        val profesor = snapshot.toObject(Usuario::class.java)
                        val nombreCompleto = "${profesor?.nombre} ${profesor?.apellido ?: ""}".trim()
                        if (nombreCompleto.isNotEmpty()) nombres.add(nombreCompleto)
                    }
                }
                if (nombres.isNotEmpty()) {
                    textView.text = nombres.joinToString(", ")
                    textView.setTextColor(ContextCompat.getColor(context, R.color.azul_oscuro))
                } else {
                    textView.text = "Sin profesores"
                    textView.setTextColor(ContextCompat.getColor(context, R.color.gris_oscuro))
                }
            } catch (e: Exception) {
                Log.e("AsignaturasAdapter", "Error fetching professor names: ${e.message}")
                textView.text = "Error al cargar profesores"
                textView.setTextColor(ContextCompat.getColor(context, R.color.rojo_error))
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newAsignaturas: List<Asignatura>) {
        items.clear()
        items.addAll(newAsignaturas)
        notifyDataSetChanged()
    }
}