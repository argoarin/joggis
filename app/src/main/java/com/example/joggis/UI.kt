package com.example.joggis

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavController
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.joggis.RegistrationActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

object UI {

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
            // Register Activity Box
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



@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        Surface {
            UI.AppNavigator()
        }
    }
}
