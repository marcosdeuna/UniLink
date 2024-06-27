package com.marcosdeuna.unilink.ui.user

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcosdeuna.unilink.data.model.Group
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.data.repository.GroupRepository
import com.marcosdeuna.unilink.data.repository.UserRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor( val repository: GroupRepository): ViewModel() {
    private val _createGroup = MutableLiveData<UIState<String>>()
    val createGroup: MutableLiveData<UIState<String>>
        get() = _createGroup

    fun createGroup(group: Group){
        _createGroup.postValue(UIState.Loading)
        repository.addGroup(group){
            _createGroup.postValue(it)
        }
    }

    private val _groups = MutableLiveData<UIState<List<Group>>>()
    val groups: MutableLiveData<UIState<List<Group>>>
        get() = _groups

    fun getGroups() {
        _groups.value = UIState.Loading
        repository.getGroups { result ->
            _groups.value = result
        }
    }

    private val _deleteGroup = MutableLiveData<UIState<String>>()
    val deleteGroup: LiveData<UIState<String>>
        get() = _deleteGroup

    fun deleteGroup(group: Group){
        _deleteGroup.postValue(UIState.Loading)
        repository.deleteGroup(group){
            _deleteGroup.postValue(it)
        }
    }

    private val _updateGroup = MutableLiveData<UIState<String>>()

    val updateGroup: LiveData<UIState<String>>
        get() = _updateGroup

    fun updateGroup(group: Group){

        _updateGroup.postValue(UIState.Loading)
        repository.updateGroup(group){
            _updateGroup.postValue(it)
        }
    }

    fun onUploadImage(imagesUri: List<Uri>, result: (UIState<List<String>>) -> Unit){
        result.invoke(UIState.Loading)
        var imagesUrl = arrayListOf<String>()
        for (imageUri in imagesUri) {
            viewModelScope.launch {
                repository.uploadGroupPicture(imageUri) { uploadResult ->
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