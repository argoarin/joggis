package com.example.joggis

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class RegistrationActivity {

    private val db = FirebaseFirestore.getInstance()

    fun registerActivity(
        description: String,
        duration: Int,
        date: Calendar = Calendar.getInstance(),
        onSuccess: (String) -> Unit = {}, // onSuccess callback with the ID of the new document
        onFailure: (Exception) -> Unit = {} // onFailure callback with the exception
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val activity = hashMapOf(
                "uid" to user.uid,
                "description" to description,
                "duration" to duration,
                "date" to date.time
            )

            db.collection("activity")
                .add(activity)
                .addOnSuccessListener { documentReference ->
                    onSuccess(documentReference.id) // Invoke onSuccess callback with the document ID
                }
                .addOnFailureListener { e ->
                    onFailure(e) // Invoke onFailure callback with the exception
                }
        } else {
            onFailure(Exception("No authenticated user found.")) // Invoke onFailure callback
        }
    }
}
