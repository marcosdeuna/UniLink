package com.marcosdeuna.unilink.data.repository

import android.content.SharedPreferences
import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
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

class AuthRepositoryImpl(
    val database: FirebaseFirestore,
    val storageReference: StorageReference,
    val auth: FirebaseAuth,
    val appPreferences: SharedPreferences,
    val gson: Gson
): AuthRepository {
    override fun login(email: String, password: String, result: (UIState<String>) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(){
                if (it.isSuccessful){
                    storeSessioon(
                        id = it.result?.user?.uid ?: ""
                    ){
                        if(it != null){
                            result.invoke(UIState.Success("Inicio de sesión exitoso"))
                        }else{
                            result.invoke(UIState.Error("Error al iniciar sesión"))
                        }
                    }
                }
            }
            .addOnFailureListener(){
                result.invoke(UIState.Error(it.localizedMessage?:"No existe usuario con ese email"))
            }
    }

    override fun register(email: String, password: String, user: User, result: (UIState<String>) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(){
                if (it.isSuccessful){
                    user.id = it.result?.user?.uid ?: ""
                    updateUserInfo(user){state ->
                        when(state){
                            is UIState.Success -> {
                                storeSessioon(
                                    id = it.result?.user?.uid ?: ""
                                ){
                                    if(it != null){
                                        result.invoke(UIState.Success("Registro exitoso"))
                                    }else{
                                        result.invoke(UIState.Error("Error"))
                                    }
                                }
                            }
                            is UIState.Error -> result.invoke(UIState.Error(state.exception))
                            UIState.Empty -> TODO()
                            UIState.Loading -> TODO()
                        }
                    } //update user info (name, email, password, etc.
                }else{
                    try {
                        throw it.exception ?: java.lang.Exception("Autenticación fallida")
                    }catch (e: FirebaseAuthWeakPasswordException){
                        result.invoke(UIState.Error("Contraseña débil, debe tener al menos 6 caracteres"))
                    }catch (e: FirebaseAuthInvalidCredentialsException){
                        result.invoke(UIState.Error("Email inválido"))
                    }catch (e: FirebaseAuthUserCollisionException){
                        result.invoke(UIState.Error("Email ya registrado"))
                    }catch (e: Exception){
                        result.invoke(UIState.Error("Error"))
                    }
                }
            }
            .addOnFailureListener(){
                result.invoke(UIState.Error(it.localizedMessage?:"Error"))
            }

    }

    override fun updateUserInfo(user: User, result: (UIState<String>) -> Unit) {
        val document = database.collection(FirestoreCollection.USER).document(user.id)
        document.set(user)
            .addOnSuccessListener {
                result.invoke(UIState.Success("Usuario actualizado correctamente"))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.localizedMessage?:"Error"))
            }
    }

    override fun logout(result: (UIState<String>) -> Unit) {
        //logout
        auth.signOut()
        appPreferences.edit().putString(SharedPreferencesKey.USER_SESSION, null).apply()
        result.invoke(UIState.Success("Cierre de sesión exitoso"))
    }

    override fun forgotPassword(email: String, result: (UIState<String>) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener(){
                if (it.isSuccessful){
                    result.invoke(UIState.Success("Correo enviado correctamente"))
                }
            }
            .addOnFailureListener(){
                result.invoke(UIState.Error(it.localizedMessage?:"Error"))
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

    override fun getUserSession(result: (User?) -> Unit) {
        val user = appPreferences.getString(SharedPreferencesKey.USER_SESSION, null)
        if (user != null){
            result.invoke(gson.fromJson(user, User::class.java))
        }else{
            result.invoke(null)
        }
    }

    override fun getUserById(id: String, result: (User?) -> Unit) {
        val document = database.collection(FirestoreCollection.USER).document(id)
        document.get()
            .addOnCompleteListener(){
                if (it.isSuccessful){
                    val user = it.result?.toObject(User::class.java)
                    result.invoke(user)
                }
            }
            .addOnFailureListener(){
                result.invoke(null)
            }
    }

    override fun deleteAccount(result: (UIState<String>) -> Unit) {
        val user = auth.currentUser
        user?.delete()
            ?.addOnCompleteListener(){
                if (it.isSuccessful){
                    appPreferences.edit().putString(SharedPreferencesKey.USER_SESSION, null).apply()
                    result.invoke(UIState.Success("Cuenta eliminada correctamente"))
                }
            }
            ?.addOnFailureListener(){
                result.invoke(UIState.Error(it.localizedMessage?:"Error"))
            }
    }

    override suspend fun uploadProfilePicture(imageUri: Uri, result: (UIState<String>) -> Unit) {
        try{
            val uri: Uri = withContext(Dispatchers.IO){
                storageReference.child(FirebaseStorageConstant.PROFILE_IMAGE).child(imageUri.lastPathSegment?:"{${System.currentTimeMillis()}}" )
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