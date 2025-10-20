package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.MessageModel
import com.example.computronica.R
import com.google.firebase.auth.FirebaseAuth

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val messageList = mutableListOf<MessageModel>()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    fun add(message: MessageModel) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }

    fun clear() {
        messageList.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == FirebaseAuth.getInstance().currentUser?.uid){
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder{
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.message_row_sent
        } else {
            R.layout.message_row_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int){
        val message = messageList[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messageList.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val textViewSentMessage: TextView? = itemView.findViewById(R.id.textViewSentMessage)
        private val textViewReceivedMessage: TextView? = itemView.findViewById(R.id.textViewReceivedMessage)

        fun bind(message: MessageModel){
            textViewSentMessage?.text = message.message
            textViewReceivedMessage?.text = message.message
        }
    }
}