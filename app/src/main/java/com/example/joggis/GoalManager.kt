package com.example.joggis

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onCompletion
import java.util.UUID

class GoalManager {

    private val db = FirebaseFirestore.getInstance()

    private val _goalsFlow = MutableSharedFlow<List<Goal>>()
    val goalsFlow: Flow<List<Goal>> get() = _goalsFlow.asSharedFlow()

    fun getGoalsFlow(userId: String): StateFlow<List<Goal>> {
        val goals = MutableStateFlow(emptyList<Goal>())

        val listenerRegistration = db.collection("goal")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                } else {
                    val goalList = value?.toObjects(Goal::class.java) ?: emptyList()
                    goals.tryEmit(goalList)
                }
            }

        goals.onCompletion { listenerRegistration.remove() }

        return goals
    }

    fun addGoal(
        userId: String,
        goalText: String,
        onSuccess: (String) -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val goalId = UUID.randomUUID().toString()
            val goal = Goal(goalId, userId, goalText)

            db.collection("goal")
                .document(goalId)
                .set(goal)
                .addOnSuccessListener {
                    onSuccess(goalId)
                }
                .addOnFailureListener { e ->
                    onFailure(e)
                }
        } else {
            onFailure(Exception("No authenticated user found."))
        }
    }

    fun removeGoal(
        id:String
    ){
        db.collection("goal")
            .document(id)
            .delete()

    }
}