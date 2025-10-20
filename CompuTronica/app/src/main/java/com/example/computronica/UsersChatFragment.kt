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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class UsersChatFragment : Fragment() {

    private var _binding: ActivityUsersChatBinding? = null
    private val binding get() = _binding!!
    private var uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var usersAdapter: UsersAdapter

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
        val databaseReference = FirebaseDatabase.getInstance().getReference("users")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                usersAdapter.clear()
                for (dataSnapshot in snapshot.children) {
                    val userId = dataSnapshot.key ?: continue
                    val userModel = dataSnapshot.getValue(UserModel::class.java)

                    if (userModel != null) {
                        // Asegurar que el UserID esté configurado
                        if (userModel.userID.isEmpty()) {
                            userModel.userID = userId
                        }

                        // No mostrar el usuario actual en la lista
                        if (userId != currentUserId) {
                            usersAdapter.add(userModel)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar usuarios",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
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