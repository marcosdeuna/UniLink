package com.marcosdeuna.unilink.data.model

import java.util.Date

data class Events(
    var id: String = "",
    val userid: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    var first_reminder: String? = "",
    var second_reminder: String? = "",
    var time: Date? = null
)