package com.ramen73.ramenchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ramen73.ramenchat.R
import com.ramen73.ramenchat.databinding.ItemChatRoomBinding
import com.ramen73.ramenchat.model.ChatRoom
import com.ramen73.ramenchat.utils.toTimeString

class ChatListAdapter(
    private val onClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, ChatListAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemChatRoomBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(room: ChatRoom) {
            if (room.isGroup) {
                binding.tvName.text = room.groupName.ifEmpty { "Gruppo" }
                binding.ivAvatar.setImageResource(R.drawable.ic_group_avatar)
                binding.viewOnline.visibility = View.INVISIBLE
            } else {
                val user = room.otherUser
                binding.tvName.text = user.displayName.ifEmpty { "Utente sconosciuto" }
                binding.viewOnline.visibility = if (user.online) View.VISIBLE else View.INVISIBLE
                if (user.photoUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(user.photoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_default_avatar)
                        .into(binding.ivAvatar)
                } else {
                    binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
            }

            binding.tvLastMessage.text = room.lastMessage.ifEmpty { "Inizia la conversazione!" }
            binding.tvTime.text = if (room.lastMessageTime > 0) room.lastMessageTime.toTimeString() else ""
            binding.root.setOnClickListener { onClick(room) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ChatRoom>() {
            override fun areItemsTheSame(old: ChatRoom, new: ChatRoom) = old.chatId == new.chatId
            override fun areContentsTheSame(old: ChatRoom, new: ChatRoom) = old == new
        }
    }
}
