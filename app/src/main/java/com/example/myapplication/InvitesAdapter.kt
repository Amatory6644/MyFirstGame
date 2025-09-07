package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InvitesAdapter(
    private val onAcceptInvite: (GroupInviteRequest) -> Unit,
    private val onDeclineInvite: (GroupInviteRequest) -> Unit
) : RecyclerView.Adapter<InvitesAdapter.InviteViewHolder>() {

    private var invites: List<GroupInviteRequest> = emptyList()

    fun updateInvites(newInvites: List<GroupInviteRequest>) {
        invites = newInvites
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite, parent, false)
        return InviteViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        holder.bind(invites[position])
    }

    override fun getItemCount(): Int = invites.size

    inner class InviteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSender: TextView = itemView.findViewById(R.id.tv_sender)
        private val tvGroupId: TextView = itemView.findViewById(R.id.tv_group_id)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_message)
        private val btnAccept: Button = itemView.findViewById(R.id.btn_accept)
        private val btnDecline: Button = itemView.findViewById(R.id.btn_decline)

        fun bind(invite: GroupInviteRequest) {
            tvSender.text = "От: ${invite.sender}"
            tvGroupId.text = "Группа: ${invite.groupId}"
            tvMessage.text = invite.message

            btnAccept.setOnClickListener {
                onAcceptInvite(invite)
            }

            btnDecline.setOnClickListener {
                onDeclineInvite(invite)
            }
        }
    }
}
