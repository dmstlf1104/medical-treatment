package com.example.ex0710

import android.graphics.*
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString.Companion.toByteString
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

import androidx.compose.material.Button
import androidx.navigation.NavController

@Composable
fun FaceLoginScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recognitionResult = remember { mutableStateOf<Int?>(null) }
    val isProcessingImage = remember { AtomicBoolean(false) }
    val coroutineScope = rememberCoroutineScope()

    val webSocket = remember {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://192.168.247.41:8000/ws").build()
        client.newWebSocket(request, createWebSocketListener(recognitionResult))
    }

    DisposableEffect(Unit) {
        onDispose {
            webSocket.close(1000, "Screen destroyed")
        }
    }

    LaunchedEffect(recognitionResult.value) {
        recognitionResult.value?.let { result ->
            if (result != -1) {
                delay(1000) // 사용자가 결과를 볼 수 있도록 잠시 대기
                navController.navigate("main") {
                    popUpTo("test5") { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FaceLoginContent(
            onImageCaptured = { image ->
                processAndSendImage(image, webSocket, isProcessingImage, coroutineScope)
            },
            recognitionResult = recognitionResult
        )

        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Text("Back")
        }
    }
}

@Composable
fun FaceLoginContent(
    onImageCaptured: (ImageProxy) -> Unit,
    recognitionResult: State<Int?>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                            setAnalyzer(executor, { imageProxy ->
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

        recognitionResult.value?.let { result ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (result != -1) "Match Found!" else "No Match",
                    color = if (result != -1) Color.Green else Color.Red
                )
                if (result != -1) {
                    Text(
                        text = "Similar ID: $result",
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun createWebSocketListener(recognitionResult: MutableState<Int?>) = object : WebSocketListener() {
    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("WebSocket", "Received message: $text")
        recognitionResult.value = text.toIntOrNull() ?: -1
    }
}

private fun processAndSendImage(
    image: ImageProxy,
    webSocket: WebSocket,
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
            webSocket.send(jpegBytes.toByteString())
            Log.d("ImageProcessing", "Image sent: ${jpegBytes.size} bytes")
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