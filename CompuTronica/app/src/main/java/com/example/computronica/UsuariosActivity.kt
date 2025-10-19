package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Adapter.UsuarioAdapter
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityUsuariosBinding
import com.example.computronica.databinding.FormUsuariosBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class UsuariosActivity : Fragment() {

    private var _b: ActivityUsuariosBinding? = null
    private val b get() = _b!!
    private var ui = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val adapter = UsuarioAdapter(mutableListOf(), onEdit = this::showEditDialog)
    private var listenerReg: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityUsuariosBinding.inflate(inflater, container, false)

        // Configurar RecyclerView
        b.rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        b.rvUsuarios.adapter = adapter
        b.rvUsuarios.addItemDecoration(
            DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        )

        // Botón para crear nuevo usuario
        b.btnUsuarioCrear.setOnClickListener { showCreateDialog() }

        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listarUsuarios()
    }

    // 🔹 Listar usuarios desde Firestore
    private fun listarUsuarios() {
        listenerReg?.remove()
        listenerReg = db.collection("usuarios")
            .orderBy("nombre", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    toast("Error al cargar usuarios: ${e.message}")
                    return@addSnapshotListener
                }

                val list = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Usuario::class.java)?.apply {
                        id = doc.id // Genera ID entero único
                    }
                }.orEmpty()

                adapter.replaceAll(list)
                b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    // 🔹 Crear usuario nuevo
    private fun showCreateDialog() {
        val dialogBinding = FormUsuariosBinding.inflate(layoutInflater)

        val tipos = resources.getStringArray(R.array.tipoUsuario)
        dialogBinding.spnTipo.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            tipos
        )

        // Spinner para sede (desde strings.xml)
        val sedes = resources.getStringArray(R.array.sedes)
        dialogBinding.spnSede.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            sedes
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Registrar Usuario")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = dialogBinding.etNombre.text.toString().trim()
                val apellido = dialogBinding.etApellido.text.toString().trim()
                val correo = dialogBinding.etCorreoInstitutcional.text.toString().trim()
                val codigo = dialogBinding.etCodigoInst.text.toString().trim()
                val contrasena = dialogBinding.etContrasena.text.toString().trim()
                val tipo = dialogBinding.spnTipo.selectedItem.toString()
                val sede = dialogBinding.spnSede.selectedItem.toString()

                if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty()) {
                    toast("Completa todos los campos obligatorios")
                    return@setPositiveButton
                }

                val nuevoUsuario = Usuario(
                    id = 0.toString(), // Se genera luego en Firestore
                    codigoInstitucional = codigo,
                    sede = sede,
                    nombre = nombre,
                    apellido = apellido,
                    correoInstitucional = correo,
                    contrasena = contrasena,
                    tipo = TipoUsuario.valueOf(tipo),
                    estado = true,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                db.collection("usuarios")
                    .add(nuevoUsuario)
                    .addOnSuccessListener {
                        toast("Usuario registrado correctamente")
                    }
                    .addOnFailureListener { e ->
                        toast("Error al guardar: ${e.message}")
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // 🔹 Editar usuario (pendiente de implementación)
    private fun showEditDialog(usuario: Usuario) {
        toast("Función editar próximamente 😉")
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        ui.cancel()
        listenerReg?.remove()
        _b = null
    }
}
