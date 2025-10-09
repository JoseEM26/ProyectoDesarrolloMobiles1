package com.example.computronica

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.UsuarioAdapter
import com.example.computronica.Model.TipoUsuario
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityUsuariosBinding
import com.example.computronica.databinding.FormUsuariosBinding

class Usuarios_Activity : AppCompatActivity() {


    private lateinit var adapter: UsuarioAdapter
    private lateinit var binding: ActivityUsuariosBinding

    private var dialogBinding: FormUsuariosBinding? = null
    private var pickedImageUri: Uri? = null
    private val usuarios= mutableListOf<Usuario>()

    private var nextId = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Inicializar el adapter y vincularlo al RecyclerView
        adapter = UsuarioAdapter(usuarios) {
            // Callback cuando cambia la lista, opcional
        }

        binding.rvUsuarios.adapter = adapter
        binding.rvUsuarios.layoutManager = LinearLayoutManager(this)

        // Botón para abrir el formulario
        binding.btnUsuarioCrear.setOnClickListener { openNuevoUsuarioForm() }
    }




    private fun openNuevoUsuarioForm(){
        val b = FormUsuariosBinding.inflate(layoutInflater)
        dialogBinding = b

        // Traemos el array de tipoUsuario desde los recursos
        val listAdapterTipo = ArrayAdapter(this, android.R.layout.simple_selectable_list_item, resources.getStringArray(R.array.tipoUsuario))
        val listAdapterSede = ArrayAdapter(this, android.R.layout.simple_selectable_list_item, resources.getStringArray(R.array.sedes))
        b.spnTipo.adapter = listAdapterTipo
        b.spnSede.adapter = listAdapterSede

        // Crear el diálogo
        val dlg = AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_alert_create))
            .setView(b.root)  // Establecer el layout del diálogo
            .setPositiveButton(getString(R.string.btn_crear_nuevo_usuario), null) // Guardar se maneja manualmente
            .setNegativeButton(getString(R.string.btn_cancelar)) { d, _ ->
                // Cerrar el diálogo sin guardar
                dialogBinding = null
                d.dismiss()
            }
            .create()

        dlg.setOnShowListener{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{

                val nombre = b.etNombre.text?.toString()?.trim().orEmpty()
                val apellido = b.etApellido.text?.toString()?.trim().orEmpty()
                val sede = b.spnSede.selectedItem?.toString()?.trim().orEmpty()
                val activo = b.swtEstado.isChecked
                val correoInstitucional = b.etCorreoInstitutcional.text?.toString()?.trim().orEmpty()
                val codigoInstitucional = b.etCodigoInst.text?.toString()?.trim().orEmpty()
                val contrasena = b.etContrasena.text?.toString()?.trim().orEmpty()
                val tipoStr = b.spnTipo.selectedItem?.toString()?.trim().orEmpty()

                b.titNombre.error = null
                b.titApellido.error = null
                b.titCorreoINstitucional.error = null
                b.titCodigoIns.error = null
                b.titContrasena.error = null
                //(b.spnSede.selectedView as? TextView)?.setTextColor(Color.BLACK)
                //(b.spnTipo.selectedView as? TextView)?.setTextColor(Color.BLACK)

                var ok = true

                if (nombre.isEmpty()) {
                    b.titNombre.error = getString(R.string.msg_campo_requerido)
                    ok = false
                }

                if (apellido.isEmpty()) {
                    b.titApellido.error = getString(R.string.msg_campo_requerido)
                    ok = false
                }

                if (correoInstitucional.isEmpty() || !correoInstitucional.contains("@")) {
                    b.titCorreoINstitucional.error = getString(R.string.msg_correo_invalido)
                    ok = false
                }

                if (codigoInstitucional.isEmpty()) {
                    b.titCodigoIns.error = getString(R.string.msg_campo_requerido)
                    ok = false
                }

                if (contrasena.isEmpty() || contrasena.length < 6) {
                    b.titContrasena.error = getString(R.string.msg_contrasena_invalida) // Debes definir este string
                    ok = false
                }

                if (sede == "Seleccionar..." || sede.isEmpty()) {
                    (b.spnSede.selectedView as? TextView)?.setTextColor(Color.RED)
                    Toast.makeText(this, "Debe seleccionar una sede válida", Toast.LENGTH_SHORT).show()
                    ok = false
                }

                if (tipoStr == "Seleccionar..." || tipoStr.isEmpty()) {
                    (b.spnTipo.selectedView as? TextView)?.setTextColor(Color.RED)
                    Toast.makeText(this, "Debe seleccionar un tipo de usuario válido", Toast.LENGTH_SHORT).show()
                    ok = false
                }

                if (!ok) {
                    Toast.makeText(b.root.context, getString(R.string.msg_ocurrio_error), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val newUsuario = Usuario(
                    id = usuarios.size + 1, // O usa un generador único si lo tienes
                    codigoInstitucional = codigoInstitucional,
                    sede = sede,
                    nombre = nombre,
                    apellido = apellido,
                    correoInstitucional = correoInstitucional,
                    contrasena = contrasena,
                    tipo = TipoUsuario.valueOf(tipoStr.lowercase()), // Asegúrate de que coincide con el enum
                    estado = activo,
                    imgURI = pickedImageUri
                )


                adapter.add(newUsuario)

                dialogBinding = null
                pickedImageUri = null
                dlg.dismiss()
            }
        }

        dlg.show() // Mostrar el diálogo
    }


}
