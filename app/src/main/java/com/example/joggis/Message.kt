package app.src.main.java.com.example.joggis

data class Message(
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val messageText: String,
    val timestamp: Long
)