package com.example.joggis

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.util.Log

data class UserProfile(
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



    constructor() : this("", 0, "")
}
object FirebaseRepository {
    suspend fun searchPartner(
        username: String? = null,
        skillLevel: Long? = null,
        ageOrBirthYear: Int? = null,
        birthdate: String? = null
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

        return querySnapshot.toObjects(UserProfile::class.java)
    }}