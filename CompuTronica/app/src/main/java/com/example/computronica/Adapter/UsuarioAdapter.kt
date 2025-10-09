package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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

    }

    override fun getItemCount(): Int =items.size

    fun add(usuario: Usuario) {
        items.add(usuario)  // Agregar el producto al final de la lista
        notifyItemInserted(items.lastIndex)  // Notificar que un nuevo item ha sido agregado
        onListChanged()  // Actualizar el resumen de totales
    }


}