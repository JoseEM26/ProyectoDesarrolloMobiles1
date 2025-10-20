package com.example.computronica

import android.os.Bundle
import android.view.*
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class UsuariosActivity : Fragment() {

    private var _b: ActivityUsuariosBinding? = null
    private val b get() = _b!!
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var listenerReg: ListenerRegistration? = null

    private val adapter = UsuarioAdapter(
        mutableListOf(),
        onEdit = { usuario -> showEditDialog(usuario) },
        onDelete = { usuario -> eliminarUsuario(usuario) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityUsuariosBinding.inflate(inflater, container, false)

        b.rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        b.rvUsuarios.adapter = adapter
        b.rvUsuarios.addItemDecoration(
            DividerItemDecoration(requireContext(), RecyclerView.VERTICAL)
        )

        b.btnUsuarioCrear.setOnClickListener { showCreateDialog() }

        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listarUsuarios()
    }

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
                    doc.toObject(Usuario::class.java)?.apply { id = doc.id }
                }.orEmpty()

                adapter.replaceAll(list)
                b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun showCreateDialog() {
        val dialogBinding = FormUsuariosBinding.inflate(layoutInflater)

        val tipos = resources.getStringArray(R.array.tipoUsuario)
        val sedes = resources.getStringArray(R.array.sedes)

        dialogBinding.spnTipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)
        dialogBinding.spnSede.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sedes)

        AlertDialog.Builder(requireContext())
            .setTitle("Registrar Usuario")
            .setView(dialogBinding.root)
            .setPositiveButton("Guardar") { _, _ ->
                val usuario = Usuario(
                    id = db.collection("usuarios").document().id,
                    codigoInstitucional = dialogBinding.etCodigoInst.text.toString().trim(),
                    sede = dialogBinding.spnSede.selectedItem.toString(),
                    nombre = dialogBinding.etNombre.text.toString().trim(),
                    apellido = dialogBinding.etApellido.text.toString().trim(),
                    correoInstitucional = dialogBinding.etCorreoInstitutcional.text.toString().trim(),
                    contrasena = dialogBinding.etContrasena.text.toString().trim(),
                    tipo = TipoUsuario.valueOf(dialogBinding.spnTipo.selectedItem.toString()),
                    estado = dialogBinding.swtEstado.isActivated,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                db.collection("usuarios")
                    .add(usuario)
                    .addOnSuccessListener { toast("✅ Usuario registrado") }
                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditDialog(usuario: Usuario) {
        val dialogBinding = FormUsuariosBinding.inflate(layoutInflater)
        val tipos = resources.getStringArray(R.array.tipoUsuario)
        val sedes = resources.getStringArray(R.array.sedes)

        dialogBinding.etNombre.setText(usuario.nombre)
        dialogBinding.etApellido.setText(usuario.apellido)
        dialogBinding.etCorreoInstitutcional.setText(usuario.correoInstitucional)
        dialogBinding.etCodigoInst.setText(usuario.codigoInstitucional)
        dialogBinding.etContrasena.setText(usuario.contrasena)
        dialogBinding.swtEstado.isChecked = usuario.estado

        dialogBinding.spnTipo.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, tipos)
        dialogBinding.spnSede.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, sedes)

        dialogBinding.spnTipo.setSelection(tipos.indexOf(usuario.tipo.name))
        dialogBinding.spnSede.setSelection(sedes.indexOf(usuario.sede))


        AlertDialog.Builder(requireContext())
            .setTitle("Editar Usuario")
            .setView(dialogBinding.root)
            .setPositiveButton("Actualizar") { _, _ ->
                val updates = mapOf(
                    "nombre" to dialogBinding.etNombre.text.toString().trim(),
                    "apellido" to dialogBinding.etApellido.text.toString().trim(),
                    "correoInstitucional" to dialogBinding.etCorreoInstitutcional.text.toString().trim(),
                    "codigoInstitucional" to dialogBinding.etCodigoInst.text.toString().trim(),
                    "contrasena" to dialogBinding.etContrasena.text.toString().trim(),
                    "sede" to dialogBinding.spnSede.selectedItem.toString(),
                    "tipo" to dialogBinding.spnTipo.selectedItem.toString(),
                    "estado" to dialogBinding.swtEstado.isChecked,
                    "updatedAt" to Timestamp.now()
                )

                db.collection("usuarios").document(usuario.id)
                    .update(updates)
                    .addOnSuccessListener { toast("✅ Usuario actualizado") }
                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarUsuario(usuario: Usuario) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Usuario")
            .setMessage("¿Deseas eliminar a ${usuario.nombre} ${usuario.apellido}?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.collection("usuarios").document(usuario.id)
                    .delete()
                    .addOnSuccessListener { toast("🗑️ Usuario eliminado") }
                    .addOnFailureListener { e -> toast("❌ Error: ${e.message}") }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
        _b = null
    }
}
