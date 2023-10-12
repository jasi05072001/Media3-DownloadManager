package com.example.myapplicationm3.screens

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigator() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.MainScreen.route
        ){
        composable(Screens.MainScreen.route){
            MainScreen(navController)
        }
        composable(Screens.HlsPlayerScreen.route){
            HlsScreen(navController)
        }
        composable(Screens.SubtitleVideoScreen.route){
            SubTitlesScreen(navController)
        }
        composable(Screens.OfflinePlayerScreen.route){
            OfflinePlayerScreen()
        }
    }
}