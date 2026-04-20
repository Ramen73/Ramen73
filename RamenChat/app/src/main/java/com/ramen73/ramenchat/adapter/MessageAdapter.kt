package com.ramen73.ramenchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ramen73.ramenchat.databinding.ItemMessageReceivedBinding
import com.ramen73.ramenchat.databinding.ItemMessageSentBinding
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.utils.toFullTimeString

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val VIEW_SENT = 1
        private const val VIEW_RECEIVED = 2

        val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(old: Message, new: Message) = old.messageId == new.messageId
            override fun areContentsTheSame(old: Message, new: Message) = old == new
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_SENT else VIEW_RECEIVED
    }

    inner class SentVH(val binding: ItemMessageSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.tvMessage.text = msg.text
            binding.tvTime.text = msg.timestamp.toFullTimeString()
        }
    }

    inner class ReceivedVH(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.tvMessage.text = msg.text
            binding.tvTime.text = msg.timestamp.toFullTimeString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_SENT) {
            SentVH(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedVH(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentVH -> holder.bind(getItem(position))
            is ReceivedVH -> holder.bind(getItem(position))
        }
    }
}
