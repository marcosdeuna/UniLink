package com.marcosdeuna.unilink.data.repository

import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.util.FirestoreCollection
import com.marcosdeuna.unilink.util.SharedPreferencesKey
import com.marcosdeuna.unilink.util.UIState

class UserRepositoryImpl (val database: FirebaseFirestore,
                          val appPreferences: SharedPreferences,
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
                        result.invoke(UIState.Success("User updated successfully"))
                    }else{
                        result.invoke(UIState.Error("Error"))
                    }
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

}