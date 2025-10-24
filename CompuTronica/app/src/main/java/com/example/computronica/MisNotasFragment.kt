package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.AsignaturasNotasAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificaciones
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.FragmentMisNotasBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MisNotasFragment : Fragment() {

    private var _binding: FragmentMisNotasBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private var listenerReg: ListenerRegistration? = null
    private var asignatura: Asignatura? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        asignatura = arguments?.getSerializable("asignatura") as? Asignatura
    }

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
        val usuario: Usuario? = SessionManager.currentUser
        binding.btnCreateCalificacion.isVisible = usuario?.tipo == TipoUsuario.administrativo || usuario?.tipo == TipoUsuario.profesor
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
        listenerReg?.remove()
        val currentUser = SessionManager.currentUser
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUser == null || currentUserId == null) {
            Log.e("MisNotasFragment", "SessionManager.currentUser or FirebaseAuth.currentUser is null")
            binding.tvEmpty.text = "Error: No hay usuario autenticado"
            binding.tvEmpty.visibility = View.VISIBLE
            return
        }

        val query = when (currentUser.tipo) {
            TipoUsuario.estudiante -> {
                val baseQuery = db.collection("calificaciones")
                    .whereEqualTo("estudianteId", currentUserId)
                if (asignatura != null) {
                    baseQuery.whereEqualTo("asignaturaId", asignatura!!.id)
                } else {
                    baseQuery
                }
            }
            TipoUsuario.profesor, TipoUsuario.administrativo -> {
                val baseQuery = db.collection("calificaciones")
                if (asignatura != null) {
                    baseQuery.whereEqualTo("asignaturaId", asignatura!!.id)
                } else {
                    baseQuery
                }
            }
        }.orderBy("fecha", Query.Direction.DESCENDING)

        listenerReg = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("MisNotasFragment", "Error loading calificaciones: ${error.message}", error)
                binding.tvEmpty.text = "Error al cargar calificaciones: ${error.message}"
                binding.tvEmpty.visibility = View.VISIBLE
                return@addSnapshotListener
            }

            if (snapshot != null && !snapshot.isEmpty) {
                val calificaciones = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Calificaciones::class.java)?.copy(id = doc.id)
                }

                if (calificaciones.isNotEmpty()) {
                    // Agrupar por asignatura
                    val agrupadas = calificaciones.groupBy { it.asignaturaId }
                    binding.rvMisNotas.adapter = AsignaturasNotasAdapter(agrupadas)
                    binding.tvEmpty.visibility = View.GONE
                } else {
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } else {
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _binding = null
    }
}