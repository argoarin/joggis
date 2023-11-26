package com.example.joggis

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.joggis.UI.ChatPage
import com.example.joggis.UI.navController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object UI {

    //  Variabelen blir brukt til å bytte fra en rute til en annen
    lateinit var navController: NavController

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun AppNavigator() {

        val navControl = rememberNavController()
        val registrationActivity = remember { RegistrationActivity() }
        NavHost(navController = navControl, startDestination = "startup") {
            composable("startup") { StartupScreen(navControl) }
            composable("loginRegister") { LoginRegisterScreen(navControl) }
            composable("home") { HomePage(navControl) }

            composable("privateChat/{username}") { backStackEntry ->
                PrivateChatPage(
                    backStackEntry.arguments?.getString("username") ?: ""
                )
            }
// Her har jeg satt navController til skjermnavigator, og fjernet også sider som vises på én skjerm.
            navController = navControl

        }
    }

    @Composable
    fun StartupScreen(navController: NavController) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Joggis", style = MaterialTheme.typography.h1)

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = { navController.navigate("loginRegister") }) {
                    Text(text = "Log in/Register")
                }
            }
        }
    }

    //fjernet navController siden global navController blir brukt
    @Composable
    fun ChatPage() {
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

            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Back button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                Text(text = "Chat", fontSize = 26.sp)

            }

            // Check if user has a username
            if (currentUserUsername.isNullOrEmpty()) {
                Text(
                    "Go to Profile and create a username to use chat",
                    Modifier.align(Alignment.CenterHorizontally)
                )
                Button(onClick = { navController.navigate("profile") }) {
                    Text("Go to My Profile")
                }
            } else {
                if (loading) {
                    CircularProgressIndicator()
                } else {
                    LazyColumn {
                        items(usernames) { username ->
                            val displayUsername =
                                if (username == currentUserUsername) "$username (Myself)" else username
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
    fun PrivateChatPage(toUsername: String) {
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
                Log.d("PrivateChatPage", "UID for $toUsername: $uid")
                loading = false
            }, onFailure = { exception ->
                Log.e("PrivateChatPage", "Failed to get UID: ${exception.message}")
                loading = false
            })
        }

        // Load messages
        LaunchedEffect(toUid.value) {
            if (toUid.value.isNotBlank()) {
                chatManager.loadMessages(
                    currentUserUid,
                    toUid.value,
                    onSuccess = { loadedMessages ->
                        messages = loadedMessages
                        loading = false
                    },
                    onFailure = { /* Handle failure */ })
            }
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
                                date = Date()
                            ),
                            onSuccess = {
                                inputText = ""
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
        val backgroundColor =
            if (isCurrentUser) Color(0xFFADD8E6) else Color(0xFFD3D3D3)
        val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
        val senderUsername =
            if (isCurrentUser) "You" else toUsername

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
//    fjerne navController ettersom at global nvacontroller bli brukt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
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
    var skillLevelError by remember { mutableStateOf(false) }

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

            TextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") })
            TextField(
                value = profileImageUrl,
                onValueChange = { profileImageUrl = it },
                label = { Text("Profile Image URL") })
            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") })
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
                    skillLevel = newValue.toIntOrNull() ?: skillLevel
                    skillLevelError = skillLevel !in 1..10
                },
                label = { Text("Skill Level") },
                isError = skillLevelError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (skillLevelError) {
                Text("Please choose a level between 1 and 10", color = Color.Red)
            }

            Row {
                Text("Private Profile")
                Switch(checked = privateProfile, onCheckedChange = { privateProfile = it })
            }
            Button(
                onClick = {
                    val updatedUser = User(
                        currentUserUid, username, profileImageUrl,
                        description, birthdate, privateProfile, skillLevel
                    )
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(registrationActivity: RegistrationActivity) {
    val activities = remember { mutableStateListOf<Activity>() }
    val user = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val density = LocalDensity.current

    var description by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        if (user != null) {
            FirebaseFirestore.getInstance()
                .collection("activity")
                .whereEqualTo("uid", user.uid)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        Toast.makeText(
                            context,
                            "Error getting activities: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
//                        clearing the list before adding new items
                        activities.clear()
                        for (document in value!!) {
                            val activity = document.toObject(Activity::class.java)
                            activities.add(activity)
                        }
                    }
                }

        } else {
            Toast.makeText(context, "Not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Activities") },
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
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
                Button(onClick = {
                    // Assuming the duration input is a valid integer
                    val durationInt = duration.toIntOrNull()
                    if (description.isNotBlank() && durationInt != null) {
                        registrationActivity.registerActivity(description, durationInt)
                        description = ""
                        duration = ""
                    } else {
                        // Handle the error state
                    }
                }) {
                    Text("Register Activity")
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),

                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    val itemCount = activities.size
                    val itemHeightEstimate = 48.dp // Estimate the height of each item
                    val totalItemHeightPx =
                        with(density) { (itemHeightEstimate * itemCount).toPx() }
                    val screenHeightPx = with(density) { screenHeightDp.toPx() }


                    items(activities) { activity ->
                        ActivityItem(activity)
                    }
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
            Text(
                text = "Description: ${activity.description}",
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "Duration: ${activity.duration} minutes",
                style = MaterialTheme.typography.subtitle1
            )
            Text(
                text = "Date: ${DateFormat.getDateTimeInstance().format(activity.date)}",
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(goalManager: GoalManager) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val goalsState = remember { mutableStateOf<List<Goal>>(emptyList()) }

    LaunchedEffect(userId) {
        goalManager.getGoalsFlow(userId).collect { goalsList ->
            goalsState.value = goalsList
        }
    }

    val goals = goalsState.value
    val context = LocalContext.current
    var newGoalText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Goals") },
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
            verticalArrangement = Arrangement.Top
        ) {
            LazyColumn {
                items(goals) { goal ->
                    GoalItem(goal, goalManager)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = newGoalText,
                onValueChange = { newGoalText = it },
                label = { Text("Enter New Goal") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (newGoalText.isNotBlank()) {
                    goalManager.addGoal(
                        userId = userId,
                        goalText = newGoalText,
                        onSuccess = {
                            Toast.makeText(context, "Goal added!", Toast.LENGTH_SHORT).show()
                            newGoalText = ""
                        },
                        onFailure = { exception ->
                            Toast.makeText(
                                context,
                                "Error: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }) {
                Text("Add New Goal")
            }
        }
    }
}

@Composable
fun GoalItem(goal: Goal, goalManager: GoalManager) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { /* Handle click on a goal item if needed */ }
    ) {
        Row {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = "Goal Text: ${goal.goalText}",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(text = "Goal ID: ${goal.goalId}", style = MaterialTheme.typography.subtitle1)
                Text(text = "User ID: ${goal.userId}", style = MaterialTheme.typography.subtitle1)
            }
//            delete goal button
            IconButton(
                onClick = {


//                    calling removeGoal function from GoalManager class
                    goalManager.removeGoal(goal.goalId)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    modifier = Modifier.size(128.dp),
                    contentDescription = "Delete"
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(navController: NavController) {
    // den kontrolleren er for å bytte ruter i main screen
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            AppBar(navController)
        },
        bottomBar = { NavigationBar(navController) }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = "activities") {
//            alle rutene i main screen
            composable("chat") { ChatPage() }
            composable("activities") { ActivitiesScreen(RegistrationActivity()) }
            composable("goal") { GoalsScreen(GoalManager()) }
            composable("profile") { ProfileScreen() }
            composable("search") { SearchScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(navController: NavController) {
    TopAppBar(
        title = {
            Text(
                text = "Joggis",
                fontSize = 30.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            )

        },
        actions = {
//            search tab at top
            IconButton(onClick = {
                navController.navigate("search")
            }) {
                Icon(imageVector = Icons.Filled.Search, contentDescription = "Search")
            }
        }
    )
}

// navigation bar
@Composable
fun NavigationBar(navController: NavController) {
    BottomAppBar() {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavigationItem(
                icon = Icons.Default.Build,
                label = "Activities",
                onClick = { navController.navigate("activities") }
            )
            NavigationItem(
                icon = Icons.Default.Email,
                label = "Chat",
                onClick = { navController.navigate("chat") }
            )
            NavigationItem(
                icon = Icons.Default.Done,
                label = "Goals",
                onClick = { navController.navigate("goal") }
            )
            NavigationItem(
                icon = Icons.Default.Person,
                label = "Profile",
                onClick = { navController.navigate("profile") }
            )
        }
    }
}

@Composable
fun NavigationItem(route: String, icon: ImageVector, navController: NavController) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { navController.navigate(route) }) {
            Icon(icon, contentDescription = route)
        }
        Text(text = route, color = Color.Black) // Adjust color or style as needed
    }
}


@Composable
fun NavigationItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(text = label, color = Color.Black)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    var usernameQuery by remember { mutableStateOf("") }
    var skillLevelQuery by remember { mutableStateOf("") }
    var ageQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<UserProfile>()) }

    LaunchedEffect(usernameQuery, skillLevelQuery, ageQuery) {
        searchResults = FirebaseRepository.searchPartner(
            username = if (usernameQuery.isNotBlank()) usernameQuery else null,
            skillLevel = if (skillLevelQuery.isNotBlank()) skillLevelQuery.toLong() else null,
            ageOrBirthYear = if (ageQuery.isNotBlank()) ageQuery.toInt() else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
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
                .padding(padding)
        ) {
            TextField(
                value = usernameQuery,
                onValueChange = { usernameQuery = it },
                placeholder = { Text("Search by username") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add a separate TextField for age
            TextField(
                value = ageQuery,
                onValueChange = { ageQuery = it },
                placeholder = { Text("Search by age") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = skillLevelQuery,
                onValueChange = { skillLevelQuery = it },
                placeholder = { Text("Search by skill level") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { /** Do something here **/ }) {
                Text("Search")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (searchResults.isNotEmpty()) {
                LazyColumn {
                    items(searchResults) { userProfile ->
                        SearchResultItem(
                            userProfile = userProfile,
                            onProfileClick = {
                                // Handle profile selection
                            },
                            onChatClick = {
                                // Navigate to private chat with the selected username
                                navController.navigate("privateChat/${userProfile.username}")
                            }
                        )
                    }
                }
            } else {
                Text(text = "No results found.")
            }
        }
    }
}

@Composable
fun SearchResultItem(
    userProfile: UserProfile,
    onProfileClick: () -> Unit,
    onChatClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onProfileClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Username: ${userProfile.username}",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = "Age: ${userProfile.calculateAge()}",
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = "Skill Level: ${userProfile.skillLevel}",
                    style = MaterialTheme.typography.subtitle1
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(onClick = onChatClick) {
                Text("Chat")
            }
        }
    }
}


