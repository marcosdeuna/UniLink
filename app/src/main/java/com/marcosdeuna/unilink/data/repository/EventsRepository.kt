package com.marcosdeuna.unilink.data.repository

import com.marcosdeuna.unilink.data.model.Events
import com.marcosdeuna.unilink.util.UIState

interface EventsRepository {
    fun getEvents(result: (UIState<List<Events>>) -> Unit)
    fun addEvent(event: Events, result: (UIState<String>) -> Unit)
    fun updateEvent(event: Events, result: (UIState<String>) -> Unit)
    fun deleteEvent(event: Events, result: (UIState<String>) -> Unit)

}