package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.util.UIState

interface PostRepository {
    fun getPosts(result: (UIState<List<Post>>) -> Unit)
    fun addPost(post: Post, result: (UIState<String>) -> Unit)
    fun updatePost(post: Post, result: (UIState<String>) -> Unit)

    fun deletePost(post: Post, result: (UIState<String>) -> Unit)

    suspend fun uploadPostPicture(imageUri: Uri, result: (UIState<String>) -> Unit)
}