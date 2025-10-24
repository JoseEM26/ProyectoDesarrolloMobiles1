package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificaciones
import com.example.computronica.R
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AsignaturasNotasAdapter(
    private val notasPorAsignatura: Map<String, List<Calificaciones>>
) : RecyclerView.Adapter<AsignaturasNotasAdapter.ViewHolder>() {

    private val asignaturaIds = notasPorAsignatura.keys.toList()
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNombreAsignatura: TextView = itemView.findViewById(R.id.txtNombreAsignatura)
        val layoutNotas: LinearLayout = itemView.findViewById(R.id.layoutNotas)
        val txtPromedio: TextView = itemView.findViewById(R.id.txtPromedio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_asignatura_notas, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val asignaturaId = asignaturaIds[position]
        val notas = notasPorAsignatura[asignaturaId] ?: emptyList()

        // Obtener nombre de la asignatura
        scope.launch {
            try {
                val doc = db.collection("asignaturas")
                    .document(asignaturaId)
                    .get()
                    .await()
                val asignatura = doc.toObject(Asignatura::class.java)
                holder.txtNombreAsignatura.text = asignatura?.nombre ?: "Asignatura"
            } catch (e: Exception) {
                holder.txtNombreAsignatura.text = "Error al cargar asignatura"
            }
        }

        // Limpiar layout de notas
        holder.layoutNotas.removeAllViews()

        // Agregar cada nota
        notas.forEach { calificacion ->
            val notaView = LayoutInflater.from(holder.itemView.context)
                .inflate(android.R.layout.simple_list_item_2, holder.layoutNotas, false)

            val txtTitulo = notaView.findViewById<TextView>(android.R.id.text1)
            val txtNota = notaView.findViewById<TextView>(android.R.id.text2)

            txtTitulo.text = calificacion.evaluacion
            txtTitulo.textSize = 16f
            txtTitulo.setTextColor(holder.itemView.resources.getColor(R.color.azul_oscuro, null))

            txtNota.text = "Nota: ${calificacion.nota}"
            txtNota.textSize = 14f

            // Color según nota
            val colorNota = if (calificacion.nota >= 14) {
                holder.itemView.resources.getColor(R.color.verde, null)
            } else {
                holder.itemView.resources.getColor(R.color.rojo, null)
            }
            txtNota.setTextColor(colorNota)

            holder.layoutNotas.addView(notaView)
        }

        // Calcular y mostrar promedio
        val promedio = if (notas.isNotEmpty()) {
            notas.map { it.nota }.average()
        } else {
            0.0
        }

        holder.txtPromedio.text = String.format("%.2f", promedio)

        // Color del promedio
        val colorPromedio = if (promedio >= 14) {
            holder.itemView.resources.getColor(R.color.verde, null)
        } else {
            holder.itemView.resources.getColor(R.color.rojo, null)
        }
        holder.txtPromedio.setTextColor(colorPromedio)
    }

    override fun getItemCount(): Int = asignaturaIds.size
}