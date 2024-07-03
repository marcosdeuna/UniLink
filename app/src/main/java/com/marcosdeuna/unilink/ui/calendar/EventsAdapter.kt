package com.marcosdeuna.unilink.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.marcosdeuna.unilink.data.model.Events
import com.marcosdeuna.unilink.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventsAdapter ( val onEditAction: (Events) -> Unit, val onDeleteAction: (Events) -> Unit) :
    ListAdapter<Events, EventsAdapter.EventsViewHolder>(EventsDiffUtil()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventsViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventsViewHolder(private val binding: ItemEventBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(event: Events){
            binding.apply {
                eventTitle.text = event.title
                eventDescription.text = event.description
                eventLocation.text = event.location

                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val eventTimestamp: Date? = event.time
                clockTextView.text = eventTimestamp?.let { timeFormat.format(it) }

                editButton.setOnClickListener {
                    onEditAction(event)
                }

                deleteButton.setOnClickListener {
                    onDeleteAction(event)
                }
            }
        }
    }

    class EventsDiffUtil: DiffUtil.ItemCallback<Events>(){
        override fun areItemsTheSame(oldItem: Events, newItem: Events): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Events, newItem: Events): Boolean {
            return oldItem == newItem
        }
    }
}