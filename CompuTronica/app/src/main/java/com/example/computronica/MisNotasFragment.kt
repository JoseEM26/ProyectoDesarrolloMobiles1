package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificaciones
import com.example.computronica.databinding.ActivityMisNotasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MisNotasFragment : Fragment() {

    private var _binding: ActivityMisNotasBinding? = null
    private val binding get() = _binding!!
    private var uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityMisNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        cargarMisNotas()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvMisNotas.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun cargarMisNotas() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Obtener todas las calificaciones del usuario actual
        db.collection("calificaciones")
            .whereEqualTo("estudianteId", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    binding.tvEmpty.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val calificaciones = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Calificaciones::class.java)?.copy(id = doc.id)
                    }

                    if (calificaciones.isNotEmpty()) {
                        // Agrupar por asignatura
                        agruparYMostrarPorAsignatura(calificaciones)
                        binding.tvEmpty.visibility = View.GONE
                    } else {
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                } else {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
    }

    private fun agruparYMostrarPorAsignatura(calificaciones: List<Calificaciones>) {
        // Agrupar calificaciones por asignaturaId
        val agrupadas = calificaciones.groupBy { it.asignaturaId }

        // Crear adapter personalizado
        val adapter = AsignaturasNotasAdapter(agrupadas)
        binding.rvMisNotas.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }

    // ========== ADAPTER INTERNO ==========
    inner class AsignaturasNotasAdapter(
        private val notasPorAsignatura: Map<String, List<Calificaciones>>
    ) : RecyclerView.Adapter<AsignaturasNotasAdapter.ViewHolder>() {

        private val asignaturaIds = notasPorAsignatura.keys.toList()

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
            db.collection("asignaturas")
                .document(asignaturaId)
                .get()
                .addOnSuccessListener { doc ->
                    val asignatura = doc.toObject(Asignatura::class.java)
                    holder.txtNombreAsignatura.text = asignatura?.nombre ?: "Asignatura"
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
                txtTitulo.setTextColor(resources.getColor(R.color.azul_oscuro, null))

                txtNota.text = "Nota: ${calificacion.nota}"
                txtNota.textSize = 14f

                // Color según nota
                val colorNota = if (calificacion.nota >= 14) {
                    resources.getColor(R.color.verde, null)
                } else {
                    resources.getColor(R.color.rojo, null)
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
                resources.getColor(R.color.verde, null)
            } else {
                resources.getColor(R.color.rojo, null)
            }
            holder.txtPromedio.setTextColor(colorPromedio)
        }

        override fun getItemCount(): Int = asignaturaIds.size
    }

}