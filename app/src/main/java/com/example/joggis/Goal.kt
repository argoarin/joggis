package com.example.joggis

data class Goal(
    val goalId: String,
    val userId: String,
    val goalText: String
) {
    constructor() : this("", "", "") {
    }
}