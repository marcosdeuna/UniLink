package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.marcosdeuna.unilink.data.model.Group
import com.marcosdeuna.unilink.util.UIState

class GroupRepositoryImpl(val  database: FirebaseFirestore, val storageReference: StorageReference): GroupRepository {
    override fun getGroups(result: (UIState<List<Group>>) -> Unit) {
        database.collection("groups")
            .get()
            .addOnSuccessListener { resultData ->
                val groups = resultData.toObjects(Group::class.java)
                result(UIState.Success(groups))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun addGroup(group: Group, result: (UIState<String>) -> Unit) {
        val document = database.collection("groups").document()
        group.id = document.id
        document
            .set(group)
            .addOnSuccessListener {
                result(UIState.Success("Grupo Creado Exitosamente!"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun updateGroup(group: Group, result: (UIState<String>) -> Unit) {
        database.collection("groups")
            .document(group.id)
            .set(group)
            .addOnSuccessListener {
                result(UIState.Success("Grupo Actualizado Exitosamente!"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override fun deleteGroup(group: Group, result: (UIState<String>) -> Unit) {
        database.collection("groups")
            .document(group.id)
            .delete()
            .addOnSuccessListener {
                result(UIState.Success("Grupo Eliminado Exitosamente!"))
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }

    override suspend fun uploadGroupPicture(imageUri: Uri, result: (UIState<String>) -> Unit) {
        val storageRef = storageReference.child("group_pictures/${imageUri.lastPathSegment}")
        storageRef.putFile(imageUri)
            .addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener {
                    result(UIState.Success(it.toString()))
                }
            }
            .addOnFailureListener { exception ->
                result(UIState.Error(exception.message.toString()))
            }
    }
}