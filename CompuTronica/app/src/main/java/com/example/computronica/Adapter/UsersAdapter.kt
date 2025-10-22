package com.example.computronica.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.computronica.Model.UserModel
import com.example.computronica.R

class UsersAdapter(
    private val onUserClick: (UserModel) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private val userList = mutableListOf<UserModel>()

    fun add(user: UserModel){
        userList.add(user)
        notifyItemInserted(userList.size - 1)
    }

    fun clear() {
        userList.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_row, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int){
        val user = userList[position]
        holder.bind(user, onUserClick)
    }

    override fun getItemCount(): Int = userList.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val username: TextView = itemView.findViewById(R.id.username)
        private val useremail: TextView = itemView.findViewById(R.id.useremail)

        fun bind(user: UserModel, onClick: (UserModel) -> Unit){
            username.text = user.userName
            useremail.text = user.userEmail

            itemView.setOnClickListener{
                onClick(user)
            }
        }
    }
}