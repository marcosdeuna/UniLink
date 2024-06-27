package com.marcosdeuna.unilink.data.model

data class Markers(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    val userId: String = "",
    var valoration: Double = 0.0,
    var images: List<String> = arrayListOf(),
)