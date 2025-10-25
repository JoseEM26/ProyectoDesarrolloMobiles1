package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.AsignaturasAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.FragmentMisNotasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MisNotasFragment : Fragment() {

    private var _binding: FragmentMisNotasBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var listenerReg: ListenerRegistration? = null
    private lateinit var cursosAdapter: AsignaturasAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMisNotasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFabVisibility()
        cargarCursosDelUsuario()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Mis Cursos"
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvMisNotas.layoutManager = LinearLayoutManager(requireContext())

        cursosAdapter = AsignaturasAdapter(
            items = mutableListOf(),
            onEdit = { /* No operation en calificaciones */ },
            onDelete = { /* No operation en calificaciones */ },
            onItemClick = { asignatura ->
                onCursoSelected(asignatura)
            },
            readOnlyMode = true // Modo solo lectura para calificaciones
        )

        binding.rvMisNotas.adapter = cursosAdapter
    }

    private fun setupFabVisibility() {
        val currentUser = SessionManager.currentUser
        binding.btnCreateCalificacion.isVisible =
            currentUser?.tipo == TipoUsuario.administrativo ||
                    currentUser?.tipo == TipoUsuario.profesor


        binding.btnCreateCalificacion.setOnClickListener {
            mostrarDialogoCrearCalificacion()
        }
    }

    private fun mostrarDialogoCrearCalificacion() {
        val dialog = CrearCalificacionDialogFragment()
        dialog.show(parentFragmentManager, "CrearCalificacionDialog")
    }

    private fun cargarCursosDelUsuario() {
        val currentUser = SessionManager.currentUser
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUser == null || currentUserId == null) {
            Log.e("MisNotasFragment", "Usuario no autenticado")
            binding.tvEmpty.text = "Error: No hay usuario autenticado"
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        binding.tvEmpty.visibility = View.GONE

        val query = when (currentUser.tipo) {
            TipoUsuario.estudiante -> {
                // Cursos donde el estudiante está inscrito
                db.collection("asignaturas")
                    .whereArrayContains("estudiantes", currentUserId)
            }
            TipoUsuario.profesor -> {
                // Cursos donde el profesor está asignado
                db.collection("asignaturas")
                    .whereArrayContains("profesores", currentUserId)
            }
            TipoUsuario.administrativo -> {
                // Admin ve todos los cursos
                db.collection("asignaturas")
            }
        }

        listenerReg = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("MisNotasFragment", "Error loading cursos: ${error.message}", error)
                binding.tvEmpty.text = "Error al cargar cursos: ${error.message}"
                binding.tvEmpty.visibility = View.VISIBLE
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val cursos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Asignatura::class.java)?.copy(id = doc.id)
                }

                if (cursos.isNotEmpty()) {
                    cursosAdapter.replaceAll(cursos)
                    binding.tvEmpty.visibility = View.GONE
                } else {
                    binding.tvEmpty.text = "No tienes cursos asignados"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } else {
                binding.tvEmpty.text = "No se encontraron cursos"
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    // Función que estaba faltando - maneja la selección de un curso
    private fun onCursoSelected(asignatura: Asignatura) {
        val currentUser = SessionManager.currentUser

        when (currentUser?.tipo) {
            TipoUsuario.estudiante -> {
                // Navegar a fragment con las notas del estudiante en ese curso
                val fragment = DetalleNotasEstudianteFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("asignatura", asignatura)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack("notas_estudiante")
                    .commit()
            }
            TipoUsuario.profesor, TipoUsuario.administrativo -> {
                // Navegar a fragment con lista de estudiantes y sus notas
                val fragment = ListaEstudiantesNotasFragment().apply {
                    arguments = Bundle().apply {
                        putSerializable("asignatura", asignatura)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, fragment)
                    .addToBackStack("lista_estudiantes")
                    .commit()
            }
            else -> {
                Log.e("MisNotasFragment", "Tipo de usuario no reconocido")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _binding = null
    }
}