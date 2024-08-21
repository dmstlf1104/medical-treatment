package com.example.ex0710

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp



object AppState {
    var fontSize = mutableStateOf(16.sp)
    var messages = mutableStateOf(listOf<Message>())
    var conversations = mutableStateOf(listOf<Conversation>())
    var currentConversationId = mutableStateOf<String?>(null)
}
