package com.example.computronica

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.computronica.databinding.ActivityPresentacionBinding
import com.example.computronica.databinding.ActivityRegisterBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PresentacionActivity : Fragment() {
    private var _b: ActivityPresentacionBinding?=null
    private val b get() = _b!!
    private var ui= CoroutineScope(Dispatchers.Main+ SupervisorJob())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _b= ActivityPresentacionBinding.inflate(inflater,container,false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        b.cardSede.setOnClickListener {
            try {
                val direccion = "Instituto CompuTronica, Lima, Perú"
                val gmmIntentUri = Uri.parse("geo:0,0?q=${Uri.encode(direccion)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                startActivity(mapIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No se encontró una app de mapas instalada", Toast.LENGTH_SHORT).show()
            }
        }

        b.cardTelefono.setOnClickListener {
            val numero = "+51987654321"
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$numero")
            startActivity(intent)
        }

        b.cardCorreo.setOnClickListener {
            try {
                val correo = "contacto@computronica.edu.pe"
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$correo")
                    putExtra(Intent.EXTRA_SUBJECT, "Consulta desde la app CompuTronica")
                    putExtra(Intent.EXTRA_TEXT, "Hola, quisiera más información sobre...")
                }
                startActivity(Intent.createChooser(intent, "Enviar correo con..."))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(requireContext(), "No hay aplicaciones de correo instaladas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ui.cancel()
        _b=null
    }
}