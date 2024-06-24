package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.util.UIState

interface UserRepository {
    fun updateUserInfo(user: User, result: (UIState<String>) -> Unit)

    fun getUsers(result: (UIState<List<User>>) -> Unit)

    fun deleteUser(user: User, result: (UIState<String>) -> Unit)

    fun existeUserName(userName: String, email: String, result: (UIState<Boolean>) -> Unit)

    fun storeSessioon(id: String, result: (User?) -> Unit)

    suspend fun uploadSocialPicture(imageUri: Uri, result: (UIState<String>) -> Unit)

}