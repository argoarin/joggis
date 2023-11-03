package com.example.joggis

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

object UI {

    @Composable
    fun AppNavigator() {
        val navController = rememberNavController()
        val registrationActivity = remember {RegistrationActivity()}
        /*
        UI currently has Startup Screen with Login/Register button
        Login/Register button navigates to Login/Register page
        Login on Login/Register page leads to Home page
        */
        NavHost(navController = navController, startDestination = "startup") {
            composable("startup") { StartupScreen(navController) }
            composable("loginRegister") { LoginRegisterScreen(navController) }
            composable("home") { HomePage(navController) }
            composable("registerActivity") {
                // Pass the instance of RegistrationActivity to the RegisterActivityScreen composable
                RegisterActivityScreen(navController, registrationActivity)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                .padding(innerPadding), // Apply padding to avoid overlapping with the TopAppBar
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
                    .clickable { /* Handle click, navigate to activities list */ },
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
