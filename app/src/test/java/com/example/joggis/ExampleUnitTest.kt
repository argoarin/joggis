package com.example.joggis

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test

import org.junit.Assert.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`


class ChatManagerTest {

    private val mockDb = mock(FirebaseFirestore::class.java)
    private val chatManager = ChatManager(mockDb)

    @Test
    fun testSendMessage() {
        // Arrange
        val chat = Chat(/* parameters */)
        val mockCollection = mock(CollectionReference::class.java)
        val mockTask = mock(Task::class.java) as Task<DocumentReference> // Mock the Task object

        `when`(mockDb.collection("chat")).thenReturn(mockCollection)
        `when`(mockCollection.add(chat)).thenReturn(mockTask)

        // Optionally, mock the behavior of addOnSuccessListener and addOnFailureListener
        `when`(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)
        `when`(mockTask.addOnFailureListener(any())).thenReturn(mockTask)

        // Act
        chatManager.sendMessage(chat, {}, { /* Handle failure */ })

        // Assert
        verify(mockCollection).add(chat)
        // Optionally, verify that addOnSuccessListener and addOnFailureListener are called
        verify(mockTask).addOnSuccessListener(any())
        verify(mockTask).addOnFailureListener(any())
    }
}

