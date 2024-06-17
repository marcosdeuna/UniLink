package com.marcosdeuna.unilink.data.model

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Post(
    var id: String = "",
    var userId: String = "",
    val title: String = "",
    val category: String = "",
    val description: String = "",
    var images: List<String> = arrayListOf(),
    @ServerTimestamp
    val timestamp: Date? = null
) : Parcelable