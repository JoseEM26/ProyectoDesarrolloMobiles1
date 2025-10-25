package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Tema
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityTemaBinding
import com.example.computronica.databinding.FormTemaBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TemaActivity : Fragment() {

    private var _b: ActivityTemaBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var asignatura: Asignatura? = null
    private lateinit var temasAdapter: TemasAdapter
    private var listenerReg: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityTemaBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar Toolbar
        b.toolbar.setNavigationOnClickListener {
            Log.d("TemaFragment", "Navigation icon clicked, returning to AsignaturaActivity")
            (activity as? MainActivity)?.changeFrame(AsignaturaActivity())
        }

        // Configurar RecyclerView
        temasAdapter = TemasAdapter(mutableListOf())
        b.rvTemas.layoutManager = LinearLayoutManager(requireContext())
        b.rvTemas.adapter = temasAdapter
        b.rvTemas.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
        Log.d("TemaFragment", "RecyclerView initialized")

        // Obtener asignatura del Bundle
        asignatura = arguments?.getSerializable("asignatura") as? Asignatura
        if (asignatura == null) {
            Log.e("TemaFragment", "No asignatura provided")
            b.tvNombreAsignatura.text = "Error: Asignatura no encontrada"
            b.tvEmptyTemas.text = "Error al cargar datos"
            b.tvEmptyTemas.isVisible = true
            return
        }
        Log.d("TemaFragment", "Asignatura received: ${asignatura?.nombre}, ID: ${asignatura?.id}")

        // Mostrar datos de la asignatura
        displayAsignaturaData(asignatura!!)

        // Configurar FAB basado en el tipo de usuario
        val currentUser = SessionManager.currentUser
        b.fabCreateTema.isVisible = currentUser?.tipo == TipoUsuario.administrativo || currentUser?.tipo == TipoUsuario.profesor
        Log.d("TemaFragment", "FAB visibility: ${b.fabCreateTema.isVisible}, User type: ${currentUser?.tipo}")
        b.fabCreateTema.setOnClickListener {
            Log.d("TemaFragment", "FAB clicked, opening create tema dialog")
            showCreateTemaDialog()
        }

        // Cargar temas en tiempo real
        listenTemas()
    }

    private fun displayAsignaturaData(asignatura: Asignatura) {
        b.toolbar.title = asignatura.nombre
        b.tvNombreAsignatura.text = asignatura.nombre
        b.tvCodigoAsignatura.text = asignatura.codigoAsignatura
        b.tvCreditosAsignatura.text = asignatura.creditos.toString()
        b.tvDescripcionAsignatura.text = asignatura.descripcion
        Log.d("TemaFragment", "Displaying asignatura data: ${asignatura.nombre}")

        // Cargar nombres de profesores
        scope.launch {
            try {
                val profesoresNombres = mutableListOf<String>()
                for (profesorId in asignatura.profesores) {
                    val snapshot = db.collection("usuarios").document(profesorId).get().await()
                    if (snapshot.exists()) {
                        val usuario = snapshot.toObject(Usuario::class.java)
                        val nombreCompleto = "${usuario?.nombre} ${usuario?.apellido ?: ""}".trim()
                        if (nombreCompleto.isNotEmpty()) profesoresNombres.add(nombreCompleto)
                    }
                }
                b.tvProfesoresAsignatura.text = if (profesoresNombres.isNotEmpty()) {
                    profesoresNombres.joinToString(", ")
                } else {
                    "Sin profesores"
                }
                b.tvProfesoresAsignatura.setTextColor(
                    requireContext().getColor(if (profesoresNombres.isNotEmpty()) R.color.azul_oscuro else R.color.gris_oscuro)
                )
                Log.d("TemaFragment", "Profesores loaded: ${b.tvProfesoresAsignatura.text}")
            } catch (e: Exception) {
                Log.e("TemaFragment", "Error loading profesores: ${e.message}", e)
                b.tvProfesoresAsignatura.text = "Error al cargar profesores"
                b.tvProfesoresAsignatura.setTextColor(requireContext().getColor(R.color.rojo_error))
            }
        }

        // Cargar nombres de estudiantes
        scope.launch {
            try {
                val estudiantesNombres = mutableListOf<String>()
                for (estudianteId in asignatura.estudiantes) {
                    val snapshot = db.collection("usuarios").document(estudianteId).get().await()
                    if (snapshot.exists()) {
                        val usuario = snapshot.toObject(Usuario::class.java)
                        val nombreCompleto = "${usuario?.nombre} ${usuario?.apellido ?: ""}".trim()
                        if (nombreCompleto.isNotEmpty()) estudiantesNombres.add(nombreCompleto)
                    }
                }
                b.tvEstudiantesAsignatura.text = if (estudiantesNombres.isNotEmpty()) {
                    estudiantesNombres.joinToString(", ")
                } else {
                    "Sin estudiantes"
                }
                b.tvEstudiantesAsignatura.setTextColor(
                    requireContext().getColor(if (estudiantesNombres.isNotEmpty()) R.color.azul_oscuro else R.color.gris_oscuro)
                )
                Log.d("TemaFragment", "Estudiantes loaded: ${b.tvEstudiantesAsignatura.text}")
            } catch (e: Exception) {
                Log.e("TemaFragment", "Error loading estudiantes: ${e.message}", e)
                b.tvEstudiantesAsignatura.text = "Error al cargar estudiantes"
                b.tvEstudiantesAsignatura.setTextColor(requireContext().getColor(R.color.rojo_error))
            }
        }
    }

    private fun listenTemas() {
        listenerReg?.remove()
        val query = db.collection("temas")
            .whereEqualTo("asignaturaId", asignatura?.id)
            .whereEqualTo("estado", true)
            .orderBy("fechaCreacion", Query.Direction.DESCENDING)
        Log.d("TemaFragment", "Setting up listener for temas with asignaturaId: ${asignatura?.id}")

        listenerReg = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("TemaFragment", "Error listening to temas: ${e.message}", e)
                b.tvEmptyTemas.text = getString(R.string.error_loading_temas)
                b.tvEmptyTemas.isVisible = true
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                Log.d("TemaFragment", "No temas found for asignaturaId: ${asignatura?.id}")
                temasAdapter.replaceAll(emptyList())
                b.tvEmptyTemas.text = getString(R.string.no_temas_available)
                b.tvEmptyTemas.isVisible = true
                return@addSnapshotListener
            }

            val temas = snapshot.documents.mapNotNull { doc ->
                val tema = doc.toObject(Tema::class.java)?.copy(id = doc.id)
                Log.d("TemaFragment", "Tema loaded: ${tema?.nombre}, ID: ${tema?.id}")
                tema
            }
            Log.d("TemaFragment", "Updating RecyclerView with ${temas.size} temas")
            temasAdapter.replaceAll(temas)
            b.tvEmptyTemas.isVisible = temas.isEmpty()
        }
    }

    private fun showCreateTemaDialog() {
        val dialogBinding = FormTemaBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.create_tema))
            .setView(dialogBinding.root)
            .setPositiveButton(getString(R.string.save), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialogBinding.tilNombre.error = null
            dialogBinding.tilDescripcion.error = null

            val nombre = dialogBinding.etNombre.text.toString().trim()
            val descripcion = dialogBinding.etDescripcion.text.toString().trim()

            var valid = true
            if (nombre.isEmpty()) {
                dialogBinding.tilNombre.error = "Ingrese el nombre del tema"
                valid = false
            }
            if (descripcion.isEmpty()) {
                dialogBinding.tilDescripcion.error = "Ingrese una descripción"
                valid = false
            }

            if (!valid) {
                Log.d("TemaFragment", "Validation failed: nombre=$nombre, descripcion=$descripcion")
                return@setOnClickListener
            }

            scope.launch {
                try {
                    val tema = Tema(
                        id = db.collection("temas").document().id,
                        asignaturaId = asignatura?.id ?: "",
                        nombre = nombre,
                        descripcion = descripcion,
                        estado = true,
                        fechaCreacion = Timestamp.now()
                    )
                    Log.d("TemaFragment", "Creating tema: ${tema.nombre}, asignaturaId: ${tema.asignaturaId}")

                    db.runTransaction { transaction ->
                        val temaRef = db.collection("temas").document(tema.id)
                        transaction.set(temaRef, tema)
                    }.await()

                    Log.d("TemaFragment", "Tema created successfully: ${tema.id}")
                    dialog.dismiss()
                    // No need to call listenTemas() as the snapshot listener will update the list
                } catch (e: Exception) {
                    Log.e("TemaFragment", "Error saving tema: ${e.message}", e)
                    dialogBinding.tilNombre.error = "Error al guardar: ${e.message}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        Log.d("TemaFragment", "Listener removed and view destroyed")
        _b = null
    }
}