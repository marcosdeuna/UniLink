package com.marcosdeuna.unilink.ui.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.data.repository.PostRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostViewModel @Inject constructor(
    private val postRepository: PostRepository
) : ViewModel() {

    private val _posts = MutableLiveData<UIState<List<Post>>>()
    val posts: LiveData<UIState<List<Post>>>
        get() = _posts

    fun getPosts() {
        _posts.value = UIState.Loading
        postRepository.getPosts { result ->
            _posts.value = result
        }
    }

    private val _addPost = MutableLiveData<UIState<String>>()
    val addPost: LiveData<UIState<String>>
        get() = _addPost

    fun addPost(post: Post) {
        _addPost.value = UIState.Loading
        postRepository.addPost(post) { result ->
            _addPost.value = result
        }
    }

    private val _updatePost = MutableLiveData<UIState<String>>()

    val updatePost: LiveData<UIState<String>>
        get() = _updatePost

    fun updatePost(post: Post) {
        _updatePost.value = UIState.Loading
        postRepository.updatePost(post) { result ->
            _updatePost.value = result
        }
    }

    private val _deletePost = MutableLiveData<UIState<String>>()

    val deletePost: LiveData<UIState<String>>
        get() = _deletePost

    fun deletePost(post: Post) {
        _deletePost.value = UIState.Loading
        postRepository.deletePost(post) { result ->
            _deletePost.value = result
        }
    }

    fun onUploadImage(imagesUri: List<Uri>, result: (UIState<List<String>>) -> Unit){
        result.invoke(UIState.Loading)
        var imagesUrl = arrayListOf<String>()
        for (imageUri in imagesUri) {
            viewModelScope.launch {
                postRepository.uploadPostPicture(imageUri) { uploadResult ->
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