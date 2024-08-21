package com.example.ex0710

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.clickable
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.TextUnit
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import okhttp3.ResponseBody
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

data class Message(val id: String = UUID.randomUUID().toString(), val text: String, val isUser: Boolean)
data class Conversation(
    val id: String,
    val name: String,
    val messages: List<Message>,
    val savedDate: Date? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBackPress: () -> Unit, onSettingsClick: () -> Unit, tts: TextToSpeech) {
    val context = LocalContext.current
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Initialization successful
            }
        }
    }

    var showNewChatDialog by remember { mutableStateOf(false) }
    var showConversationList by remember { mutableStateOf(false) }
    var showSavedConversation by remember { mutableStateOf(false) }
    var selectedConversation by remember { mutableStateOf<Conversation?>(null) }
    var currentConversation by remember {
        mutableStateOf(AppState.conversations.value.firstOrNull { it.id == AppState.currentConversationId.value }
            ?: Conversation(UUID.randomUUID().toString(), "New Chat", emptyList()))
    }
    var inputText by rememberSaveable { mutableStateOf("") }
    val fontSize by remember { mutableStateOf(AppState.fontSize.value.value) }
    val chatBotService = remember { ChatBotService.create() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by rememberSaveable { mutableStateOf(currentConversation.messages) }

    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    fun getLocation(callback: (Location?) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback(null)
            return
        }

        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        callback(location)
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(messages.size) {
        listState.scrollToItem(0)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("HEALY", fontSize = fontSize.sp) },
            navigationIcon = {
                IconButton(onClick = {
                    AppState.currentConversationId.value = currentConversation.id
                    AppState.messages.value = currentConversation.messages
                    onBackPress()
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기")
                }
            },
            actions = {
                IconButton(onClick = { showNewChatDialog = true }) {
                    Icon(Icons.Default.Chat, contentDescription = "새 대화")
                }
                IconButton(onClick = { showConversationList = true }) {
                    Icon(Icons.Default.List, contentDescription = "대화 목록")
                }
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "설정")
                }
            }
        )

        if (showNewChatDialog) {
            NewChatDialog(
                onDismiss = { showNewChatDialog = false },
                onSaveAndNewChat = { name ->
                    val savedConversation = currentConversation.copy(name = name, savedDate = Date())
                    AppState.conversations.value = AppState.conversations.value + savedConversation
                    val newConversation = Conversation(
                        id = UUID.randomUUID().toString(),
                        name = "New Chat",
                        messages = emptyList()
                    )
                    currentConversation = newConversation
                    AppState.currentConversationId.value = newConversation.id
                    messages = newConversation.messages
                    showNewChatDialog = false
                },
                onNewChatWithoutSave = {
                    val newConversation = Conversation(
                        id = UUID.randomUUID().toString(),
                        name = "New Chat",
                        messages = emptyList()
                    )
                    currentConversation = newConversation
                    AppState.currentConversationId.value = newConversation.id
                    messages = newConversation.messages
                    showNewChatDialog = false
                }
            )
        }

        if (showConversationList) {
            ConversationListDialog(
                conversations = AppState.conversations.value,
                onDismiss = { showConversationList = false },
                onSelect = { conversation ->
                    selectedConversation = conversation
                    showSavedConversation = conversation.savedDate != null
                    showConversationList = false
                }
            )
        }

        if (showSavedConversation && selectedConversation != null) {
            ConversationDisplay(
                conversation = selectedConversation!!,
                onDismiss = {
                    showSavedConversation = false
                    selectedConversation = null
                },
                isReadOnly = true,
                tts = tts
            )
        } else if (!showSavedConversation && selectedConversation != null) {
            ConversationDisplay(
                conversation = selectedConversation!!,
                onDismiss = {
                    showSavedConversation = false
                    selectedConversation = null
                },
                isReadOnly = false,
                tts = tts
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            reverseLayout = true
        ) {
            items(messages, key = { it.id }) { message ->
                ChatBubble(message, fontSize.sp, tts)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("궁금한 내용을 질문하세요", fontSize = fontSize.sp) },
                textStyle = LocalTextStyle.current.copy(fontSize = fontSize.sp),
                enabled = currentConversation.savedDate == null
            )
            IconButton(onClick = {
                if (inputText.isNotBlank() && currentConversation.savedDate == null) {
                    val userMessage = Message(text = inputText, isUser = true)
                    val updatedMessages = listOf(userMessage) + messages
                    messages = updatedMessages
                    currentConversation = currentConversation.copy(messages = updatedMessages)
                    AppState.conversations.value = AppState.conversations.value.map {
                        if (it.id == currentConversation.id) currentConversation else it
                    }
                    inputText = ""

                    coroutineScope.launch {
                        try {
                            if (userMessage.text.toLowerCase() == "병원검색") {
                                val location = context.getLocationSuspend()
                                if (location != null) {
                                    Log.d("LocationDebug", "위치 정보: 위도 ${location.latitude}, 경도 ${location.longitude}")
                                    sendChatRequestWithLocation(
                                        chatBotService,
                                        userMessage.text,
                                        location.latitude,
                                        location.longitude,
                                        currentConversation,
                                        messages
                                    ) { newMessages ->
                                        messages = newMessages
                                        currentConversation = currentConversation.copy(messages = newMessages)
                                        AppState.conversations.value = AppState.conversations.value.map {
                                            if (it.id == currentConversation.id) currentConversation else it
                                        }
                                    }
                                } else {
                                    Log.e("LocationDebug", "위치 정보를 가져오지 못했습니다.")
                                    val errorMessage = Message(text = "위치 정보를 가져올 수 없습니다.", isUser = false)
                                    messages = listOf(errorMessage) + messages
                                    currentConversation = currentConversation.copy(messages = messages)
                                    AppState.conversations.value = AppState.conversations.value.map {
                                        if (it.id == currentConversation.id) currentConversation else it
                                    }
                                }
                            } else {
                                sendChatRequest(chatBotService, userMessage.text, currentConversation, messages) { newMessages ->
                                    messages = newMessages
                                    currentConversation = currentConversation.copy(messages = newMessages)
                                    AppState.conversations.value = AppState.conversations.value.map {
                                        if (it.id == currentConversation.id) currentConversation else it
                                    }
                                }
                            }
                        } catch (e: IOException) {
                            handleChatError("IO Error sending message", e, messages) { newMessages ->
                                messages = newMessages
                                currentConversation = currentConversation.copy(messages = newMessages)
                                AppState.conversations.value = AppState.conversations.value.map {
                                    if (it.id == currentConversation.id) currentConversation else it
                                }
                            }
                        } catch (e: HttpException) {
                            handleChatError("HTTP Error sending message", e, messages) { newMessages ->
                                messages = newMessages
                                currentConversation = currentConversation.copy(messages = newMessages)
                                AppState.conversations.value = AppState.conversations.value.map {
                                    if (it.id == currentConversation.id) currentConversation else it
                                }
                            }
                        }
                    }
                }
            }) {
                Icon(Icons.Filled.Send, contentDescription = "전송")
            }
        }
    }
}

suspend fun Context.getLocationSuspend(): Location? {
    return withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(
                this@getLocationSuspend,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        suspendCancellableCoroutine { continuation ->
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            continuation.resume(location)
        }
    }
}

suspend fun sendChatRequestWithLocation(
    chatBotService: ChatBotService,
    message: String,
    latitude: Double,
    longitude: Double,
    currentConversation: Conversation,
    currentMessages: List<Message>,
    onMessagesUpdated: (List<Message>) -> Unit
) {
    val chatRequest = ChatRequest(
        message = message,
        latitude = latitude,
        longitude = longitude,
        chat_history = currentConversation.messages.map { it.text },
        user_id = ""
    )

    val response = chatBotService.sendMessage(chatRequest)
    handleChatbotResponse(response, currentMessages, onMessagesUpdated)
}

suspend fun sendChatRequest(
    chatBotService: ChatBotService,
    message: String,
    currentConversation: Conversation,
    currentMessages: List<Message>,
    onMessagesUpdated: (List<Message>) -> Unit
) {
    val chatRequest = ChatRequest(
        message = message,
        chat_history = currentConversation.messages.map { it.text },
        user_id = ""
    )

    val response = chatBotService.sendMessage(chatRequest)
    handleChatbotResponse(response, currentMessages, onMessagesUpdated)
}

suspend fun handleChatbotResponse(
    response: ResponseBody,
    currentMessages: List<Message>,
    onMessagesUpdated: (List<Message>) -> Unit
) {
    val reader = response.byteStream().bufferedReader()
    var chatbotResponse = ""

    var botMessage = Message(text = "", isUser = false)
    var updatedMessages = listOf(botMessage) + currentMessages
    onMessagesUpdated(updatedMessages)

    while (true) {
        val line = reader.readLine() ?: break
        if (line.startsWith("data: ")) {
            val content = line.substring(6)
            if (content == "[DONE]") break
            chatbotResponse += content
            botMessage = botMessage.copy(text = chatbotResponse.trim())
            updatedMessages = updatedMessages.map { if (it.id == botMessage.id) botMessage else it }
            onMessagesUpdated(updatedMessages)
            delay(20)
        }
    }
}

fun handleChatError(
    errorMessage: String,
    exception: Exception,
    currentMessages: List<Message>,
    onMessagesUpdated: (List<Message>) -> Unit
) {
    Log.e("ChatError", errorMessage, exception)
    val errorMessage = Message(text = "챗봇 응답에 실패했습니다.", isUser = false)
    val updatedMessages = listOf(errorMessage) + currentMessages
    onMessagesUpdated(updatedMessages)
}

@Composable
fun ChatBubble(message: Message, fontSize: TextUnit, tts: TextToSpeech) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (message.isUser) Color.LightGray else Color.Gray)
                .padding(8.dp)
                .clickable {
                    tts.speak(message.text, TextToSpeech.QUEUE_FLUSH, null, null)
                }
        ) {
            Text(
                message.text,
                fontSize = fontSize,
                overflow = TextOverflow.Ellipsis,
                maxLines = 20
            )
        }
    }
}

@Composable
fun ConversationDisplay(
    conversation: Conversation,
    onDismiss: () -> Unit,
    isReadOnly: Boolean,
    tts: TextToSpeech
) {
    val savedDate = conversation.savedDate?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(it)
    } ?: "미정"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("대화 - ${conversation.name}") },
        text = {
            Column {
                Text("저장 날짜: $savedDate", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(conversation.messages) { message ->
                        ChatBubble(message, fontSize = 16.sp, tts = tts)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        },
        dismissButton = {
            if (!isReadOnly) {
                TextButton(onClick = onDismiss) {
                    Text("나가기")
                }
            }
        }
    )
}

@Composable
fun NewChatDialog(onDismiss: () -> Unit, onSaveAndNewChat: (String) -> Unit, onNewChatWithoutSave: () -> Unit) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 대화 만들기") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("대화 이름") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Button(onClick = { onSaveAndNewChat(name) }) {
                        Text("저장하고 새 대화 시작")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onNewChatWithoutSave) {
                        Text("저장하지 않고 새 대화 시작")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

@Composable
fun ConversationListDialog(
    conversations: List<Conversation>,
    onDismiss: () -> Unit,
    onSelect: (Conversation) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("대화 목록") },
        text = {
            LazyColumn {
                items(conversations) { conversation ->
                    Text(
                        text = conversation.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(conversation) }
                            .padding(vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}