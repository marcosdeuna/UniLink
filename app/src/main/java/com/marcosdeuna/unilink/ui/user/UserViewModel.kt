package com.marcosdeuna.unilink.ui.user

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.data.repository.UserRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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

    private val _deleteUser = MutableLiveData<UIState<String>>()
    val deleteUser: MutableLiveData<UIState<String>>
        get() = _deleteUser

    fun deleteUser(user: User){
        _deleteUser.postValue(UIState.Loading)
        repository.deleteUser(user){
            _deleteUser.postValue(it)
        }
    }

    private val _users = MutableLiveData<UIState<List<User>>>()
    val users: MutableLiveData<UIState<List<User>>>
        get() = _users

    fun getUsers(){
        _users.postValue(UIState.Loading)
        repository.getUsers {
            _users.postValue(it)
        }
    }

    fun existeUserName(userName: String, email: String, result: (UIState<Boolean>) -> Unit){
        repository.existeUserName(userName, email){
            result.invoke(it)
        }
    }

    fun onUploadSocialImage(imagesUri: List<Uri>, result: (UIState<List<String>>) -> Unit){
        result.invoke(UIState.Loading)
        var imagesUrl = arrayListOf<String>()
        for (imageUri in imagesUri) {
            viewModelScope.launch {
                repository.uploadSocialPicture(imageUri) { uploadResult ->
                    when (uploadResult) {
                        is UIState.Success -> {
                            imagesUrl.add(uploadResult.data)
                            if (imagesUrl.size == imagesUri.size) {
                                result.invoke(UIState.Success(imagesUrl))
                            }
                        }

                        is UIState.Error -> {
                            result.invoke(UIState.Error(uploadResult.exception))
                        }

                        UIState.Empty -> TODO()
                        UIState.Loading -> TODO()
                    }
                }
            }
        }
    }
}