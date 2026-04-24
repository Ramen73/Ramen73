package com.ramen73.ramenchat.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ramen73.ramenchat.databinding.ItemMessageReceivedBinding
import com.ramen73.ramenchat.databinding.ItemMessageSentBinding
import com.ramen73.ramenchat.model.Message
import com.ramen73.ramenchat.utils.AudioPlayer
import com.ramen73.ramenchat.utils.toFullTimeString
import java.util.Locale

class MessageAdapter(
    private val currentUserId: String
) : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    var isGroup: Boolean = false

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
            bindCommon(
                binding.tvMessage, binding.tvTime,
                binding.audioContainer, binding.btnPlayPause, binding.tvDuration,
                null,
                msg
            ) { notifyItemChanged(bindingAdapterPosition) }
        }
    }

    inner class ReceivedVH(val binding: ItemMessageReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            if (isGroup && msg.senderName.isNotEmpty()) {
                binding.tvSenderName.visibility = View.VISIBLE
                binding.tvSenderName.text = msg.senderName
            } else {
                binding.tvSenderName.visibility = View.GONE
            }
            bindCommon(
                binding.tvMessage, binding.tvTime,
                binding.audioContainer, binding.btnPlayPause, binding.tvDuration,
                binding.tvSenderName,
                msg
            ) { notifyItemChanged(bindingAdapterPosition) }
        }
    }

    private fun bindCommon(
        tvMessage: android.widget.TextView,
        tvTime: android.widget.TextView,
        audioContainer: View,
        btnPlayPause: android.widget.ImageView,
        tvDuration: android.widget.TextView,
        @Suppress("UNUSED_PARAMETER") tvSenderName: android.widget.TextView?,
        msg: Message,
        notifyChange: () -> Unit
    ) {
        tvTime.text = msg.timestamp.toFullTimeString()
        when (msg.type) {
            Message.TYPE_AUDIO -> {
                tvMessage.visibility = View.GONE
                audioContainer.visibility = View.VISIBLE
                val secs = (msg.audioDuration / 1000).toInt()
                tvDuration.text = String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60)
                btnPlayPause.setImageResource(
                    if (AudioPlayer.isPlaying(msg.audioUrl)) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play
                )
                audioContainer.setOnClickListener {
                    val started = AudioPlayer.toggle(msg.audioUrl) { notifyChange() }
                    btnPlayPause.setImageResource(
                        if (started) android.R.drawable.ic_media_pause
                        else android.R.drawable.ic_media_play
                    )
                }
            }
            else -> {
                audioContainer.visibility = View.GONE
                tvMessage.visibility = View.VISIBLE
                tvMessage.text = msg.text
            }
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
