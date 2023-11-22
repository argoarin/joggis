package com.example.joggis

import java.util.Date

data class Chat(
    val fromUid: String = "",
    val toUid: String = "",
    val text: String = "",
    val date: Date = Date()
    )