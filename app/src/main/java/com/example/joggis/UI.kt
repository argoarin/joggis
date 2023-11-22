package com.example.joggis

import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UI {

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppNavigator() {
        val navController = rememberNavController()
        val registrationActivity = remember {RegistrationActivity()}
        NavHost(navController = navController, startDestination = "startup") {
            composable("startup") { StartupScreen(navController) }
            composable("loginRegister") { LoginRegisterScreen(navController) }
            composable("home") { HomePage(navController) }
            composable("registerActivity") {
                RegisterActivityScreen(navController, registrationActivity)
            }
            composable("activities") { ActivitiesScreen(navController) }
            composable("profile") {
                ProfileScreen(navController)
            }
            composable("chat") {
                ChatPage(navController)
            }
            composable("privateChat/{username}") { backStackEntry ->
                PrivateChatPage(navController, backStackEntry.arguments?.getString("username") ?: "")
            }
            // Other destinations...
        }
    }
    @Composable
    fun StartupScreen(navController: NavController) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Joggis", style = MaterialTheme.typography.h3)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { navController.navigate("loginRegister") }) {
                    Text(text = "Log in/Register")
                }
            }
        }
    }

    @Composable
    fun ChatPage(navController: NavController) {
        val chatManager = remember { ChatManager() }
        val profileManager = remember { ProfileManager() }
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // State for usernames and current user's username
        var usernames by remember { mutableStateOf<List<String>>(emptyList()) }
        var currentUserUsername by remember { mutableStateOf<String?>(null) }
        var loading by remember { mutableStateOf(true) }
        var selectedUsername by remember { mutableStateOf<String?>(null) }

        // Load usernames and current user's username
        LaunchedEffect(Unit) {
            chatManager.getAllUsernames(onSuccess = { loadedUsernames ->
                usernames = loadedUsernames
                loading = false
            }, onFailure = { /* Handle failure */ })

            profileManager.getProfile(currentUserUid, onSuccess = { userProfile ->
                currentUserUsername = userProfile?.username
            }, onFailure = { /* Handle failure */ })
        }

        // UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Back button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            // Check if user has a username
            if (currentUserUsername.isNullOrEmpty()) {
                Text("Go to Profile and create a username to use chat", Modifier.align(Alignment.CenterHorizontally))
                Button(onClick = { navController.navigate("profile") }) {
                    Text("Go to My Profile")
                }
            } else {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    LazyColumn {
                        items(usernames) { username ->
                            val displayUsername = if (username == currentUserUsername) "$username (Myself)" else username
                            TextButton(onClick = { selectedUsername = username }) {
                                Text(displayUsername)
                            }
                        }
                    }

                    Button(
                        onClick = { navController.navigate("privateChat/${selectedUsername}") },
                        enabled = selectedUsername != null
                    ) {
                        Text("Chat with ${selectedUsername ?: "Select a user"}")
                    }
                }
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun PrivateChatPage(navController: NavController, toUsername: String) {
        val chatManager = remember { ChatManager() }
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        var messages by remember { mutableStateOf<List<Chat>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var inputText by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }

        // Get UID by username
        val toUid = remember { mutableStateOf("") }
        LaunchedEffect(toUsername) {
            chatManager.getUidByUsername(toUsername, onSuccess = { uid ->
                toUid.value = uid
                loading = false
            }, onFailure = { /* Handle failure */ })
        }

        // Load messages
        LaunchedEffect(toUid.value) {
            chatManager.loadMessages(currentUserUid, toUid.value, onSuccess = { loadedMessages ->
                messages = loadedMessages
                loading = false
            }, onFailure = { /* Handle failure */ })
        }

        // UI Elements
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            }

            if (loading) {
                CircularProgressIndicator()
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(messages) { chat ->
                        val isCurrentUser = chat.fromUid == currentUserUid
                        MessageBubble(chat, isCurrentUser, toUsername)
                    }
                }
            }

            // Message input and send button
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter your message") },
                modifier = Modifier.fillMaxWidth()
            )

            if (error.isNotEmpty()) {
                Text(text = error, color = MaterialTheme.colors.error)
            }

            Button(
                onClick = {
                    if (inputText.isNotEmpty()) {
                        chatManager.sendMessage(
                            Chat(
                                fromUid = currentUserUid,
                                toUid = toUid.value,
                                text = inputText,
                                date = Date() // Use current date
                            ),
                            onSuccess = {
                                inputText = "" // Clear the text field
                                error = ""
                            },
                            onFailure = {
                                error = "Failed to send message."
                            }
                        )
                    } else {
                        error = "Can't send an empty message."
                    }
                },
                enabled = inputText.isNotEmpty()
            ) {
                Text("Send")
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun MessageBubble(chat: Chat, isCurrentUser: Boolean, toUsername: String) {
        val backgroundColor = if (isCurrentUser) Color(0xFFADD8E6) else Color(0xFFD3D3D3) // Light blue for sent, light grey for received
        val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
        val senderUsername = if (isCurrentUser) "You" else toUsername // Replace "You" with your actual user's username if available

        Box(
            contentAlignment = alignment,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
                modifier = Modifier
                    .background(backgroundColor, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(chat.text, textAlign = TextAlign.Center)
                Text(
                    text = "$senderUsername sent at ${formatDate(chat.date)}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    private fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return formatter.format(date)
    }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginRegisterScreen(navController: NavController) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        // Function to handle the login button click
        fun handleLogin() {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                isLoading = true
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            message = "Successfully logged in as $email!"
                            navController.navigate("home") {
                                popUpTo("loginRegister") { inclusive = true }
                            }
                        } else {
                            message = task.exception?.localizedMessage ?: "Login failed"
                        }
                    }
            } else {
                message = "Email and password cannot be empty."
            }
        }

        // Function to handle the register button click
        fun handleRegister() {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                isLoading = true
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        isLoading = false
                        if (task.isSuccessful) {
                            message = "Registered $email! You can now log in."
                        } else {
                            message = task.exception?.localizedMessage ?: "Registration failed"
                        }
                    }
            } else {
                message = "Email and password cannot be empty."
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (message.isNotEmpty()) {
                        Text(text = message, color = MaterialTheme.colors.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { handleLogin() }, modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text("Login")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { handleRegister() }, modifier = Modifier.fillMaxWidth(0.8f)) {
                        Text("Register")
                    }
                }
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val profileManager = remember { ProfileManager() }
    val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    var user by remember { mutableStateOf<User?>(null) }
    var loading by remember { mutableStateOf(true) }
    var saveMessage by remember { mutableStateOf("") }
    var birthdateError by remember { mutableStateOf(false) }

    // Initialize fields
    var username by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var birthdate by remember { mutableStateOf("") }
    var privateProfile by remember { mutableStateOf(false) }
    var skillLevel by remember { mutableStateOf(1) }

    // Birthdate format validation
    val birthdateRegex = Regex("\\d{2}\\.\\d{2}\\.\\d{4}") // Pattern for "DD.MM.YYYY"

    // Load profile data
    LaunchedEffect(currentUserUid) {
        profileManager.getProfile(currentUserUid, onSuccess = { userProfile ->
            user = userProfile
            if (userProfile != null) {
                username = userProfile.username
                profileImageUrl = userProfile.profileImageUrl
                description = userProfile.description
                birthdate = userProfile.birthdate
                privateProfile = userProfile.privateProfile
                skillLevel = userProfile.skillLevel
            }
            loading = false
        }, onFailure = { /* Handle failure */ })
    }

    if (loading) {
        CircularProgressIndicator()
    } else {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }

            TextField(value = username, onValueChange = { username = it }, label = { Text("Username") })
            TextField(value = profileImageUrl, onValueChange = { profileImageUrl = it }, label = { Text("Profile Image URL") })
            TextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            TextField(
                value = birthdate,
                onValueChange = {
                    birthdate = it
                    birthdateError = !birthdateRegex.matches(it)
                },
                label = { Text("Birthdate (DD.MM.YYYY)") },
                isError = birthdateError
            )
            if (birthdateError) {
                Text("Please use DD.MM.YYYY for birthdate", color = Color.Red)
            }
            TextField(
                value = skillLevel.toString(),
                onValueChange = { newValue ->
                    skillLevel = newValue.toIntOrNull()?.coerceIn(1, 10) ?: skillLevel // Ensures value is between 1 and 10
                },
                label = { Text("Skill Level") }
            )
            Row {
                Text("Private Profile")
                Switch(checked = privateProfile, onCheckedChange = { privateProfile = it })
            }
            Button(
                onClick = {
                    val updatedUser = User(currentUserUid, username, profileImageUrl,
                        description, birthdate, privateProfile, skillLevel)
                    profileManager.saveProfile(updatedUser, onSuccess = {
                        saveMessage = "Profile successfully saved."
                    }, onFailure = { /* Handle failure */ })
                },
                enabled = !birthdateError
            ) {
                Text("Save")
            }
            if (saveMessage.isNotEmpty()) {
                Text(saveMessage)
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterActivityScreen(navController: NavController, registrationActivity: RegistrationActivity) {
    // State variables to hold the input from the user
    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Activity") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") }
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (minutes)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Assuming the duration input is a valid integer
                val durationInt = duration.toIntOrNull()
                if (description.isNotBlank() && durationInt != null) {
                    registrationActivity.registerActivity(description, durationInt)
                    // Optionally navigate back to the home page or show a success message
                    navController.popBackStack()
                } else {
                    // Handle the error state
                }
            }) {
                Text("Register Activity")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(navController: NavController) {
    val activities = remember { mutableStateListOf<Activity>() }
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("activity")
                .whereEqualTo("uid", user.uid)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val activity = document.toObject(Activity::class.java)
                        activities.add(activity)
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Error getting activities: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Activities") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                val itemCount = activities.size
                val itemHeightEstimate = 48.dp // Estimate the height of each item
                val totalItemHeightPx = with(density) { (itemHeightEstimate * itemCount).toPx() }
                val screenHeightPx = with(density) { screenHeightDp.toPx() }
                val topPaddingPx = maxOf((screenHeightPx - totalItemHeightPx) / 2, 0f)

                item {
                    Spacer(modifier = Modifier.height(with(density) { topPaddingPx.toDp() }))
                }

                items(activities) { activity ->
                    ActivityItem(activity)
                }
            }
        }
    }
}

@Composable
fun ActivityItem(activity: Activity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(text = "Description: ${activity.description}", style = MaterialTheme.typography.subtitle1)
            Text(text = "Duration: ${activity.duration} minutes", style = MaterialTheme.typography.subtitle1)
            Text(text = "Date: ${DateFormat.getDateTimeInstance().format(activity.date)}", style = MaterialTheme.typography.subtitle1)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Joggis") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { navController.navigate("profile") },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "My Profile", style = MaterialTheme.typography.h6)
                }
            }

            // Chat Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { navController.navigate("chat") },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "Chat", style = MaterialTheme.typography.h6)
                }
            }

            /// Register Activity Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { navController.navigate("registerActivity") },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "Register Activity", style = MaterialTheme.typography.h6)
                }
            }

            // Activities Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { navController.navigate("activities") },
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(text = "Activities", style = MaterialTheme.typography.h6)
                }
            }
        }
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        Surface {
            UI.AppNavigator()
        }
    }
}
