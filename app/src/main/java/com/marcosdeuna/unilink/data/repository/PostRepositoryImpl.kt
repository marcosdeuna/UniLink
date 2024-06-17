package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.marcosdeuna.unilink.data.model.Post
import com.marcosdeuna.unilink.util.FirebaseStorageConstant
import com.marcosdeuna.unilink.util.FirestoreCollection
import com.marcosdeuna.unilink.util.UIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PostRepositoryImpl (val  database: FirebaseFirestore, val storageReference: StorageReference): PostRepository {
    override fun getPosts(result: (UIState<List<Post>>) -> Unit) {
        database.collection(FirestoreCollection.POST)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener {
                val posts = it.toObjects(Post::class.java)
                result.invoke(UIState.Success(posts))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override fun addPost(post: Post, result: (UIState<String>) -> Unit) {
        val document = database.collection(FirestoreCollection.POST).document()
        post.id = document.id
        document
            .set(post)
            .addOnSuccessListener {
                result.invoke(UIState.Success("Post creado correctamente"))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override fun updatePost(post: Post, result: (UIState<String>) -> Unit) {
        val document = database.collection(FirestoreCollection.POST).document(post.id)
        document
            .set(post)
            .addOnSuccessListener {
                result.invoke(UIState.Success("Post actualizado correctamente"))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }

    }

    override fun deletePost(post: Post, result: (UIState<String>) -> Unit) {
        val document = database.collection(FirestoreCollection.POST).document(post.id)
        document
            .delete()
            .addOnSuccessListener {
                result.invoke(UIState.Success("Post eliminado correctamente"))
            }
            .addOnFailureListener {
                result.invoke(UIState.Error(it.message.toString()))
            }
    }

    override suspend fun uploadPostPicture(imageUri: Uri, result: (UIState<String>) -> Unit) {
        try{
            val uri: Uri = withContext(Dispatchers.IO){
                storageReference.child(FirebaseStorageConstant.POST_IMAGE).child(imageUri.lastPathSegment?:"{${System.currentTimeMillis()}}" )
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