package com.example.computronica

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Tema
import com.example.computronica.databinding.ItemTemaBinding
import java.text.SimpleDateFormat
import java.util.*

class TemasAdapter(
    private val items: MutableList<Tema>
) : RecyclerView.Adapter<TemasAdapter.VH>() {

    inner class VH(val binding: ItemTemaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTemaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tema = items[position]
        val b = holder.binding

        b.tvNombreTema.text = tema.nombre
        b.tvDescripcionTema.text = tema.descripcion
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        b.tvFechaCreacion.text = tema.fechaCreacion?.toDate()?.let { dateFormat.format(it) } ?: "Sin fecha"
    }

    override fun getItemCount(): Int = items.size

    fun replaceAll(newTemas: List<Tema>) {
        items.clear()
        items.addAll(newTemas)
        notifyDataSetChanged()
    }
}