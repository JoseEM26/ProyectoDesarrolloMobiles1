package com.example.computronica

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.computronica.Adapter.MessageAdapter
import com.example.computronica.Model.MessageModel
import com.example.computronica.databinding.ActivityChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.*

class ChatFragment : Fragment() {

    private var _binding: ActivityChatBinding? = null
    private val binding get() = _binding!!
    private var uiScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var dbReferenceSender: DatabaseReference
    private lateinit var dbReferenceReceiver: DatabaseReference

    private var receiverId: String = ""
    private var receiverName: String = ""
    private var senderRoom: String = ""
    private var receiverRoom: String = ""

    companion object {
        private const val ARG_RECEIVER_ID = "receiver_id"
        private const val ARG_RECEIVER_NAME = "receiver_name"

        fun newInstance(receiverId: String, receiverName: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString(ARG_RECEIVER_ID, receiverId)
            args.putString(ARG_RECEIVER_NAME, receiverName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receiverId = it.getString(ARG_RECEIVER_ID, "")
            receiverName = it.getString(ARG_RECEIVER_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupChat()
        setupRecyclerView()
        loadMessages()
        setupSendButton()
    }

    private fun setupToolbar() {
        binding.toolbar.title = receiverName
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupChat() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        senderRoom = "$currentUserId$receiverId"
        receiverRoom = "$receiverId$currentUserId"

        dbReferenceSender = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(senderRoom)

        dbReferenceReceiver = FirebaseDatabase.getInstance()
            .getReference("chats")
            .child(receiverRoom)
    }

    private fun setupRecyclerView() {
        messageAdapter = MessageAdapter()
        binding.chatRecycler.apply {
            adapter = messageAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadMessages() {
        dbReferenceSender.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<MessageModel>()

                for (dataSnapshot in snapshot.children) {
                    val messageModel = dataSnapshot.getValue(MessageModel::class.java)
                    if (messageModel != null) {
                        messages.add(messageModel)
                    }
                }

                // Ordenar por timestamp
                messages.sortBy { it.timestamp }

                messageAdapter.clear()
                messages.forEach { messageAdapter.add(it) }

                // Scroll al último mensaje
                if (messageAdapter.itemCount > 0) {
                    binding.chatRecycler.scrollToPosition(messageAdapter.itemCount - 1)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Error al cargar mensajes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setupSendButton() {
        binding.sendMessageIcon.setOnClickListener {
            val message = binding.messageEdit.text.toString().trim()

            if (message.isNotEmpty()) {
                sendMessage(message)
            } else {
                Toast.makeText(
                    requireContext(),
                    "El mensaje no puede estar vacío",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun sendMessage(message: String) {
        val messageId = UUID.randomUUID().toString()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val messageModel = MessageModel(
            messageId = messageId,
            senderId = currentUserId,
            message = message,
            timestamp = System.currentTimeMillis()
        )

        // Enviar a ambos rooms
        dbReferenceSender.child(messageId).setValue(messageModel)
            .addOnSuccessListener {
                dbReferenceReceiver.child(messageId).setValue(messageModel)
                binding.messageEdit.text?.clear()
            }
            .addOnFailureListener {
                Toast.makeText(
                    requireContext(),
                    "Error al enviar mensaje",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiScope.cancel()
        _binding = null
    }
}