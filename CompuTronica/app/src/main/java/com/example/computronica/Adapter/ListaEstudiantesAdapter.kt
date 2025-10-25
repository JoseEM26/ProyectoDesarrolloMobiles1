package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Usuario
import com.example.computronica.R
import java.text.DecimalFormat

class ListaEstudiantesAdapter(
    private val onEstudianteClick: (Usuario, Double) -> Unit
) : ListAdapter<ListaEstudiantesAdapter.EstudianteConNotas, ListaEstudiantesAdapter.ViewHolder>(DiffCallback) {

    // Data class interna para type safety
    data class EstudianteConNotas(
        val estudiante: Usuario,
        val promedio: Double
    )

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombre: TextView = itemView.findViewById(R.id.txtNombreEstudiante)
        val txtCodigo: TextView = itemView.findViewById(R.id.txtCodigoEstudiante)
        val txtPromedio: TextView = itemView.findViewById(R.id.txtPromedioEstudiante)
        val txtEstado: TextView = itemView.findViewById(R.id.txtEstadoNotas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_estudiante_notas, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val estudiante = item.estudiante
        val promedio = item.promedio
        val df = DecimalFormat("#.##")

        holder.txtNombre.text = "${estudiante.nombre} ${estudiante.apellido}"
        holder.txtCodigo.text = estudiante.codigoInstitucional
        holder.txtPromedio.text = df.format(promedio)

        // Configurar color del promedio y estado
        val (color, estado) = when {
            promedio >= 14 -> Pair(R.color.verde, "Aprobado")
            promedio >= 10 -> Pair(R.color.rojo, "Regular")
            promedio > 0 -> Pair(R.color.rojo, "Reprobado")
            else -> Pair(R.color.gris_oscuro, "Sin notas")
        }

        holder.txtPromedio.setTextColor(ContextCompat.getColor(holder.itemView.context, color))
        holder.txtEstado.text = estado
        holder.txtEstado.setTextColor(ContextCompat.getColor(holder.itemView.context, color))

        // Configurar click
        holder.itemView.setOnClickListener {
            onEstudianteClick(estudiante, promedio)
        }
    }

    fun submitEstudiantesList(estudiantesConNotas: List<com.example.computronica.ListaEstudiantesNotasFragment.EstudianteConNotas>) {
        val items = estudiantesConNotas.map {
            EstudianteConNotas(it.estudiante, it.promedio)
        }
        super.submitList(items)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<EstudianteConNotas>() {
        override fun areItemsTheSame(oldItem: EstudianteConNotas, newItem: EstudianteConNotas): Boolean {
            return oldItem.estudiante.id == newItem.estudiante.id
        }

        override fun areContentsTheSame(oldItem: EstudianteConNotas, newItem: EstudianteConNotas): Boolean {
            return oldItem == newItem
        }
    }
}