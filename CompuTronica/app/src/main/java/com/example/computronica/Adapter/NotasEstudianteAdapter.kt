package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Calificacion
import com.example.computronica.R
import java.text.SimpleDateFormat
import java.util.Locale

class NotasEstudianteAdapter : ListAdapter<Calificacion, NotasEstudianteAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtEvaluacion: TextView = itemView.findViewById(R.id.txtEvaluacion)
        val txtNota: TextView = itemView.findViewById(R.id.txtNota)
        val txtFecha: TextView = itemView.findViewById(R.id.txtFecha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nota_estudiante, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val nota = getItem(position)

        holder.txtEvaluacion.text = nota.evaluacion
        holder.txtNota.text = String.format("%.2f", nota.nota)

        // Formatear fecha
        val fechaFormateada = if (nota.fecha != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            sdf.format(nota.fecha!!.toDate())
        } else {
            "Sin fecha"
        }
        holder.txtFecha.text = fechaFormateada

        // Color según la nota
        val color = when {
            nota.nota >= 14 -> ContextCompat.getColor(holder.itemView.context, R.color.verde)
            nota.nota >= 10 -> ContextCompat.getColor(holder.itemView.context, R.color.rojo)
            else -> ContextCompat.getColor(holder.itemView.context, R.color.rojo)
        }
        holder.txtNota.setTextColor(color)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Calificacion>() {
        override fun areItemsTheSame(oldItem: Calificacion, newItem: Calificacion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Calificacion, newItem: Calificacion): Boolean {
            return oldItem == newItem
        }
    }

}