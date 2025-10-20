package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityPerfilBinding

class PerfilActivity : Fragment() {

    private var _b: ActivityPerfilBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityPerfilBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtiene el usuario actual desde SessionManager
        val usuario: Usuario? = SessionManager.currentUser

        if (usuario != null) {
            b.txtPerfilNombreHeader.text = "${usuario.nombre} ${usuario.apellido}"
            b.txtPerfilCodigo.text = usuario.codigoInstitucional
            b.txtPerfilCorreo.text = usuario.correoInstitucional
            b.txtUsuariosSede.text = usuario.sede
            b.txtPerfilTipo.text = usuario.tipo.name.capitalize()
            b.txtPerfilEstado.text = if (usuario.estado) "Activo" else "Inactivo"
        } else {
            // Opcional: mostrar un mensaje si no hay usuario activo
            b.txtPerfilNombreHeader.text = "Sin sesión"
            b.txtPerfilCodigo.text = "-"
            b.txtPerfilCorreo.text = "-"
            b.txtUsuariosSede.text = "-"
            b.txtPerfilTipo.text = "-"
            b.txtPerfilEstado.text = "-"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
