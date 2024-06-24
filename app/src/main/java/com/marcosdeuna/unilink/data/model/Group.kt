package com.marcosdeuna.unilink.data.model

data class Group (
    var id: String = "",
    val admin: String = "",
    val name: String = "",
    val description: String = "",
    val members: List<String> = emptyList(),
    val images: List<String> = emptyList()
)