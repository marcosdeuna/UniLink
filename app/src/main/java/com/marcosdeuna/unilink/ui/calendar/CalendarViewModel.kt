package com.marcosdeuna.unilink.ui.calendar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marcosdeuna.unilink.data.model.Events
import com.marcosdeuna.unilink.data.model.Markers
import com.marcosdeuna.unilink.data.repository.EventsRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor(val repository: EventsRepository): ViewModel() {

    private val _events = MutableLiveData<UIState<List<Events>>>()

    val events: MutableLiveData<UIState<List<Events>>>
        get() = _events

    fun getEvents(){
        _events.postValue(UIState.Loading)
        repository.getEvents{
            _events.postValue(it)
        }
    }

    private val _addEvent = MutableLiveData<UIState<String>>()

    val addEvent: LiveData<UIState<String>>
        get() = _addEvent

    fun addEvent(event: Events){
        _addEvent.postValue(UIState.Loading)
        repository.addEvent(event){
            _addEvent.postValue(it)
        }
    }

    private val _deleteEvent = MutableLiveData<UIState<String>>()

    val deleteEvent: LiveData<UIState<String>>
        get() = _deleteEvent

    fun deleteEvent(event: Events){
        _deleteEvent.postValue(UIState.Loading)
        repository.deleteEvent(event){
            _deleteEvent.postValue(it)
        }
    }

    private val _updateEvent = MutableLiveData<UIState<String>>()

    val updateEvent: LiveData<UIState<String>>
        get() = _updateEvent

    fun updateEvent(event: Events){
        _updateEvent.postValue(UIState.Loading)
        repository.updateEvent(event){
            _updateEvent.postValue(it)
        }
    }




}
