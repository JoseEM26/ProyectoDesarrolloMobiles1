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

class UsuarioAdapter(private val items:MutableList<Usuario> , private val onEdit:(Usuario)->Unit
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


        b.txtUsuariosNombre.text=u.nombre+" "+u.apellido
        b.txtUsuariosSede.text=u.sede
        b.txtUsuariosActivo.text = if (u.estado) "Activo" else "Inactivo"
        b.root.setOnClickListener{onEdit(u)}
    }

    override fun getItemCount(): Int =items.size

    fun getUsuarioAt(position: Int): Usuario = items[position]

    fun replaceAll(newUsuarios: List<Usuario>) {
        items.clear()
        items.addAll(newUsuarios)
        notifyDataSetChanged()
    }

}