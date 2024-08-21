package com.example.ex0710

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable {
                navController.navigate("googleLogin") {
                    popUpTo("splash") { inclusive = true }
                }
            },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.rb),
                contentDescription = "Splash Screen",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}