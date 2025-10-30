package com.example.computronica

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.NotasEstudianteAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificacion
import com.example.computronica.databinding.FragmentDetalleNotasEstudianteBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.DecimalFormat

class DetalleNotasEstudianteFragment : Fragment() {

    private var _binding: FragmentDetalleNotasEstudianteBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var listenerReg: ListenerRegistration? = null
    private lateinit var asignatura: Asignatura
    private lateinit var adapter: NotasEstudianteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getSerializable("asignatura")?.let {
            asignatura = it as Asignatura
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
        cargarNotasDelEstudiante()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Mis Notas - ${asignatura.nombre}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvNotasEstudiante.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotasEstudianteAdapter()
        binding.rvNotasEstudiante.adapter = adapter
    }

    private fun cargarNotasDelEstudiante() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserId == null) {
            Log.e("DetalleNotasEstudiante", "Usuario no autenticado")
            binding.tvEmpty.text = "Error: Usuario no autenticado"
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvEmpty.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        val query = db.collection("calificaciones")
            .whereEqualTo("estudianteId", currentUserId)
            .whereEqualTo("asignaturaId", asignatura.id)

        listenerReg = query.addSnapshotListener { snapshot, error ->
            binding.progressBar.visibility = View.GONE

            if (error != null) {
                Log.e("DetalleNotasEstudiante", "Error loading notas: ${error.message}", error)
                binding.tvEmpty.text = "Error al cargar notas: ${error.message}"
                binding.tvEmpty.visibility = View.VISIBLE
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val notas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Calificacion::class.java)?.copy(id = doc.id)
                }.sortedBy { it.fecha }

                if (notas.isNotEmpty()) {
                    adapter.submitList(notas)
                    calcularYMostrarPromedio(notas)
                    binding.tvEmpty.visibility = View.GONE
                } else {
                    binding.tvEmpty.text = "No hay notas registradas para este curso"
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.layoutPromedio.visibility = View.GONE
                }
            } else {
                binding.tvEmpty.text = "No se encontraron notas"
                binding.tvEmpty.visibility = View.VISIBLE
                binding.layoutPromedio.visibility = View.GONE
            }
        }
    }

    private fun calcularYMostrarPromedio(notas: List<Calificacion>) {
        if (notas.isEmpty()) {
            binding.layoutPromedio.visibility = View.GONE
            return
        }

        val promedio = notas.map { it.nota }.average()
        val df = DecimalFormat("#.##")

        binding.txtPromedio.text = df.format(promedio)
        binding.txtTotalNotas.text = "Basado en ${notas.size} evaluación(es)"

        // Color según el promedio
        val color = when {
            promedio >= 14 -> requireContext().getColor(R.color.verde)
            promedio >= 10 -> requireContext().getColor(R.color.rojo)
            else -> requireContext().getColor(R.color.rojo)
        }
        binding.txtPromedio.setTextColor(color)

        binding.layoutPromedio.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _binding = null
    }

}