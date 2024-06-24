package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.marcosdeuna.unilink.data.model.Group
import com.marcosdeuna.unilink.util.UIState

interface GroupRepository {
    fun getGroups(result: (UIState<List<Group>>) -> Unit)
    fun addGroup(group: Group, result: (UIState<String>) -> Unit)
    fun updateGroup(group: Group, result: (UIState<String>) -> Unit)

    fun deleteGroup(group: Group, result: (UIState<String>) -> Unit)

    suspend fun uploadGroupPicture(imageUri: Uri, result: (UIState<String>) -> Unit)
}