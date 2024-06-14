package com.marcosdeuna.unilink.data.model

data class User (
    var id : String = "",
    val firstName : String = "",
    val lastName : String = "",
    val userName : String = "",
    val email : String = "",
    val password : String = "",
    val profilePicture : String = "",
)