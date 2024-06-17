package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.util.UIState

interface AuthRepository {
    fun login(email: String, password: String, result: (UIState<String>) -> Unit)
    fun register(email: String, password: String, user: User, result: (UIState<String>) -> Unit)

    fun updateUserInfo(user: User, result: (UIState<String>) -> Unit)
    fun logout(result: (UIState<String>) -> Unit)

    fun forgotPassword(email: String, result: (UIState<String>) -> Unit)

    fun storeSessioon(id: String, result: (User?) -> Unit)

    fun getUserSession(result: (User?) -> Unit)

    fun getUserById(id: String, result: (User?) -> Unit)

    suspend fun uploadProfilePicture(imageUri: Uri, result: (UIState<String>) -> Unit)
}