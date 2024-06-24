package com.marcosdeuna.unilink.data.repository

import android.content.SharedPreferences
import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.util.FirebaseStorageConstant
import com.marcosdeuna.unilink.util.FirestoreCollection
import com.marcosdeuna.unilink.util.SharedPreferencesKey
import com.marcosdeuna.unilink.util.UIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepositoryImpl (val database: FirebaseFirestore,
                          val appPreferences: SharedPreferences,
                          val storageReference: StorageReference,
                          val gson: Gson
): UserRepository {
    override fun updateUserInfo(user: User, result: (UIState<String>) -> Unit) {
        val document = database.collection(FirestoreCollection.USER).document(user.id)
        document
            .set(user)
            .addOnCompleteListener {
                storeSessioon(
                    id = user.id
                ){
                    if(it != null){
                        result.invoke(UIState.Success("Usuario actualizado correctamente"))
                    }else{
                        result.invoke(UIState.Error("Error"))
                    }
                }
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override fun getUsers(result: (UIState<List<User>>) -> Unit) {
        database.collection(FirestoreCollection.USER)
            .get()
            .addOnSuccessListener {
                val users = it.toObjects(User::class.java)
                result.invoke(UIState.Success(users))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override fun deleteUser(user: User, result: (UIState<String>) -> Unit) {
        val document = database.collection(FirestoreCollection.USER).document(user.id)
        document
            .delete()
            .addOnCompleteListener {
                result.invoke(UIState.Success("Usuario eliminado correctamente"))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override fun existeUserName(userName: String, email: String, result: (UIState<Boolean>) -> Unit) {
        database.collection(FirestoreCollection.USER)
            .whereEqualTo("userName", userName)
            .get()
            .addOnSuccessListener {
                if (it.size() == 1 && it.documents[0].toObject(User::class.java)?.email == email){
                    result.invoke(UIState.Success(false))
                }else{
                    result.invoke(UIState.Success(true))
                }
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override fun storeSessioon(id: String, result: (User?) -> Unit){
        val document = database.collection(FirestoreCollection.USER).document(id)
        document.get()
            .addOnCompleteListener(){
                if (it.isSuccessful){
                    val user = it.result?.toObject(User::class.java)
                    appPreferences.edit().putString(SharedPreferencesKey.USER_SESSION, gson.toJson(user)).apply()
                    result.invoke(user)
                }
            }
            .addOnFailureListener(){
                result.invoke(null)
            }
    }

    override suspend fun uploadSocialPicture(imageUri: Uri, result: (UIState<String>) -> Unit) {
        try{
            val uri: Uri = withContext(Dispatchers.IO){
                storageReference.child(FirebaseStorageConstant.SOCIAL_IMAGE).child(imageUri.lastPathSegment?:"{${System.currentTimeMillis()}}" )
                    .putFile(imageUri)
                    .await()
                    .storage
                    .downloadUrl
                    .await()
            }
            result.invoke(UIState.Success(uri.toString()))
        }catch (e: FirebaseException){
            result.invoke(UIState.Error(e.localizedMessage?:"Error"))
        }catch (e: Exception){
            result.invoke(UIState.Error(e.localizedMessage?:"Error"))
        }
    }

}