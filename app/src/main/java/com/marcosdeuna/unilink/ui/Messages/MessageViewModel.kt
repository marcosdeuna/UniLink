package com.marcosdeuna.unilink.ui.Messages

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marcosdeuna.unilink.data.model.Message
import com.marcosdeuna.unilink.data.repository.MessageRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MessageViewModel @Inject constructor(
    private val repository: MessageRepository
) : ViewModel() {

    private val _messages = MutableLiveData<UIState<List<Message>>>()
    val messages: LiveData<UIState<List<Message>>>
        get() = _messages

    fun getMessages(myId: String, userId: String) {
        _messages.value = UIState.Loading
        repository.getMessages(myId, userId) { result ->
            _messages.value = result
        }
    }

    private val _sendMessage = MutableLiveData<UIState<String>>()
    val sendMessage: LiveData<UIState<String>>
        get() = _sendMessage

    fun sendMessage(message: Message) {
        _sendMessage.value = UIState.Loading
        repository.sendMessage(message) { result ->
            _sendMessage.value = result
        }
    }

    private val _updateMessage = MutableLiveData<UIState<String>>()
    val updateMessage: LiveData<UIState<String>>
        get() = _updateMessage

    fun updateMessage(message: Message) {
        _updateMessage.value = UIState.Loading
        repository.updateMessage(message) { result ->
            _updateMessage.value = result
        }
    }


}