package com.marcosdeuna.unilink.data.repository

import com.marcosdeuna.unilink.data.model.Token
import com.marcosdeuna.unilink.util.UIState

interface TokenRepository {
    fun getTokens(result: (UIState<List<Token>>) -> Unit)
    fun saveToken(token: Token, result: (UIState<String>) -> Unit)
}