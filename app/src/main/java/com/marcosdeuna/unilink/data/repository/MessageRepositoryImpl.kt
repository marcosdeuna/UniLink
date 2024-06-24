package com.marcosdeuna.unilink.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.marcosdeuna.unilink.data.model.Message
import com.marcosdeuna.unilink.util.UIState

class MessageRepositoryImpl (val database: FirebaseFirestore): MessageRepository {
    override fun sendMessage(message: Message, result: (UIState<String>) -> Unit) {
        val document = database.collection("messages").document()
        message.id = document.id
        document.set(message)
            .addOnSuccessListener {
                result(UIState.Success("Mensaje Enviado Exitosamente!"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }


    override fun getMessages(myId: String, userId: String, result: (UIState<List<Message>>) -> Unit) {
        database.collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    result(UIState.Error(error.message.toString()))
                    return@addSnapshotListener
                }
                val messages = mutableListOf<Message>()
                if(myId.isNotEmpty() && userId.isNotEmpty()){
                    value?.documents?.forEach { document ->
                        val message = document.toObject(Message::class.java)
                        if (message != null) {
                            if ((message.senderId == myId && message.receiverId == userId) || (message.senderId == userId && message.receiverId == myId)) {
                                messages.add(message)
                            }
                        }
                    }
                }else{
                    value?.documents?.forEach { document ->
                        val message = document.toObject(Message::class.java)
                        if (message != null) {
                            messages.add(message)
                        }
                    }
                }
                result(UIState.Success(messages))
            }
    }

    override fun deleteMessage(message: Message, result: (UIState<String>) -> Unit) {
        database.collection("messages")
            .document(message.id)
            .delete()
            .addOnSuccessListener {
                result(UIState.Success("Mensaje Eliminado Exitosamente!"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun updateMessage(message: Message, result: (UIState<String>) -> Unit) {
        database.collection("messages")
            .document(message.id)
            .set(message)
            .addOnSuccessListener {
                result(UIState.Success("Mensaje Actualizado Exitosamente!"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }
}