package com.marcosdeuna.unilink.data.repository

import android.content.ContentValues
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
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
                result.invoke(UIState.Error("No existe usuario con ese email"))
            }
    }

    override fun register(email: String, password: String, user: User, result: (UIState<String>) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(){
                if (it.isSuccessful){
                    val authuser = auth.currentUser
                    authuser?.sendEmailVerification()
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
                            UIState.Empty -> {}
                            UIState.Loading -> {}
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
                result.invoke(UIState.Error("Error al registrar usuario"))
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
        getUserSession { user ->
            if (user != null){
                updateUserInfo(user.copy(status = "offline")){}
            }
        }
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
                    if (user != null) {
                        updateUserInfo(user.copy(status = "online")){}
                    }
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

    override fun updatePassword(currentPassword: String, newPassword: String, result: (UIState<String>) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                result.invoke(UIState.Success("Contraseña actualizada correctamente"))
                            } else {
                                result.invoke(UIState.Error("Error al actualizar la contraseña"))
                            }
                        }
                }
                .addOnFailureListener { e ->
                    result.invoke(UIState.Error("Error al reautenticar al usuario"))
                }
        } else {
            result.invoke(UIState.Error("No se pudo obtener el usuario actual"))
        }
    }


    override fun updateEmail(currentPassword: String, newEmail: String, result: (UIState<String>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // Obtener las credenciales para reautenticar al usuario
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            // Reautenticar al usuario con las credenciales actuales
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // Envío de correo de verificación al nuevo correo electrónico
                    auth.fetchSignInMethodsForEmail(newEmail).addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result?.signInMethods?.isEmpty() == true) {
                            user.updateEmail(newEmail)
                                .addOnCompleteListener { updateEmailTask ->
                                    if (updateEmailTask.isSuccessful) {
                                        user.sendEmailVerification()
                                            .addOnCompleteListener { sendVerificationTask ->
                                                if (sendVerificationTask.isSuccessful) {
                                                    result.invoke(UIState.Success("Correo electrónico actualizado exitosamente. Verifica tu nuevo correo electrónico."))
                                                } else {
                                                    result.invoke(UIState.Error(sendVerificationTask.exception?.localizedMessage ?: "Error al enviar correo de verificación"))
                                                }
                                            }
                                    } else {
                                        result.invoke(UIState.Error(updateEmailTask.exception?.localizedMessage ?: "Error al actualizar el correo electrónico"))
                                    }
                                }
                        } else {
                            result.invoke(UIState.Error("El correo electrónico ya está en uso o no es válido"))
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Error al reautenticar al usuario
                    result.invoke(UIState.Error(e.localizedMessage ?: "Error al reautenticar al usuario"))
                }
        } else {
            // Usuario no encontrado
            result.invoke(UIState.Error("Usuario no encontrado"))
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