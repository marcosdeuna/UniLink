package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageReference
import com.marcosdeuna.unilink.data.model.Markers
import com.marcosdeuna.unilink.util.FirebaseStorageConstant
import com.marcosdeuna.unilink.util.UIState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MarkersRepositoryImpl (val database: FirebaseFirestore, val storageReference: StorageReference): MarkersRepository {
    override fun getMarkers(result: (UIState<List<Markers>>) -> Unit) {
        database.collection("markers").get().addOnSuccessListener { querySnapshot ->
            val markers = querySnapshot.toObjects(Markers::class.java)
            result(UIState.Success(markers))
        }.addOnFailureListener { exception ->
            result(UIState.Error(exception.message.toString()))
        }
    }

    override fun getMarkerById(id: String, result: (UIState<Markers>) -> Unit) {
        database.collection("markers").document(id).get().addOnSuccessListener { documentSnapshot ->
            val marker = documentSnapshot.toObject(Markers::class.java)
            if (marker != null) {
                result(UIState.Success(marker))
            } else {
                result(UIState.Error("Marker not found"))
            }
        }.addOnFailureListener { exception ->
            result(UIState.Error(exception.message.toString()))
        }
    }

    override fun addMarker(marker: Markers, result: (UIState<String>) -> Unit) {
        val document = database.collection("markers").document()
        marker.id = document.id
        document.set(marker).addOnSuccessListener {
            result(UIState.Success("Marker added"))
        }.addOnFailureListener { exception ->
            result(UIState.Error(exception.message.toString()))
        }
    }

    override fun updateMarker(marker: Markers, result: (UIState<String>) -> Unit) {
        database.collection("markers").document(marker.id).set(marker).addOnSuccessListener {
            result(UIState.Success("Marker updated"))
        }.addOnFailureListener { exception ->
            result(UIState.Error(exception.message.toString()))
        }
    }

    override fun deleteMarker(id: String, result: (UIState<String>) -> Unit) {
        database.collection("markers").document(id).delete().addOnSuccessListener {
            result(UIState.Success("Marker deleted"))
        }.addOnFailureListener { exception ->
            result(UIState.Error(exception.message.toString()))
        }
    }

    override suspend fun uploadMarkerPicture(imageUri: Uri, result: (UIState<String>) -> Unit) {
        try{
            val uri: Uri = withContext(Dispatchers.IO){
                storageReference.child(FirebaseStorageConstant.MARKER_IMAGE).child(imageUri.lastPathSegment?:"{${System.currentTimeMillis()}}" )
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