package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Calificacion
import com.example.computronica.databinding.ItemNotaBinding

class CalificacionesAdapter(
    private val notas: MutableList<Calificacion>,
    private val onEdit: (Calificacion) -> Unit,
    private val onDelete: (Calificacion) -> Unit
) : RecyclerView.Adapter<CalificacionesAdapter.VH>() {

    inner class VH(val binding: ItemNotaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemNotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val nota = notas[position]
        val b = holder.binding

        b.tvTipoNota.text = nota.evaluacion
        b.tvNota.text = String.format("%.1f", nota.nota)

        b.btnEditNota.setOnClickListener { onEdit(nota) }
        b.btnDeleteNota.setOnClickListener { onDelete(nota) }
    }

    override fun getItemCount() = notas.size

    fun replaceAll(newNotas: List<Calificacion>) {
        notas.clear()
        notas.addAll(newNotas)
        notifyDataSetChanged()
    }

    fun remove(nota: Calificacion) {
        val pos = notas.indexOf(nota)
        if (pos != -1) {
            notas.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }
}