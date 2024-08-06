package nes.app.ui

import androidx.annotation.OptIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.media3.common.util.UnstableApi
import androidx.navigation.compose.rememberNavController

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun NesApp() {
    val navController = rememberNavController()
    NesNavController(navController = navController)
}
