package com.example.ex0710

import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class SharedViewModel : ViewModel() {
    var imageUri by mutableStateOf<Uri?>(null)
    var title by mutableStateOf("")
    var analysisResult by mutableStateOf("")
}

class MainViewModel : ViewModel() {
    private val apiService: ApiService_analysis = Retrofit.Builder()
        .baseUrl("http://192.168.247.41:8002/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService_analysis::class.java)

    fun searchTerms(query: String, onResult: (Map<String, List<String>>) -> Unit) {
        viewModelScope.launch {
            try {
                val result = apiService.searchTerms(mapOf("query" to query))
                onResult(result)
            } catch (e: Exception) {
                onResult(mapOf("error" to listOf(e.message ?: "알 수 없는 오류가 발생했습니다.")))
            }
        }
    }
}

interface ApiService_analysis {
    @POST("search_terms")
    suspend fun searchTerms(@Body query: Map<String, String>): Map<String, List<String>>
}

@Composable
fun DetailedResultScreen(
    onBackPress: () -> Unit,
    onSettingsClick: () -> Unit,
    sharedViewModel: SharedViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel()
) {
    val imageUri = sharedViewModel.imageUri
    val title = sharedViewModel.title
    val analysisResult = sharedViewModel.analysisResult

    var spannableResult by remember { mutableStateOf<SpannableString?>(null) }
    var showTermDialog by remember { mutableStateOf(false) }
    var termDialogContent by remember { mutableStateOf("") }

    val fontSize = AppState.fontSize.value
    var showImageDialog by remember { mutableStateOf(false) }
    var scale by remember { mutableStateOf(1f) }
    val state = rememberTransformableState { zoomChange, _, _ ->
        scale *= zoomChange
    }

    LaunchedEffect(analysisResult) {
        mainViewModel.searchTerms(analysisResult) { response ->
            spannableResult = createClickableSpannableString(analysisResult, response) { content ->
                termDialogContent = content
                showTermDialog = true
            }
        }
    }

    if (showImageDialog) {
        AlertDialog(
            onDismissRequest = {
                showImageDialog = false
                scale = 1f
            },
            confirmButton = {
                Button(onClick = {
                    showImageDialog = false
                    scale = 1f
                }) {
                    Text("닫기")
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(uri),
                            contentDescription = "확대된 검사 결과지",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale
                                )
                                .transformable(state = state)
                        )
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("검사 결과", style = MaterialTheme.typography.headlineMedium, fontSize = fontSize)
                Row {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                    IconButton(onClick = onBackPress) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(title, style = MaterialTheme.typography.titleMedium, fontSize = fontSize)

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clickable { showImageDialog = true }
            ) {
                imageUri?.let { uri ->
                    Image(
                        painter = rememberImagePainter(uri),
                        contentDescription = "검사 결과지",
                        modifier = Modifier.matchParentSize()
                    )
                }
                Icon(
                    Icons.Default.Search,
                    contentDescription = "이미지 확대",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("결과지 설명", style = MaterialTheme.typography.titleMedium, fontSize = fontSize)

            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            AndroidView(
                factory = { context ->
                    TextView(context).apply {
                        movementMethod = LinkMovementMethod.getInstance()
                        setTextColor(Color.BLACK)  // 기본 텍스트 색상 설정
                    }
                },
                update = { textView ->
                    textView.text = spannableResult
                    textView.textSize = fontSize.value  // 동적 폰트 크기 적용
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onBackPress) {
                Text("뒤로가기", fontSize = fontSize)
            }
        }
    }

    if (showTermDialog) {
        AlertDialog(
            onDismissRequest = { showTermDialog = false },
            title = { Text("단어 설명") },
            text = { Text(termDialogContent) },
            confirmButton = {
                TextButton(onClick = { showTermDialog = false }) {
                    Text("확인")
                }
            }
        )
    }
}

fun createClickableSpannableString(
    text: String,
    clickableTerms: Map<String, List<String>>,
    onClick: (String) -> Unit
): SpannableString {
    val spannableString = SpannableString(text)

    clickableTerms.forEach { (term, definitions) ->
        val startIndex = text.indexOf(term)
        if (startIndex != -1) {
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(view: View) {
                    onClick(definitions.joinToString("\n"))
                }

                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.color = Color.BLUE
                    ds.isUnderlineText = true
                }
            }
            spannableString.setSpan(clickableSpan, startIndex, startIndex + term.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    return spannableString
}