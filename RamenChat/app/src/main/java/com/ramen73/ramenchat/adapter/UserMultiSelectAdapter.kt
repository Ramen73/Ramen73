package com.ramen73.ramenchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ramen73.ramenchat.R
import com.ramen73.ramenchat.databinding.ItemUserMultiSelectBinding
import com.ramen73.ramenchat.model.User

class UserMultiSelectAdapter(
    private val selected: MutableSet<User>,
    private val onToggle: (User, Boolean) -> Unit
) : ListAdapter<User, UserMultiSelectAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemUserMultiSelectBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvName.text = user.displayName
            binding.tvEmail.text = user.email
            binding.checkbox.setOnCheckedChangeListener(null)
            binding.checkbox.isChecked = selected.any { it.uid == user.uid }
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }
            val toggle: () -> Unit = {
                val newState = !binding.checkbox.isChecked
                binding.checkbox.isChecked = newState
                onToggle(user, newState)
            }
            binding.root.setOnClickListener { toggle() }
            binding.checkbox.setOnClickListener {
                onToggle(user, binding.checkbox.isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemUserMultiSelectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.uid == new.uid
            override fun areContentsTheSame(old: User, new: User) = old == new
        }
    }
}
