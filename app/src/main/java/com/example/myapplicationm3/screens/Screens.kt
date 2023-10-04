package com.example.myapplicationm3.screens

sealed class Screens(val route:String){
    object MainScreen :Screens("home")
    object HlsPlayerScreen :Screens("hlsPlayer")
    object SubtitleVideoScreen :Screens("subtitles")
    object OfflinePlayerScreen  :Screens("offlinePlayer")

}
