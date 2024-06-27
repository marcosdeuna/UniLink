package com.marcosdeuna.unilink.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Review(
    var id: String = "",
    val userId: String = "",
    val markerId: String = "",
    val valoration: Double = 0.0,
    val comment: String = "",
    @ServerTimestamp
    val timestamp: Date? = null
)