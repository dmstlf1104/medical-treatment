package com.example.ex0710

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@Composable
fun ScanResultScreen(
    navController: NavController,
    scanResultViewModel: ScanResultViewModel = viewModel(),
    sharedViewModel: SharedViewModel,
    onSettingsClick: () -> Unit
) {
    var showImageOptions by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var isUploading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val analysisResults by scanResultViewModel.analysisResults.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var showRationaleDialog by remember { mutableStateOf(false) }
    var permissionToRequest by remember { mutableStateOf<String?>(null) }

    val apiService = ApiService.create()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            when (permissionToRequest) {
                Manifest.permission.CAMERA -> hasCameraPermission = true
            }
            if (hasCameraPermission) {
                showImageOptions = true
            }
        } else {
            showRationaleDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let { uri ->
                title = TextFieldValue("") // 제목 초기화
            }
            showImageOptions = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            title = TextFieldValue("") // 제목 초기화
            showImageOptions = false
        }
    }

    val fontSize by remember { AppState.fontSize }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("검사 결과지 해석", style = MaterialTheme.typography.headlineMedium, fontSize = fontSize)
            Row {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "설정")
                }
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isUploading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("힐리가 이미지를 분석중이에요! 잠시만 기다려주세요..", fontSize = fontSize)
            }
        } else if (imageUri != null && !isUploading) {
            // 이미지 업로드 후 제목 입력 화면
            Column {
                Image(
                    painter = rememberImagePainter(imageUri),
                    contentDescription = "검사 결과지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("제목", fontSize = fontSize) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontSize = fontSize)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isUploading = true
                            try {
                                uploadImage(imageUri!!, apiService, context) { result ->
                                    val resultItem = ResultItem(imageUri!!, title.text, result)
                                    scanResultViewModel.addResultItem(resultItem)
                                    imageUri = null
                                    title = TextFieldValue("") // 제목 초기화
                                }
                            } catch (e: Exception) {
                                Log.e("ImageUpload", "Error uploading image", e)
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("저장", fontSize = fontSize)
                }
            }
        } else if (analysisResults.isNotEmpty()) {
            Text("내 결과지", style = MaterialTheme.typography.titleLarge, fontSize = fontSize)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(analysisResults) { resultItem ->
                    ResultItemCard(
                        resultItem = resultItem,
                        onItemClick = {
                            // 여기서 sharedViewModel을 사용
                            sharedViewModel.imageUri = resultItem.uri
                            sharedViewModel.title = resultItem.title
                            sharedViewModel.analysisResult = resultItem.result
                            navController.navigate("detailedResult")
                        },
                        onDeleteClick = {
                            scanResultViewModel.deleteResultItem(resultItem)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.result),
                        contentDescription = "Empty result",
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "의료 검사결과지를 올려주세요!",
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = fontSize,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "어려운 검사결과지를 눈높이에 맞는 쉬운 용어로 설명드려요.",
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = fontSize,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (imageUri == null) {
            Button(
                onClick = {
                    if (hasCameraPermission) {
                        showImageOptions = true
                    } else {
                        permissionToRequest = Manifest.permission.CAMERA
                        if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, Manifest.permission.CAMERA)) {
                            showRationaleDialog = true
                        } else {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("검사결과지 올리기", fontSize = fontSize)
            }
        }

        if (showRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showRationaleDialog = false },
                title = { Text("권한 필요", fontSize = fontSize) },
                text = { Text("이 기능을 사용하려면 카메라 권한이 필요합니다. 권한을 허용해주세요.", fontSize = fontSize) },
                confirmButton = {
                    Button(onClick = {
                        showRationaleDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text("설정으로 이동", fontSize = fontSize)
                    }
                },
                dismissButton = {
                    Button(onClick = { showRationaleDialog = false }) {
                        Text("취소", fontSize = fontSize)
                    }
                }
            )
        }

        if (showImageOptions) {
            AlertDialog(
                onDismissRequest = { showImageOptions = false },
                title = { Text("이미지 선택", fontSize = fontSize) },
                text = {
                    Column {
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    val uri = ComposeFileProvider.getImageUri(context)
                                    imageUri = uri
                                    cameraLauncher.launch(uri)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("카메라로 촬영하기", fontSize = fontSize)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("갤러리 불러오기", fontSize = fontSize)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showImageOptions = false }) {
                        Text("취소", fontSize = fontSize)
                    }
                }
            )
        }
    }
}



@Composable
fun ResultItemCard(
    resultItem: ResultItem,
    onItemClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onItemClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(resultItem.title, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, contentDescription = "삭제")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Image(
                painter = rememberImagePainter(resultItem.uri),
                contentDescription = "검사 결과지",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(resultItem.result, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

suspend fun uploadImage(uri: Uri, apiService: ApiService, context: Context, onResult: (String) -> Unit) {
    try {
        val file = createFileFromUri(uri, context)
        if (file != null) {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response: List<TermResult> = apiService.uploadImage(body)
            Log.d("ImageUpload", "Server response: $response")
            val analysisResult = response.find { it.term_ko == "분석 결과" }?.explanation ?: "분석 결과를 찾을 수 없습니다."
            onResult(analysisResult)
        } else {
            onResult("URI에서 파일을 생성하는 데 실패했습니다.")
        }
    } catch (e: Exception) {
        Log.e("ImageUpload", "Error uploading image", e)
        onResult("이미지 업로드 중 오류가 발생했습니다.")
    }
}

fun createFileFromUri(uri: Uri, context: Context): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("upload_image", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)

        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        tempFile
    } catch (e: Exception) {
        Log.e("FileUtils", "Error creating file from URI", e)
        null
    }
}

object ComposeFileProvider {
    fun getImageUri(context: Context): Uri {
        val directory = File(context.cacheDir, "images")
        directory.mkdirs()
        val file = File.createTempFile(
            "selected_image_",
            ".jpg",
            directory
        )
        val authority = context.packageName + ".fileprovider"
        return FileProvider.getUriForFile(
            context,
            authority,
            file
        )
    }
}

data class ResultItem(val uri: Uri, val title: String, val result: String)
