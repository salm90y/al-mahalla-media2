package com.ps1.netplay.network

 
import  okhttp3.MediaType.Companion.toMediaType
import  okhttp3.OkHttpClient 
import  okhttp3.Request
import  okhttp3.RequestBody.Companion.toRequestBody 
import  kotlinx.coroutines.Dispatchers
import  kotlinx.coroutines.withContext

object ApiService {
    private val client = OkHttpClient()
private val JSON = "application/json; charset=utf-8".toMediaType()
suspend fun get(context: android.content.Context, path: String): String = withContext(Dispatchers.IO) {
        val url = CloudflareClient.getBaseUrl(context) + path.replace("/api", "")
val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${CloudflareClient.getAuthToken(context)}")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            return@withContext response.body?.string() ?: ""
        }
    }
suspend fun post(context: android.content.Context, path: String, jsonBody: String): String = withContext(Dispatchers.IO) {
        val url = CloudflareClient.getBaseUrl(context) + path.replace("/api", "")
val body = jsonBody.toRequestBody(JSON)
val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${CloudflareClient.getAuthToken(context)}")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            return@withContext response.body?.string() ?: ""
        }
    }
suspend fun delete(context: android.content.Context, path: String): String = withContext(Dispatchers.IO) {
        val url = CloudflareClient.getBaseUrl(context) + path.replace("/api", "")
val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${CloudflareClient.getAuthToken(context)}")
            .delete()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Unexpected code $response")
            return@withContext response.body?.string() ?: ""
        }
    }}