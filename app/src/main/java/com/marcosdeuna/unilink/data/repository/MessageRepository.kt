package com.marcosdeuna.unilink.data.repository

import com.marcosdeuna.unilink.data.model.Message
import com.marcosdeuna.unilink.util.UIState

interface MessageRepository {

    fun sendMessage(message: Message, result: (UIState<String>) -> Unit)
    fun getMessages(myId: String, userId: String, result: (UIState<List<Message>>) -> Unit)
    fun deleteMessage(message: Message, result: (UIState<String>) -> Unit)

    fun updateMessage(message: Message, result: (UIState<String>) -> Unit)

}