package com.marcosdeuna.unilink.ui.auth

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.data.repository.AuthRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor( val repository: AuthRepository) : ViewModel() {
    private val _register = MutableLiveData<UIState<String>>()
    val register: MutableLiveData<UIState<String>>
            get() = _register

    fun register(email: String, password: String, user: User){
        _register.postValue(UIState.Loading)
        repository.register(email, password, user){
            _register.postValue(it)
        }
    }

    private val _login = MutableLiveData<UIState<String>>()
    val login: MutableLiveData<UIState<String>>
            get() = _login

    fun login(email: String, password: String){
        _login.postValue(UIState.Loading)
        repository.login(email, password){
            _login.postValue(it)
        }
    }

    private val _forgotPassword = MutableLiveData<UIState<String>>()
    val forgotPassword: MutableLiveData<UIState<String>>
            get() = _forgotPassword


    fun forgotPassword(email: String){
        _forgotPassword.postValue(UIState.Loading)
        repository.forgotPassword(email){
            _forgotPassword.postValue(it)
        }
    }

    private val _logout = MutableLiveData<UIState<String>>()
    val logout: MutableLiveData<UIState<String>>
            get() = _logout

    fun logout(){
        _logout.postValue(UIState.Loading)
        repository.logout{
            _logout.postValue(it)
        }
    }

    fun getUserSession(result: (User?) -> Unit){
        repository.getUserSession(result)
    }

    fun onUploadProfilePicture(imageUri: Uri, result: (UIState<String>) -> Unit){
        result.invoke(UIState.Loading)
        viewModelScope.launch {
            repository.uploadProfilePicture(imageUri, result)
        }
    }

    fun getUserById(userId: String, result: (User?) -> Unit){
        repository.getUserById(userId, result)
    }
}