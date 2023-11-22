package com.example.joggis

import com.google.firebase.firestore.FirebaseFirestore

class ChatManager {

    private val db = FirebaseFirestore.getInstance()

    fun sendMessage(chat: Chat, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("chat")
            .add(chat)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun loadMessages(currentUserUid: String, toUid: String, onSuccess: (List<Chat>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("chat")
            .whereIn("fromUid", listOf(currentUserUid, toUid))
            .whereIn("toUid", listOf(currentUserUid, toUid))
            .orderBy("date")
            .get()
            .addOnSuccessListener { documents ->
                val chats = documents.mapNotNull { it.toObject(Chat::class.java) }
                val filteredChats = chats.filter { (it.fromUid == currentUserUid && it.toUid == toUid) || (it.fromUid == toUid && it.toUid == currentUserUid) }
                onSuccess(filteredChats)
            }
            .addOnFailureListener { onFailure(it) }
    }


    fun getAllUsernames(onSuccess: (List<String>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("profile")
            .get()
            .addOnSuccessListener { result ->
                val usernames = result.mapNotNull { it.getString("username") }.filterNot { it.isBlank() }
                onSuccess(usernames)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun getUidByUsername(username: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("profile")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                val uid = result.documents.firstOrNull()?.getString("uid")
                if (uid != null) {
                    onSuccess(uid)
                } else {
                    onFailure(Exception("User not found"))
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}