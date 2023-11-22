package com.example.joggis

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

data class UserProfile(
    var userId: String = "",
    val username: String = "",
    val skillLevel: Long = 0,
    val birthdate: String = ""
) {
    val birthYear: Int
        get() {

            val birthdateParts = birthdate.split(".")
            return if (birthdateParts.size == 3) {
                birthdateParts[2].toIntOrNull() ?: 0
            } else {
                0
            }
        }

    fun calculateAge(): Int {
        val birthdateParts = birthdate.split(".")
        if (birthdateParts.size == 3) {
            val birthYear = birthdateParts[2].toIntOrNull() ?: 0
            val birthMonth = birthdateParts[1].toIntOrNull() ?: 0
            val birthDay = birthdateParts[0].toIntOrNull() ?: 0

            val today = Calendar.getInstance()
            val birthdateCalendar = Calendar.getInstance().apply {
                set(birthYear, birthMonth - 1, birthDay) // Note: Months are zero-based
            }

            var age = today.get(Calendar.YEAR) - birthdateCalendar.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthdateCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }


            Log.d("UserProfile", "Birthdate: $birthdate")
            Log.d("UserProfile", "Birthdate parts: $birthdateParts")
            Log.d("UserProfile", "Birthdate calendar: $birthdateCalendar")
            Log.d("UserProfile", "Today: $today")
            Log.d("UserProfile", "Calculated age: $age")

            return age
        } else {
            return 0
        }
    }



}
object FirebaseRepository {
    suspend fun searchPartner(
        username: String? = null,
        skillLevel: Long? = null,
        ageOrBirthYear: Int? = null
    ): List<UserProfile> {
        var query: Query = FirebaseFirestore.getInstance().collection("profile")


        if (username != null) {
            query = query.whereEqualTo("username", username)
        }

        if (ageOrBirthYear != null) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val birthYear = if (ageOrBirthYear > 0 && ageOrBirthYear < 150) {
                currentYear - ageOrBirthYear
            } else {

                ageOrBirthYear
            }


            val startDate = String.format(Locale.getDefault(), "01.01.%04d", birthYear)
            val endDate = String.format(Locale.getDefault(), "31.12.%04d", birthYear)

            query = query.whereGreaterThanOrEqualTo("birthdate", startDate)
                .whereLessThanOrEqualTo("birthdate", endDate)
        }

        if (skillLevel != null) {
            query = query.whereEqualTo("skillLevel", skillLevel)
        }

        val querySnapshot = query.get().await()

        return querySnapshot.documents.mapNotNull { document ->
            document.toObject(UserProfile::class.java)?.apply {
                userId = document.getString("uid") ?: "" // Retrieve uid from the document
            }
        }
    }}