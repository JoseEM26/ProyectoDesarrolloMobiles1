package com.example.computronica.Adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.Switch
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.R
import com.example.computronica.databinding.ItemUsuariosBinding

class UsuarioAdapter(private val items:MutableList<Usuario> , private val onListChanged:()->Unit
) : RecyclerView.Adapter<UsuarioAdapter.VH>() {

        inner class VH(val binding:ItemUsuariosBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioAdapter.VH {
        val binding=ItemUsuariosBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: UsuarioAdapter.VH, position: Int) {
        val context=holder.binding.root.context
        val u =items[position]
        val b=holder.binding

        if(u.imgURI != null){
            b.imgUsuarioPhoto.setImageURI(u.imgURI)
        }else{
            b.imgUsuarioPhoto.setImageResource(R.mipmap.ic_launcher)
        }

        b.txtUsuariosNombre.text=u.nombre+" "+u.apellido
        b.txtUsuariosSede.text=u.sede
        b.txtUsuariosActivo.text = if (u.estado) "Activo" else "Inactivo"

        //ACA ES PARA PONER EL NUEVO EDITAR Y ELIMINAR
        b.root.setOnLongClickListener{
            val pop=PopupMenu(context,b.root)
            pop.menu.add(context.getString(R.string.title_alert_editar))
            pop.menu.add(context.getString(R.string.title_alert_eliminar))
            pop.setOnMenuItemClickListener { item ->
                when (item.title) {
                    context.getString(R.string.title_alert_editar) -> {
                        showEditusuarioDialog(context, u) {
                            notifyItemChanged(position)
                            onListChanged()
                        }
                        true
                    }

                    context.getString(R.string.title_alert_eliminar) -> {
                        val idx = holder.bindingAdapterPosition
                        if (idx != RecyclerView.NO_POSITION) {
                            items.removeAt(idx)
                            notifyItemRemoved(idx) // <- esto también es importante
                            onListChanged()
                        }
                        true
                    }

                    else -> false
                }
            }

            pop.show()
            true
        }

    }

    override fun getItemCount(): Int =items.size

    fun showEditusuarioDialog(ctx: android.content.Context, p: Usuario, onDone: () -> Unit) {
        // Layout contenedor vertical
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        // Campos EditText
        val etNombre = EditText(ctx).apply {
            hint = "Nombre"
            setText(p.nombre)
        }
        val etApellido = EditText(ctx).apply {
            hint = "Apellido"
            setText(p.apellido)
        }
        val etCodigo = EditText(ctx).apply {
            hint = "Código institucional"
            setText(p.codigoInstitucional)
        }
        val etSede = EditText(ctx).apply {
            hint = "Sede"
            setText(p.sede)
        }
        val etCorreo = EditText(ctx).apply {
            hint = "Correo institucional"
            setText(p.correoInstitucional)
        }
        val etContrasena = EditText(ctx).apply {
            hint = "Contraseña"
            setText(p.contrasena)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        // Spinner para TipoUsuario
        val tipoSpinner = Spinner(ctx)
        val tipos = TipoUsuario.values().map { it.name }
        val spinnerAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, tipos)
        tipoSpinner.adapter = spinnerAdapter
        tipoSpinner.setSelection(p.tipo.ordinal)

        // Switch para estado (activo/inactivo)
        val switchEstado = Switch(ctx).apply {
            text = "Usuario activo"
            isChecked = p.estado
        }

        // Agregar todos los campos al layout
        layout.apply {
            addView(etCodigo)
            addView(etSede)
            addView(etNombre)
            addView(etApellido)
            addView(etCorreo)
            addView(etContrasena)
            addView(tipoSpinner)
            addView(switchEstado)
        }

        // Crear el diálogo
        AlertDialog.Builder(ctx)
            .setTitle(ctx.getString(R.string.title_alert_editar))
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                // Validar y guardar cambios
                val nuevoUsuario = p.copy(
                    codigoInstitucional = etCodigo.text.toString().trim(),
                    sede = etSede.text.toString().trim(),
                    nombre = etNombre.text.toString().trim(),
                    apellido = etApellido.text.toString().trim(),
                    correoInstitucional = etCorreo.text.toString().trim(),
                    contrasena = etContrasena.text.toString().trim(),
                    tipo = TipoUsuario.valueOf(tipoSpinner.selectedItem.toString()),
                    estado = switchEstado.isChecked
                )

                // Reemplazar datos del usuario original (mutable por referencia)
                p.codigoInstitucional = nuevoUsuario.codigoInstitucional
                p.sede = nuevoUsuario.sede
                p.nombre = nuevoUsuario.nombre
                p.apellido = nuevoUsuario.apellido
                p.correoInstitucional = nuevoUsuario.correoInstitucional
                p.contrasena = nuevoUsuario.contrasena
                p.tipo = nuevoUsuario.tipo
                p.estado = nuevoUsuario.estado

                onDone() // Notificar cambios
                dialog.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    fun add(usuario: Usuario) {
        items.add(usuario)  // Agregar el producto al final de la lista
        notifyItemInserted(items.lastIndex)  // Notificar que un nuevo item ha sido agregado
        onListChanged()  // Actualizar el resumen de totales
    }


}