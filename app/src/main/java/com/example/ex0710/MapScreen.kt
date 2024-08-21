package com.example.ex0710

import android.Manifest
import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.net.URLEncoder
import android.util.Log
import kotlin.math.*

data class SearchResult(
    val name: String,
    val address: String,
    val roadAddress: String,
    val lat: Double,
    val lng: Double,
    val phone: String
)

private suspend fun searchLocalPlaces(client: OkHttpClient, query: String): List<SearchResult> =
    withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://dapi.kakao.com/v2/local/search/keyword.json?query=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "KakaoAK 743c9273b9ee31811f6763fc88216080")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        Log.d("API Response", "Code: ${response.code}, Body: $responseBody")

        if (response.isSuccessful && responseBody != null) {
            val jsonObject = JSONObject(responseBody)
            val documents = jsonObject.getJSONArray("documents")
            val results = mutableListOf<SearchResult>()

            for (i in 0 until documents.length()) {
                val document = documents.getJSONObject(i)
                val category = document.getString("category_name")
                if (category.startsWith("의료,건강")) {
                    val name = document.getString("place_name")
                    val address = document.getString("address_name")
                    val roadAddress = document.optString("road_address_name", "")
                    val lat = document.getDouble("y")
                    val lng = document.getDouble("x")
                    val phone = document.optString("phone", "정보 없음")

                    results.add(SearchResult(name, address, roadAddress, lat, lng, phone))
                }
            }
            results
        } else {
            val errorBody = responseBody ?: "No error message"
            throw Exception("API 요청 실패: ${response.code} ${response.message}, 응답 본문: $errorBody")
        }
    }

private val client = OkHttpClient.Builder()
    .addInterceptor(HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    })
    .build()

@Composable
fun MapScreen(onBackPress: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val coroutineScope = rememberCoroutineScope()

    val locationSource = remember {
        FusedLocationSource(activity as ComponentActivity, LOCATION_PERMISSION_REQUEST_CODE)
    }

    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var locationTrackingMode by remember { mutableStateOf(LocationTrackingMode.None) }
    var selectedResult by remember { mutableStateOf<SearchResult?>(null) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var isLocationButtonClicked by remember { mutableStateOf(false) }
    var naverMapState by remember { mutableStateOf<NaverMap?>(null) }

    LaunchedEffect(key1 = Unit) {
        if (activity != null && !hasLocationPermissions(activity)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("병원 검색", fontSize = 14.sp, color = Color.Gray) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            try {
                                searchResults = searchLocalPlaces(client, searchQuery)
                                isLoading = false
                                if (searchResults.isNotEmpty()) {
                                    val firstResult = searchResults.first()
                                    isLocationButtonClicked = false // 검색 시 위치 버튼 클릭 상태 리셋
                                    naverMapState?.moveCamera(CameraUpdate.scrollTo(LatLng(firstResult.lat, firstResult.lng)))
                                }
                            } catch (e: Exception) {
                                errorMessage = "검색 중 오류 발생: ${e.message}"
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("검색")
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorMessage!!, color = Color.Red)
                }
            } else {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize()
                ) { mapView ->
                    mapView.getMapAsync { naverMap ->
                        naverMapState = naverMap
                        setupMap(naverMap, locationSource, locationTrackingMode, searchResults, currentLocation, isLocationButtonClicked) { result ->
                            selectedResult = result
                            locationTrackingMode = LocationTrackingMode.None
                            naverMap.locationTrackingMode = LocationTrackingMode.None
                        }

                        naverMap.addOnLocationChangeListener { location ->
                            currentLocation = LatLng(location.latitude, location.longitude)
                            if (isLocationButtonClicked) {
                                naverMap.moveCamera(CameraUpdate.scrollTo(currentLocation!!))
                                isLocationButtonClicked = false
                            }
                        }

                        naverMap.uiSettings.isLocationButtonEnabled = true

                        naverMap.addOnOptionChangeListener {
                            if (naverMap.locationTrackingMode == LocationTrackingMode.Follow) {
                                isLocationButtonClicked = true
                                coroutineScope.launch {
                                    delay(100) // 약간의 지연을 추가하여 위치 업데이트를 기다립니다
                                    currentLocation?.let { location ->
                                        naverMap.moveCamera(CameraUpdate.scrollTo(location))
                                    }
                                }
                            } else {
                                isLocationButtonClicked = false // 위치 추적 모드가 해제되면 상태 리셋
                            }
                        }
                    }
                }
            }
        }

        if (selectedResult != null) {
            SearchResultInfoDialog(result = selectedResult!!, onDismiss = { selectedResult = null })
        }
    }
}

private fun setupMap(
    naverMap: NaverMap,
    locationSource: FusedLocationSource,
    locationTrackingMode: LocationTrackingMode,
    searchResults: List<SearchResult>,
    currentLocation: LatLng?,
    isLocationButtonClicked: Boolean,
    onResultSelected: (SearchResult) -> Unit
) {
    naverMap.locationSource = locationSource
    naverMap.locationTrackingMode = locationTrackingMode
    naverMap.uiSettings.isLocationButtonEnabled = true

    val locationOverlay = naverMap.locationOverlay
    locationOverlay.isVisible = true

    searchResults.forEach { result ->
        val marker = Marker().apply {
            position = LatLng(result.lat, result.lng)
            captionText = result.name
            map = naverMap

            if (currentLocation != null) {
                val distance = calculateDistance(
                    currentLocation.latitude, currentLocation.longitude,
                    result.lat, result.lng
                )
                this.iconTintColor = if (distance <= 5000)
                    android.graphics.Color.BLUE else android.graphics.Color.GREEN
            } else {
                this.iconTintColor = android.graphics.Color.GREEN
            }
        }
        marker.setOnClickListener {
            onResultSelected(result)
            true
        }
    }

    if (searchResults.isNotEmpty() && !isLocationButtonClicked) {
        val firstResult = searchResults.first()
        naverMap.moveCamera(CameraUpdate.scrollTo(LatLng(firstResult.lat, firstResult.lng)))
    }
}

@Composable
fun SearchResultInfoDialog(result: SearchResult, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = result.name) },
        text = {
            Column {
                Text("주소: ${result.address}")
                Text("도로명 주소: ${result.roadAddress}")
                Text("전화번호: ${result.phone}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1000

private fun hasLocationPermissions(context: Activity): Boolean {
    return ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371000.0
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLng = Math.toRadians(lng2 - lng1)
    val a = sin(deltaLat / 2).pow(2.0) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLng / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}