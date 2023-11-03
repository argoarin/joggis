package com.example.joggis

import java.util.Date

data class Activity(
    val uid: String = "",
    val description: String = "",
    val duration: Int = 0,
    val date: Date = Date()
)
