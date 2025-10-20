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
            b.txtNombreUsuario.text=usuario.nombre+" "+usuario.apellido
        }else{
            b.txtNombreUsuario.text=getString(R.string.msg_usuarioNoLogueado)
        }

        b.btnChat.setOnClickListener {
            (requireActivity() as MainActivity).changeFrame(ChatActivity())
        }

        b.btnPresentacion.setOnClickListener {
            (requireActivity() as MainActivity).changeFrame(PresentacionActivity())
        }

        b.btnUsuarios.setOnClickListener {
            (requireActivity() as MainActivity).changeFrame(UsuariosActivity())
        }

        b.btnPerfil.setOnClickListener {
            (requireActivity() as MainActivity).changeFrame(PerfilActivity())
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
