package com.marcosdeuna.unilink.data.model

data class User (
    var id : String = "",
    val firstName : String = "",
    val lastName : String = "",
    val userName : String = "",
    val career : String = "",
    val email : String = "",
    val password : String = "",
    val profilePicture : String = "",
    val socialPictures: List<String> = emptyList(),
    val description: String = "",
    val genre: String = "",
    val age: Int = 0,
    val status: String = "offline",
)