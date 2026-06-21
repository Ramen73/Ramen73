package com.ramen73.ramenchat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ramen73.ramenchat.R
import com.ramen73.ramenchat.databinding.ItemUserSearchBinding
import com.ramen73.ramenchat.model.User

class UserSearchAdapter(
    private val onClick: (User) -> Unit
) : ListAdapter<User, UserSearchAdapter.VH>(DIFF) {

    inner class VH(val binding: ItemUserSearchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvName.text = user.displayName
            binding.tvEmail.text = user.email
            if (user.photoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(user.photoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_default_avatar)
                    .into(binding.ivAvatar)
            } else {
                binding.ivAvatar.setImageResource(R.drawable.ic_default_avatar)
            }
            binding.root.setOnClickListener { onClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemUserSearchBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(old: User, new: User) = old.uid == new.uid
            override fun areContentsTheSame(old: User, new: User) = old == new
        }
    }
}
