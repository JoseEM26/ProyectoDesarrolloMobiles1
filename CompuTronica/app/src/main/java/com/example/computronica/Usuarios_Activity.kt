package com.example.computronica

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.computronica.databinding.ActivityUsuariosBinding
import com.example.computronica.databinding.FormUsuariosBinding

class Usuarios_Activity : AppCompatActivity() {

    private lateinit var binding: ActivityUsuariosBinding
    private var dialogBinding: FormUsuariosBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsuariosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnUsuarioCrear.setOnClickListener {
            val b = FormUsuariosBinding.inflate(layoutInflater)
            dialogBinding = b

            // Traemos el array de tipoUsuario desde los recursos
            val tipoUsuario = resources.getStringArray(R.array.tipoUsuario)
            val listAdapter = ArrayAdapter(this, android.R.layout.simple_selectable_list_item, tipoUsuario)
            b.spnTipo.adapter = listAdapter

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

            dlg.show() // Mostrar el diálogo
        }
    }
}
