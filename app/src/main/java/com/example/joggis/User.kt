package com.example.joggis

data class User(
    val uid: String = "",
    val username: String = "",
    val profileImageUrl: String = "",
    val description: String = "",
    val birthdate: String = "", // DD.MM.YYYY
    val privateProfile: Boolean = false,
    val skillLevel: Int = 1 // Default value as 1
)

