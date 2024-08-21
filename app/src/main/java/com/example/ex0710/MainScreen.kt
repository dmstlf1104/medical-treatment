package com.example.ex0710

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import androidx.compose.ui.platform.LocalContext

@Composable
fun MainScreen(
    navController: NavController,
    googleSignInClient: GoogleSignInClient, // GoogleSignInClient 전달
    onSettingsClick: () -> Unit,
    onChatClick: () -> Unit,
    onMapClick: () -> Unit,
    onScanClick: () -> Unit,
    onLoginClick: () -> Unit
) {
    val fontSize by remember { AppState.fontSize }
    val context = LocalContext.current
    val account = GoogleSignIn.getLastSignedInAccount(context)
    var isLoggedIn by remember { mutableStateOf(account != null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        TopBar(
            onSettingsClick = {
                navController.navigate("settings")
            },
            onLoginClick = onLoginClick
        )
        Spacer(modifier = Modifier.height(1.dp))

        CardSection(
            title = "검사지 촬영",
            description = "병원 검사결과를 촬영해 올리시면 쉽게 설명해드립니다",
            fontSize = fontSize,
            backgroundColor = Color(0xFFF4D2D0),
            onClick = onScanClick,
            modifier = Modifier.padding(horizontal = 3.dp),
            imageResId = R.drawable.camera
        )

        Spacer(modifier = Modifier.height(16.dp))
        CardSection(
            title = "힐리와 대화",
            description = "궁금한 게 있다면 힐리에게 물어보세요!",
            fontSize = fontSize,
            backgroundColor = Color(0xFFBBD3EC),
            onClick = onChatClick,
            modifier = Modifier.padding(horizontal = 3.dp),
            imageResId = R.drawable.say
        )

        Spacer(modifier = Modifier.height(16.dp))
        CardSection(
            title = "주변 병원 찾기",
            description = "가까운 병원을 지도에서 확인하세요",
            fontSize = fontSize,
            backgroundColor = Color(0xFFC9ECF2),
            onClick = onMapClick,
            modifier = Modifier.padding(horizontal = 3.dp),
            imageResId = R.drawable.hospital
        )
    }
}

@Composable
fun TopBar(onSettingsClick: () -> Unit, onLoginClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.title),
            contentDescription = "title",
            modifier = Modifier
                .size(100.dp)
                .padding(end = 16.dp)
        )
        Row {

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "settings", modifier = Modifier.size(36.dp))
            }
        }
    }
}

@Composable
fun CardSection(
    title: String,
    description: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    onClick: () -> Unit,
    imageResId: Int? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            imageResId?.let {
                Image(
                    painter = painterResource(id = it),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .padding(end = 8.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize * 1.5, fontWeight = FontWeight.Bold),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = fontSize,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = fontSize * 1.5
                )
            }
        }
    }
}