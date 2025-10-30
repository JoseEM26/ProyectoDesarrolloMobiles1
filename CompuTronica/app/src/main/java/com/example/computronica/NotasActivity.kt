package com.example.computronica

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.CalificacionesAdapter
import com.example.computronica.Model.Asignatura
import com.example.computronica.Model.Calificacion
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityNotasBinding
import com.example.computronica.databinding.ItemEstudianteNotaBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NotasActivity : Fragment() {

    private var _b: ActivityNotasBinding? = null
    private val b get() = _b!!
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var asignatura: Asignatura? = null
    private var estudiantesMap = mutableMapOf<String, Usuario>()
    private var listener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = ActivityNotasBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        asignatura = arguments?.getSerializable("asignatura") as? Asignatura
        if (asignatura == null) {
            Toast.makeText(requireContext(), "Error: Asignatura no encontrada", Toast.LENGTH_SHORT).show()
            return
        }

        b.toolbar.title = "Notas - ${asignatura!!.nombre}"
        b.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cargarEstudiantesYNotas()
    }

    private fun cargarEstudiantesYNotas() {
        scope.launch {
            try {
                // Cargar estudiantes
                val estudiantesIds = asignatura!!.estudiantes
                if (estudiantesIds.isEmpty()) {
                    b.tvEmpty.text = "No hay estudiantes inscritos"
                    b.tvEmpty.visibility = View.VISIBLE
                    return@launch
                }

                val usuariosSnapshot = db.collection("usuarios")
                    .whereIn("id", estudiantesIds)
                    .get().await()

                estudiantesMap.clear()
                for (doc in usuariosSnapshot.documents) {
                    val usuario = doc.toObject(Usuario::class.java)?.copy(id = doc.id)
                    if (usuario != null) estudiantesMap[usuario.id] = usuario
                }

                // Escuchar notas
                listener?.remove()
                listener = db.collection("calificaciones")
                    .whereEqualTo("asignaturaId", asignatura!!.id)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("NotasActivity", "Error: ${e.message}")
                            return@addSnapshotListener
                        }

                        val notasMap = mutableMapOf<String, MutableList<Calificacion>>()
                        snapshot?.documents?.forEach { doc ->
                            val cal = doc.toObject(Calificacion::class.java)?.copy(id = doc.id)
                            if (cal != null) {
                                notasMap.getOrPut(cal.estudianteId) { mutableListOf() }.add(cal)
                            }
                        }

                        mostrarEstudiantes(notasMap)
                    }

            } catch (e: Exception) {
                Log.e("NotasActivity", "Error: ${e.message}")
            }
        }
    }

    private fun mostrarEstudiantes(notasMap: Map<String, List<Calificacion>>) {
        b.llEstudiantes.removeAllViews()

        val esProfesorOAdmin = SessionManager.currentUser?.tipo.let {
            it == TipoUsuario.profesor || it == TipoUsuario.administrativo
        }

        for ((estudianteId, usuario) in estudiantesMap) {
            val binding = ItemEstudianteNotaBinding.inflate(layoutInflater)
            binding.tvNombreEstudiante.text = "${usuario.nombre} ${usuario.apellido}"

            val notas = notasMap[estudianteId].orEmpty()
            binding.containerNotas.removeAllViews()

            val adapter = CalificacionesAdapter(
                notas.toMutableList(),
                onEdit = { cal -> showEditNotaDialog(cal, usuario) },
                onDelete = { cal -> eliminarNota(cal) }
            )

            notas.forEach { cal ->
                val notaView = layoutInflater.inflate(R.layout.item_nota, binding.containerNotas, false)
                notaView.findViewById<TextView>(R.id.tvTipoNota).text = cal.evaluacion
                notaView.findViewById<TextView>(R.id.tvNota).text = String.format("%.1f", cal.nota)
                notaView.findViewById<ImageButton>(R.id.btnEditNota).setOnClickListener {
                    if (esProfesorOAdmin) showEditNotaDialog(cal, usuario)
                }
                notaView.findViewById<ImageButton>(R.id.btnDeleteNota).setOnClickListener {
                    if (esProfesorOAdmin) eliminarNota(cal)
                }
                binding.containerNotas.addView(notaView)
            }

            if (esProfesorOAdmin) {
                binding.btnAgregarNota.visibility = View.VISIBLE
                binding.btnAgregarNota.setOnClickListener {
                    showCreateNotaDialog(estudianteId, usuario)
                }
            } else {
                binding.btnAgregarNota.visibility = View.GONE
            }

            // Promedio
            val promedio = notas.map { it.nota }.average()
            if (notas.isNotEmpty()) {
                val tvPromedio = TextView(requireContext()).apply {
                    text = "Promedio: ${String.format("%.2f", promedio)}"
                    setTextColor(requireContext().getColor(R.color.azul_oscuro))
                    textSize = 14f
                    setPadding(0, 8, 0, 0)
                }
                binding.containerNotas.addView(tvPromedio)
            }

            b.llEstudiantes.addView(binding.root)
        }

        b.tvEmpty.visibility = if (estudiantesMap.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCreateNotaDialog(estudianteId: String, usuario: Usuario) {
        val tipos = resources.getStringArray(R.array.tipoAsignatura).filter { it != "Seleccionar..." }
        val view = layoutInflater.inflate(R.layout.form_nota, null)
        val spTipo = view.findViewById<Spinner>(R.id.spTipoEvaluacion)
        val etNota = view.findViewById<EditText>(R.id.etNota)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("Agregar Nota - ${usuario.nombre}")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val tipo = spTipo.selectedItem?.toString() ?: return@setPositiveButton
                val notaStr = etNota.text.toString()
                val nota = notaStr.toDoubleOrNull()

                if (nota == null || nota < 0 || nota > 20) {
                    Toast.makeText(requireContext(), "Nota debe estar entre 0 y 20", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val cal = Calificacion(
                    id = db.collection("calificaciones").document().id,
                    estudianteId = estudianteId,
                    asignaturaId = asignatura!!.id,
                    evaluacion = tipo,
                    nota = nota,
                    fecha = Timestamp.now()
                )

                db.collection("calificaciones").document(cal.id).set(cal)
                    .addOnSuccessListener { Toast.makeText(requireContext(), "Nota guardada", Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditNotaDialog(cal: Calificacion, usuario: Usuario) {
        val tipos = resources.getStringArray(R.array.tipoAsignatura).filter { it != "Seleccionar..." }
        val view = layoutInflater.inflate(R.layout.form_nota, null)
        val spTipo = view.findViewById<Spinner>(R.id.spTipoEvaluacion)
        val etNota = view.findViewById<EditText>(R.id.etNota)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tipos)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapter
        spTipo.setSelection(tipos.indexOf(cal.evaluacion))
        etNota.setText(cal.nota.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Nota - ${usuario.nombre}")
            .setView(view)
            .setPositiveButton("Actualizar") { _, _ ->
                val tipo = spTipo.selectedItem?.toString() ?: return@setPositiveButton
                val notaStr = etNota.text.toString()
                val nota = notaStr.toDoubleOrNull()

                if (nota == null || nota < 0 || nota > 20) {
                    Toast.makeText(requireContext(), "Nota debe estar entre 0 y 20", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                db.collection("calificaciones").document(cal.id)
                    .update("tipoEvaluacion", tipo, "nota", nota)
                    .addOnSuccessListener { Toast.makeText(requireContext(), "Nota actualizada", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarNota(cal: Calificacion) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Nota")
            .setMessage("¿Eliminar esta nota?")
            .setPositiveButton("Sí") { _, _ ->
                db.collection("calificaciones").document(cal.id).delete()
                    .addOnSuccessListener { Toast.makeText(requireContext(), "Nota eliminada", Toast.LENGTH_SHORT).show() }
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _b = null
    }
}