package app.src.main.java.com.example.joggis

data class Session(
    val sessionId: String,
    val userId: String,
    val startTime: Long,
    //val location: Geolocation,
    val participants: List<User>,
)