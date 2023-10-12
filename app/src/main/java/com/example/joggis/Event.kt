package app.src.main.java.com.example.joggis

data class Event(
    val eventId: String,
    val eventName: String,
    val eventDate: Long,
    //val eventLocation: Geolocation,
    val participants: List<User>,
)