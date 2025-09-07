package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupsAdapter(
    private val onGroupClick: (GroupInfo) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {
    
    private var groups: List<GroupInfo> = emptyList()
    
    fun updateGroups(newGroups: List<GroupInfo>) {
        groups = newGroups
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return GroupViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(groups[position])
    }
    
    override fun getItemCount(): Int = groups.size
    
    inner class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val text1: TextView = itemView.findViewById(android.R.id.text1)
        private val text2: TextView = itemView.findViewById(android.R.id.text2)
        
        fun bind(group: GroupInfo) {
            text1.text = "Группа: ${group.groupId.take(8)}..."
            text2.text = "Игроков: ${group.players.size}/${group.maxPlayers} | Статус: ${group.status}"
            
            itemView.setOnClickListener {
                onGroupClick(group)
            }
        }
    }
}
