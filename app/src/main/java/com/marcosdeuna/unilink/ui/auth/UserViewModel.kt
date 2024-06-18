package com.marcosdeuna.unilink.ui.auth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.data.repository.UserRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor( val repository: UserRepository): ViewModel() {
    private val _updateUserInfo = MutableLiveData<UIState<String>>()
    val updateUserInfo: MutableLiveData<UIState<String>>
            get() = _updateUserInfo

    fun updateUserInfo(user: User){
        _updateUserInfo.postValue(UIState.Loading)
        repository.updateUserInfo(user){
            _updateUserInfo.postValue(it)
        }
    }
}