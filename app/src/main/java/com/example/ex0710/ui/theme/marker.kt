package com.example.ex0710.ui.theme

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Hospital(
    val id: Int,
    val city_name: String,
    val company_name: String,
    val medical_subject: String,
    val latitude: Double,
    val longitude: Double
)

fun loadHospitalsFromJson(context: Context, fileName: String): List<Hospital> {
    val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
    val hospitalListType = object : TypeToken<List<Hospital>>() {}.type
    return Gson().fromJson(jsonString, hospitalListType)
}
