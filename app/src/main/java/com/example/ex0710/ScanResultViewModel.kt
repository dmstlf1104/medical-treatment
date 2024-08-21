package com.example.ex0710

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ScanResultViewModel : ViewModel() {
    private val _analysisResults = MutableStateFlow<List<ResultItem>>(emptyList())
    val analysisResults: StateFlow<List<ResultItem>> = _analysisResults

    fun addResultItem(resultItem: ResultItem) {
        val currentResults = _analysisResults.value.toMutableList()
        currentResults.add(resultItem)
        _analysisResults.value = currentResults
    }

    fun deleteResultItem(resultItem: ResultItem) {
        val currentResults = _analysisResults.value.toMutableList()
        currentResults.remove(resultItem)
        _analysisResults.value = currentResults
    }
}