package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.screens.HomeScreen
import com.example.myapplication.ui.screens.WorkoutScreen
import com.example.myapplication.ui.theme.WorkoutTimerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkoutTimerTheme {
                WorkoutTimerApp()
            }
        }
    }
}

@Composable
fun WorkoutTimerApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onStartWorkout = {
                    navController.navigate("workout")
                }
            )
        }
        composable("workout") {
            WorkoutScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
