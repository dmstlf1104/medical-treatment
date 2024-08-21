package com.example.ex0710

import android.graphics.*
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun FaceEnrollmentScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val enrollmentResult = remember { mutableStateOf<EnrollmentResult?>(null) }
    val isProcessingImage = remember { AtomicBoolean(false) }
    val coroutineScope = rememberCoroutineScope()
    val enrollmentState = remember { mutableStateOf(EnrollmentState.GOOGLE_LOGIN) }
    var userId by remember { mutableStateOf<String?>(null) }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            userId = account.id
            enrollmentState.value = EnrollmentState.READY
            Toast.makeText(context, "Google 로그인 성공: ${account.email}", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "signInResult:failed code=" + e.statusCode)
            Toast.makeText(context, "로그인 실패: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    val webSocket = remember {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://192.168.247.41:8001/ws").build()
        client.newWebSocket(request, createWebSocketListener(enrollmentResult, enrollmentState))
    }

    DisposableEffect(Unit) {
        onDispose {
            webSocket.close(1000, "Screen destroyed")
        }
    }

    LaunchedEffect(enrollmentResult.value) {
        enrollmentResult.value?.let { result ->
            if (result.success && result.userId != null) {
                delay(1000) // 사용자가 결과를 볼 수 있도록 잠시 대기
                navController.navigate("faceMain")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (enrollmentState.value) {
            EnrollmentState.GOOGLE_LOGIN -> {
                Button(
                    onClick = { launcher.launch(googleSignInClient.signInIntent) },
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text("Google 로그인")
                }
            }
            EnrollmentState.READY -> {
                CameraPreview(
                    onImageCaptured = { image ->
                        processAndSendImage(image, webSocket, userId, isProcessingImage, coroutineScope)
                    }
                )
                Button(
                    onClick = { enrollmentState.value = EnrollmentState.CAPTURING },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text("얼굴 등록 시작")
                }
            }
            EnrollmentState.CAPTURING, EnrollmentState.PROCESSING -> {
                CameraPreview(
                    onImageCaptured = { image ->
                        processAndSendImage(image, webSocket, userId, isProcessingImage, coroutineScope)
                    }
                )
                Text(
                    if (enrollmentState.value == EnrollmentState.CAPTURING) "얼굴을 정면으로 바라봐주세요"
                    else "처리 중... 잠시만 기다려주세요",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }
            EnrollmentState.COMPLETED -> {
                Text(
                    "등록 완료!",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Green
                )
                Button(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text("메인으로")
                }
            }
            EnrollmentState.FAILED -> {
                Text(
                    "등록 실패. 다시 시도해주세요.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Red
                )
                Button(
                    onClick = { enrollmentState.value = EnrollmentState.READY },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text("재시도")
                }
            }
        }

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("뒤로")
        }
    }
}

@Composable
fun CameraPreview(
    onImageCaptured: (ImageProxy) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(executor, ImageAnalysis.Analyzer { imageProxy ->
                            onImageCaptured(imageProxy)
                        })
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (ex: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", ex)
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize(),
    )
}

private fun createWebSocketListener(
    enrollmentResult: MutableState<EnrollmentResult?>,
    enrollmentState: MutableState<EnrollmentState>
) = object : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WebSocket", "Received message: $text")
        when {
            text.isNotEmpty() -> {
                val userId = text.toIntOrNull()
                if (userId != null) {
                    enrollmentResult.value = EnrollmentResult(success = true, userId = userId)
                    enrollmentState.value = EnrollmentState.COMPLETED
                } else {
                    enrollmentState.value = EnrollmentState.FAILED
                }
            }
            else -> {
                enrollmentState.value = EnrollmentState.PROCESSING
            }
        }
    }
}

private fun processAndSendImage(
    image: ImageProxy,
    webSocket: WebSocket,
    userId: String?,
    isProcessingImage: AtomicBoolean,
    coroutineScope: CoroutineScope
) {
    if (isProcessingImage.getAndSet(true)) {
        image.close()
        return
    }

    coroutineScope.launch {
        try {
            val jpegBytes = image.convertImageProxyToJpeg()
            val message = "$userId,${jpegBytes.toByteString().base64()}"
            webSocket.send(message)
            Log.d("ImageProcessing", "Image sent with userId: $userId")
        } catch (e: Exception) {
            Log.e("ImageProcessing", "Error processing or sending image: ${e.message}", e)
        } finally {
            image.close()
            isProcessingImage.set(false)
        }
    }
}

private fun ImageProxy.convertImageProxyToJpeg(): ByteArray {
    val yBuffer = planes[0].buffer
    val uBuffer = planes[1].buffer
    val vBuffer = planes[2].buffer

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    yBuffer.get(nv21, 0, ySize)
    val pixelStride = planes[1].pixelStride
    val rowStride = planes[1].rowStride
    var pos = ySize
    for (row in 0 until height / 2) {
        for (col in 0 until width / 2) {
            val bufferIndex = col * pixelStride + row * rowStride
            nv21[pos++] = vBuffer[bufferIndex]
            nv21[pos++] = uBuffer[bufferIndex]
        }
    }

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
    val imageBytes = out.toByteArray()
    var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

    if (imageInfo.rotationDegrees != 0) {
        val matrix = Matrix()
        matrix.postRotate(imageInfo.rotationDegrees.toFloat())
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    out.reset()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    bitmap.recycle()
    return out.toByteArray()
}

enum class EnrollmentState {
    GOOGLE_LOGIN, READY, CAPTURING, PROCESSING, COMPLETED, FAILED
}

data class EnrollmentResult(
    val success: Boolean,
    val userId: Int?
)