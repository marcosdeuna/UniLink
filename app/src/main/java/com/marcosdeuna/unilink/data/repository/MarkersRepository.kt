package com.marcosdeuna.unilink.data.repository

import android.net.Uri
import com.marcosdeuna.unilink.data.model.Markers
import com.marcosdeuna.unilink.util.UIState

interface MarkersRepository {
    fun getMarkers(result: (UIState<List<Markers>>) -> Unit)

    fun getMarkerById(id: String, result: (UIState<Markers>) -> Unit)

    fun addMarker(marker: Markers, result: (UIState<String>) -> Unit)

    fun updateMarker(marker: Markers, result: (UIState<String>) -> Unit)

    fun deleteMarker(id: String, result: (UIState<String>) -> Unit)

    suspend fun uploadMarkerPicture(imageUri: Uri, result: (UIState<String>) -> Unit)

}