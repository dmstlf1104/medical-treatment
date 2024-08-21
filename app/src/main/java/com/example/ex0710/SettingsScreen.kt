package com.example.ex0710

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.google.android.gms.auth.api.signin.GoogleSignInClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    googleSignInClient: GoogleSignInClient,
    onBackPress: () -> Unit,
    onLogout: () -> Unit
) {
    var fontSize by remember { mutableStateOf(AppState.fontSize.value.value) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("설정") },
            navigationIcon = {
                IconButton(onClick = onBackPress) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("폰트 크기 조절", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("작게", modifier = Modifier.weight(1f))
                Slider(
                    value = fontSize,
                    onValueChange = {
                        fontSize = it
                        AppState.fontSize.value = it.sp
                    },
                    valueRange = 12f..24f,
                    modifier = Modifier.weight(8f)
                )
                Text("크게", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "이것은 폰트 크기 조절 예시입니다.",
                fontSize = fontSize.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 로그아웃 버튼
            Button(onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    onLogout() // 로그아웃 후 동작
                }
            }) {
                Text("로그아웃")
            }
        }
    }
}
