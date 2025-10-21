package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.UsersAdapter
import com.example.computronica.Model.UserModel
import com.example.computronica.databinding.ActivityUsersChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class UsersChatFragment : Fragment() {

    private var _binding: ActivityUsersChatBinding? = null
    private val binding get() = _binding!!
    private var uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var usersAdapter: UsersAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityUsersChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        loadUsers()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        usersAdapter = UsersAdapter { user ->
            openChat(user)
        }
        binding.recyclerViewUsers.apply {
            adapter = usersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadUsers() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // ✅ USANDO FIRESTORE: Colección "usuarios"
        db.collection("usuarios")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar usuarios: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    usersAdapter.clear()

                    for (document in snapshot.documents) {
                        val userId = document.id

                        // Mapear desde Firestore a UserModel
                        val nombre = document.getString("nombre") ?: ""
                        val apellido = document.getString("apellido") ?: ""
                        val email = document.getString("email") ?: ""

                        // Crear UserModel para el chat
                        val userModel = UserModel(
                            userID = userId,
                            userName = "$nombre $apellido",
                            userEmail = email
                        )

                        // No mostrar el usuario actual
                        if (userId != currentUserId) {
                            usersAdapter.add(userModel)
                        }
                    }
                }
            }
    }

    private fun openChat(user: UserModel) {
        val chatFragment = ChatFragment.newInstance(user.userID, user.userName)
        parentFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, chatFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }
}