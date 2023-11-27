package com.example.joggis

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`


class ChatManagerTest {

    private val mockDb = mock(FirebaseFirestore::class.java)
    private val chatManager = ChatManager(mockDb)

    @Test
    fun testSendMessage() {
        val chat = Chat()
        val mockCollection = mock(CollectionReference::class.java)
        val mockTask = mock(Task::class.java) as Task<DocumentReference>

        `when`(mockDb.collection("chat")).thenReturn(mockCollection)
        `when`(mockCollection.add(chat)).thenReturn(mockTask)

        `when`(mockTask.addOnSuccessListener(any())).thenReturn(mockTask)
        `when`(mockTask.addOnFailureListener(any())).thenReturn(mockTask)


        chatManager.sendMessage(chat, {}, {})


        verify(mockCollection).add(chat)

        verify(mockTask).addOnSuccessListener(any())
        verify(mockTask).addOnFailureListener(any())
    }
}

