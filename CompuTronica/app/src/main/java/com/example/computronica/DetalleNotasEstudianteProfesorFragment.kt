package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.NotasEstudianteAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.FragmentDetalleNotasEstudianteBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.DecimalFormat

class DetalleNotasEstudianteProfesorFragment : Fragment() {

    private var _binding: FragmentDetalleNotasEstudianteBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var listenerReg: ListenerRegistration? = null
    private lateinit var asignatura: Asignatura
    private lateinit var estudiante: Usuario
    private var promedioPrecalculado: Double = 0.0
    private lateinit var adapter: NotasEstudianteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            asignatura = it.getSerializable("asignatura") as Asignatura
            estudiante = it.getSerializable("estudiante") as Usuario
            promedioPrecalculado = it.getDouble("promedio", 0.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleNotasEstudianteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        mostrarInformacionEstudiante()
        cargarNotasDelEstudiante()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Notas de ${estudiante.nombre}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvNotasEstudiante.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotasEstudianteAdapter()
        binding.rvNotasEstudiante.adapter = adapter
    }

    private fun mostrarInformacionEstudiante() {
        // Podemos mostrar información adicional del estudiante aquí si es necesario
        binding.txtPromedio.text = String.format("%.2f", promedioPrecalculado)

        val color = when {
            promedioPrecalculado >= 14 -> requireContext().getColor(R.color.verde)
            promedioPrecalculado >= 10 -> requireContext().getColor(R.color.rojo)
            else -> requireContext().getColor(R.color.rojo)
        }
        binding.txtPromedio.setTextColor(color)
    }

    private fun cargarNotasDelEstudiante() {
        val query = db.collection("calificaciones")
            .whereEqualTo("estudianteId", estudiante.id)
            .whereEqualTo("asignaturaId", asignatura.id)

        listenerReg = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                binding.tvEmpty.text = "Error al cargar notas: ${error.message}"
                binding.tvEmpty.visibility = View.VISIBLE
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val notas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Calificaciones::class.java)?.copy(id = doc.id)
                }.sortedBy { it.fecha }

                if (notas.isNotEmpty()) {
                    adapter.submitList(notas)
                    binding.tvEmpty.visibility = View.GONE
                    binding.layoutPromedio.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.text = "No hay notas registradas"
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.layoutPromedio.visibility = View.VISIBLE
                }
            } else {
                binding.tvEmpty.text = "No se encontraron notas"
                binding.tvEmpty.visibility = View.VISIBLE
                binding.layoutPromedio.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _binding = null
    }
}