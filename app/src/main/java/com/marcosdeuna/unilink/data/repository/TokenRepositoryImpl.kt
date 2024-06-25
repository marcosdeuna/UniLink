package com.marcosdeuna.unilink.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.marcosdeuna.unilink.data.model.Token
import com.marcosdeuna.unilink.util.UIState

class TokenRepositoryImpl (val database: FirebaseFirestore): TokenRepository {
    override fun getTokens(result: (UIState<List<Token>>) -> Unit) {
        val document = database.collection("tokens")
        document.get()
            .addOnSuccessListener { documents ->
                val tokens = documents.toObjects(Token::class.java)
                result(UIState.Success(tokens))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }

    }

    override fun saveToken(token: Token, result: (UIState<String>) -> Unit) {
        val document = database.collection("tokens").document(token.userId)
        document.set(token)
            .addOnSuccessListener {
                result(UIState.Success("Token guardado"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }

    }

}