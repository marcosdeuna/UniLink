package com.marcosdeuna.unilink.ui.discoverPlaces

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.marcosdeuna.unilink.data.model.Markers
import com.marcosdeuna.unilink.data.model.User
import com.marcosdeuna.unilink.data.repository.MarkersRepository
import com.marcosdeuna.unilink.util.UIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarkersViewModel @Inject constructor( val repository: MarkersRepository) : ViewModel() {

    private val _createMarker = MutableLiveData<UIState<String>>()

    val createMarker: MutableLiveData<UIState<String>>
        get() = _createMarker

    fun createMarker(marker: Markers){
        _createMarker.postValue(UIState.Loading)
        repository.addMarker(marker){
            _createMarker.postValue(it)
        }
    }

    private val _markers = MutableLiveData<UIState<List<Markers>>>()

    val markers: MutableLiveData<UIState<List<Markers>>>
        get() = _markers

    fun getMarkers() {
        _markers.value = UIState.Loading
        repository.getMarkers { result ->
            _markers.value = result
        }
    }

    private val _updateMarker = MutableLiveData<UIState<String>>()

    val updateMarker: MutableLiveData<UIState<String>>
        get() = _updateMarker

    fun updateMarker(marker: Markers){
        _updateMarker.postValue(UIState.Loading)
        repository.updateMarker(marker){
            _updateMarker.postValue(it)
        }
    }

    private val _deleteMarker = MutableLiveData<UIState<String>>()

    val deleteMarker: MutableLiveData<UIState<String>>
        get() = _deleteMarker

    fun deleteMarker(marker: Markers){
        _deleteMarker.postValue(UIState.Loading)
        repository.deleteMarker(marker.id){
            _deleteMarker.postValue(it)
        }
    }

    private var markersListener: ListenerRegistration? = null
    fun observeMarkers(){
        markersListener = FirebaseFirestore.getInstance()
            .collection("markers")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _markers.postValue(e.message?.let { UIState.Error(it) })
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val markerlist = snapshot.documents.mapNotNull { it.toObject(Markers::class.java) }
                    _markers.postValue(UIState.Success(markerlist))
                } else {
                    _markers.postValue(UIState.Empty)
                }
            }
    }

    fun onUploadImage(imagesUri: List<Uri>, result: (UIState<List<String>>) -> Unit){
        result.invoke(UIState.Loading)
        var imagesUrl = arrayListOf<String>()
        for (imageUri in imagesUri) {
            viewModelScope.launch {
                repository.uploadMarkerPicture(imageUri) { uploadResult ->
                    when (uploadResult) {
                        is UIState.Success -> {
                            imagesUrl.add(uploadResult.data)
                            if (imagesUrl.size == imagesUri.size) {
                                result.invoke(UIState.Success(imagesUrl))
                            }
                        }
                        is UIState.Error -> {
                            result.invoke(UIState.Error(uploadResult.exception))
                        }

                        UIState.Empty -> TODO()
                        UIState.Loading -> TODO()
                    }
                }
            }
        }
    }
}