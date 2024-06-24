package com.marcosdeuna.unilink.data.model

import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Message(
    var id: String = "",
    var receiverId: String = "",
    val senderId: String = "",
    val message: String = "",
    val isseen: Boolean = false,
    @ServerTimestamp
    val timestamp: Date? = null
) : Parcelable