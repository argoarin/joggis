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
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
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
                    onSuccess(documentReference.id)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } else {
            onFailure(Exception("No authenticated user found."))
        }
    }
}
