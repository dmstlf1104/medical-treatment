package com.example.ex0710

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun FaceMainScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { navController.navigate(Screen.Test5.route) },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("face login")
        }
        Button(
            onClick = { navController.navigate(Screen.Test6.route) },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("face enrollment")
        }
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Go Back")
        }
    }
}