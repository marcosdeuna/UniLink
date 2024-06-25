package com.marcosdeuna.unilink.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.data.model.Token
import com.marcosdeuna.unilink.data.repository.PostRepository
import com.marcosdeuna.unilink.data.repository.TokenRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TokenViewModel @Inject constructor(
    private val tokenRepository: TokenRepository
) : ViewModel() {

    private val _tokens = MutableLiveData<UIState<List<Token>>>()

    val tokens: LiveData<UIState<List<Token>>>
        get() = _tokens

    fun getTokens() {
        tokenRepository.getTokens{ result ->
            _tokens.value = result
        }
    }

    private val _saveToken = MutableLiveData<UIState<String>>()

    val saveToken: LiveData<UIState<String>>
        get() = _saveToken

    fun saveToken(token: Token) {
        tokenRepository.saveToken(token) { result ->
            _saveToken.value = result
        }
    }
}