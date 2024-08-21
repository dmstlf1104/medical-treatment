package com.example.ex0710

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ex0710.ui.theme.Ex0710Theme
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        tts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.ERROR) {
                tts.language = Locale.getDefault()
            }
        }

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        setContent {
            Ex0710Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val startDestination = if (GoogleSignIn.getLastSignedInAccount(this) != null) "main" else "splash"
                    AppNavigation(tts, startDestination, googleSignInClient)
                }
            }
        }

        // 여기서 getMedicalTerms API 호출 및 로그캣에 출력
        val apiService = ApiService.create()
        lifecycleScope.launch {
            try {
                val medicalTerms = apiService.getMedicalTerms()
                // Logcat에 medical terms 출력
                medicalTerms.forEach { term ->
                    Log.d("MedicalTerm", "KO: ${term.term_ko}, EN: ${term.term_en}, Explanation: ${term.explanation}")
                }
            } catch (e: Exception) {
                Log.e("API Error", "Failed to fetch medical terms: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                baseContext, it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}

sealed class Screen(val route: String) {
    object FaceMain : Screen("faceMain")
    object Test5 : Screen("test5")
    object Test6 : Screen("test6")
}

@Composable
fun AppNavigation(
    tts: TextToSpeech,
    startDestination: String,
    googleSignInClient: GoogleSignInClient
) {
    val navController = rememberNavController()
    val scanResultViewModel: ScanResultViewModel = viewModel()
    val sharedViewModel: SharedViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {
        composable("splash") {
            SplashScreen(navController)
        }
        composable("main") {
            MainScreen(
                navController = navController,
                googleSignInClient = googleSignInClient,
                onSettingsClick = { navController.navigate("settings") },
                onChatClick = { navController.navigate("chat") },
                onMapClick = { navController.navigate("map") },
                onScanClick = { navController.navigate("scanResult") },
                onLoginClick = {
                    navController.navigate("googleLogin") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                googleSignInClient = googleSignInClient,
                onBackPress = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("googleLogin") {
                        popUpTo("main") { inclusive = false }
                    }
                }
            )
        }
        composable("chat") {
            ChatScreen(
                onBackPress = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                tts = tts
            )
        }
        composable("map") {
            MapScreen(onBackPress = { navController.popBackStack() })
        }
        composable("scanResult") {
            ScanResultScreen(
                navController,
                scanResultViewModel,
                sharedViewModel,
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("detailedResult") {
            DetailedResultScreen(
                onBackPress = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") },
                sharedViewModel = sharedViewModel
            )
        }
        composable("faceMain") {
            FaceMainScreen(
                navController = navController
            )
        }
        composable("googleLogin") {
            GoogleLoginScreen(
                googleSignInClient = googleSignInClient,
                navController = navController,
                onFaceLoginClick = { navController.navigate("faceMain") }
            )
        }
        composable(Screen.FaceMain.route) { FaceMainScreen(navController) }
        composable(Screen.Test5.route) { FaceLoginScreen(navController) }
        composable(Screen.Test6.route) { FaceEnrollmentScreen(navController) }
    }
}
