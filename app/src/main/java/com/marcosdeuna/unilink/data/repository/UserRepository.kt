package com.marcosdeuna.unilink.data.repository

import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.util.UIState

interface UserRepository {
    fun updateUserInfo(user: User, result: (UIState<String>) -> Unit)

    fun storeSessioon(id: String, result: (User?) -> Unit)
}