package com.example.joggis

import com.google.firebase.firestore.FirebaseFirestore

class ProfileManager {

    private val db = FirebaseFirestore.getInstance()

    fun getProfile(
        uid: String,
        onSuccess: (User?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("profile")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    onSuccess(null)
                } else {
                    val user = documents.documents.first().toObject(User::class.java)
                    onSuccess(user)
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun saveProfile(
        user: User,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val profileData = hashMapOf(
            "uid" to user.uid,
            "username" to user.username,
            "profileImageUrl" to user.profileImageUrl,
            "description" to user.description,
            "birthdate" to user.birthdate,
            "privateProfile" to user.privateProfile,
            "skillLevel" to user.skillLevel
        )

        db.collection("profile")
            .whereEqualTo("uid", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    db.collection("profile")
                        .add(profileData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                } else {
                    documents.documents.first().reference
                        .set(profileData)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { e -> onFailure(e) }
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }
}
