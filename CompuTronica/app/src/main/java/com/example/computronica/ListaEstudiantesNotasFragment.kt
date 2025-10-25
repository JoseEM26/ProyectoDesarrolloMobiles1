package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.ListaEstudiantesAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.FragmentListaEstudiantesNotasBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ListaEstudiantesNotasFragment : Fragment() {

    private var _binding: FragmentListaEstudiantesNotasBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var listenerReg: ListenerRegistration? = null
    private lateinit var asignatura: Asignatura
    private lateinit var adapter: ListaEstudiantesAdapter
    private val scope = CoroutineScope(Dispatchers.Main)

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
        _binding = FragmentListaEstudiantesNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        cargarEstudiantesYNotas()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Estudiantes - ${asignatura.nombre}"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvNotasEstudiante.layoutManager = LinearLayoutManager(requireContext())
        adapter = ListaEstudiantesAdapter { estudiante, promedio ->
            onEstudianteSelected(estudiante, promedio)
        }
        binding.rvNotasEstudiante.adapter = adapter
    }

    private fun cargarEstudiantesYNotas() {
        if (asignatura.estudiantes.isEmpty()) {
            binding.tvEmpty.text = "No hay estudiantes inscritos en este curso"
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvEmpty.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE

        // Cargar información de los estudiantes y sus notas
        scope.launch {
            try {
                val estudiantesConNotas = mutableListOf<EstudianteConNotas>()

                for (estudianteId in asignatura.estudiantes) {
                    // Obtener información del estudiante
                    val estudianteDoc = db.collection("usuarios")
                        .document(estudianteId)
                        .get()
                        .await()

                    if (estudianteDoc.exists()) {
                        val estudiante = estudianteDoc.toObject(Usuario::class.java)
                        if (estudiante != null) {
                            // Obtener notas del estudiante en esta asignatura
                            val notasSnapshot = db.collection("calificaciones")
                                .whereEqualTo("estudianteId", estudianteId)
                                .whereEqualTo("asignaturaId", asignatura.id)
                                .get()
                                .await()

                            val notas = notasSnapshot.documents.mapNotNull { doc ->
                                doc.toObject(Calificaciones::class.java)
                            }

                            // Calcular promedio
                            val promedio = if (notas.isNotEmpty()) {
                                notas.map { it.nota }.average()
                            } else {
                                0.0
                            }

                            estudiantesConNotas.add(
                                EstudianteConNotas(
                                    estudiante = estudiante,
                                    notas = notas,
                                    promedio = promedio
                                )
                            )
                        }
                    }
                }

                // Ordenar estudiantes por nombre
                estudiantesConNotas.sortBy { it.estudiante.nombre }

                if (estudiantesConNotas.isNotEmpty()) {
                    adapter.submitEstudiantesList(estudiantesConNotas)
                    binding.tvEmpty.visibility = View.GONE
                } else {
                    binding.tvEmpty.text = "No se encontraron estudiantes con notas"
                    binding.tvEmpty.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e("ListaEstudiantesNotas", "Error loading estudiantes: ${e.message}", e)
                binding.tvEmpty.text = "Error al cargar estudiantes: ${e.message}"
                binding.tvEmpty.visibility = View.VISIBLE
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun onEstudianteSelected(estudiante: Usuario, promedio: Double) {
        // Navegar al detalle de notas del estudiante seleccionado
        val fragment = DetalleNotasEstudianteProfesorFragment().apply {
            arguments = Bundle().apply {
                putSerializable("asignatura", asignatura)
                putSerializable("estudiante", estudiante)
                putDouble("promedio", promedio)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .addToBackStack("detalle_estudiante_profesor")
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _binding = null
    }

    // Data class para manejar estudiantes con sus notas
    data class EstudianteConNotas(
        val estudiante: Usuario,
        val notas: List<Calificaciones>,
        val promedio: Double
    )
}