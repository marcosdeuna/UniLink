package com.marcosdeuna.unilink.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.marcosdeuna.unilink.data.model.Events
import com.marcosdeuna.unilink.util.UIState

class EventsRepositoryImpl (val database: FirebaseFirestore) : EventsRepository {
    override fun getEvents(result: (UIState<List<Events>>) -> Unit) {
        database.collection("events")
            .orderBy("time", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val events = documents.map { it.toObject(Events::class.java) }
                result(UIState.Success(events))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun addEvent(event: Events, result: (UIState<String>) -> Unit) {
        val document = database.collection("events").document()
        event.id = document.id
        document.set(event)
            .addOnSuccessListener {
                result(UIState.Success("Event added successfully"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun updateEvent(event: Events, result: (UIState<String>) -> Unit) {
        database.collection("events").document(event.id)
            .set(event)
            .addOnSuccessListener {
                result(UIState.Success("Event updated successfully"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun deleteEvent(event: Events, result: (UIState<String>) -> Unit) {
        database.collection("events").document(event.id)
            .delete()
            .addOnSuccessListener {
                result(UIState.Success("Event deleted successfully"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }


}