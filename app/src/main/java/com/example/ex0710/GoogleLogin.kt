package com.example.ex0710

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.ex0710.ui.theme.Ex0710Theme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GoogleLogin : ComponentActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setContent {
            Ex0710Theme {
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var showFaceLogin by remember { mutableStateOf(false) }

                    if (showFaceLogin) {
                        FaceMainScreen(navController = navController)
                    } else {
                        GoogleLoginScreen(
                            googleSignInClient = googleSignInClient,
                            navController = navController,
                            onFaceLoginClick = { showFaceLogin = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleLoginScreen(
    googleSignInClient: GoogleSignInClient,
    navController: NavController,
    onFaceLoginClick: () -> Unit
) {
    val context = LocalContext.current
    var user by remember { mutableStateOf<GoogleSignInAccount?>(GoogleSignIn.getLastSignedInAccount(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            user = task.getResult(ApiException::class.java)
            Toast.makeText(context, "로그인 성공: ${user?.email}", Toast.LENGTH_SHORT).show()

            // 여기에 sendUserId 호출 추가
            user?.id?.let { userId ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val apiService = ApiService.create()
                        val response = apiService.sendUserId(userId)
                        if (response.isSuccessful) {
                            Log.d("GoogleSignIn", "User ID sent successfully")
                        } else {
                            Log.e("GoogleSignIn", "Failed to send User ID: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("GoogleSignIn", "Error sending User ID", e)
                    }
                }
            }

            navController.navigate("main") {
                popUpTo("googleLogin") { inclusive = true }
            }
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(context, "로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        user = GoogleSignIn.getLastSignedInAccount(context)
        if (user != null) {
            navController.navigate("main") {
                popUpTo("googleLogin") { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Image(
            painter = painterResource(id = R.drawable.title),
            contentDescription = "힐리 타이틀",
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            contentScale = ContentScale.Fit
        )

        Image(
            painter = painterResource(id = R.drawable.test),
            contentDescription = "힐리 이미지",
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentScale = ContentScale.Fit
        )

        if (user == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        launcher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Google 로그인")
                }

                Button(
                    onClick = onFaceLoginClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("다른 방법으로 로그인", color = Color.White)
                }
            }
        } else {
            Text("로그인된 이메일: ${user?.email}")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    user = null
                    Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("로그아웃")
            }
        }
    }
}