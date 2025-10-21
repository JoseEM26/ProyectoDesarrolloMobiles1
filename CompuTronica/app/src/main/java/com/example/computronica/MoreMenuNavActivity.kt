package com.example.computronica

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.computronica.Model.Usuario
import com.example.computronica.databinding.ActivityMoreMenuNavBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class MoreMenuNavActivity : Fragment() {
    private var _b: ActivityMoreMenuNavBinding? = null
    private val b get() = _b!!
    private var ui = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = ActivityMoreMenuNavBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val usuario: Usuario? = SessionManager.currentUser

        if (usuario != null) {
            b.txtNombreUsuario.text = "${usuario.nombre} ${usuario.apellido}"
        } else {
            b.txtNombreUsuario.text = getString(R.string.msg_usuarioNoLogueado)
        }

        val mainActivity = requireActivity() as MainActivity

        b.btnChat.setOnClickListener {
            mainActivity.changeFrame(ChatActivity())
            mainActivity.supportActionBar?.title = "Chat Institucional 💬"
        }

        b.btnPresentacion.setOnClickListener {
            mainActivity.changeFrame(PresentacionActivity())
            mainActivity.supportActionBar?.title = "Presentación"
        }

        b.btnUsuarios.setOnClickListener {
            mainActivity.changeFrame(UsuariosActivity())
            mainActivity.supportActionBar?.title = "Gestión de Usuarios"
        }

        b.btnPerfil.setOnClickListener {
            mainActivity.changeFrame(PerfilActivity())
            mainActivity.supportActionBar?.title = "Mi Perfil"
        }

        b.btnLogOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            SessionManager.clearSession()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui.cancel()
        _b = null
    }
}
