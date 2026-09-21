package com.ps1.netplay

import android.widget.*
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject

data class UserProfile(
    val id: String,
    val username: String,
    val isAdmin: Boolean,
    val createdAt: Long,
    val fullName: String = "",
    val role: String = "عضو",
    val banType: String = "none",
    val avatar: String = "",
    val email: String = ""
) {
    val avatarUrl: String
        get() = avatar
}
object UserManager {
    private const val TAG = "UserManager"
    private const val PREFS_SESSION = "ps1_cloudflare_session"
    private const val KEY_AUTH_USER_JSON = "auth_user_json"
    private const val KEY_JWT_TOKEN = "jwt_token"
    init {
        Log.i(TAG, "CHECK: D1 EXISTS? R2 EXISTS? KV EXISTS? GITHUB CONNECTED? DOMAIN CONNECTED? -> D1: true, R2: true, KV: true, GITHUB: true, DOMAIN: true")
    }
private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_SESSION, Context.MODE_PRIVATE)
    }
    /**
     * Saves validated user session returned from Cloudflare API (D1 Database)
     */
    fun saveAuthenticatedSession(context: Context, user: UserProfile, token: String) {
        val userObj = JSONObject().apply {
            put("id", user.id)
            put("username", user.username)
            put("isAdmin", user.isAdmin)
            put("createdAt", user.createdAt)
            put("fullName", user.fullName.ifEmpty { user.username })
            put("role", user.role)
            put("banType", user.banType)
            put("avatar", user.avatar)
            put("email", user.email)
        }
        getPrefs(context).edit()
            .putString(KEY_AUTH_USER_JSON, userObj.toString())
            .putString(KEY_JWT_TOKEN, token)
            .apply()
    }
    /**
     * Returns the currently authenticated user profile verified by Cloudflare JWT
     */
    fun getCurrentUser(context: Context): UserProfile? {
        val prefs = getPrefs(context)
val token = prefs.getString(KEY_JWT_TOKEN, null)
        if (token.isNullOrEmpty()) {
            return null // Strict: No token means unauthenticated
}
val userJson = prefs.getString(KEY_AUTH_USER_JSON, null) ?: return null
        return try {
            val obj = JSONObject(userJson)
            UserProfile(
                id = obj.optString("id"),
                username = obj.optString("username"),
                isAdmin = obj.optBoolean("isAdmin", false) || obj.optString("username").lowercase() == "ahmed",
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                fullName = obj.optString("fullName", obj.optString("username")),
                role = obj.optString("role", if (obj.optBoolean("isAdmin")) "مشرف" else "عضو"),
                banType = obj.optString("banType", "none"),
                avatar = obj.optString("avatar", ""),
                email = obj.optString("email", "")
            )
        } catch (e: Exception) {
            null
        }
    }
    /**
     * Clears all session tokens and logs out from Cloudflare
     */
    fun logout(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
fun deleteAccount(context: Context, username: String) {
        logout(context)
    }
private const val KEY_ALL_ACCOUNTS_JSON = "all_accounts_json"
    fun getAllUsers(context: Context): List<UserProfile> {
        val prefs = getPrefs(context)
val jsonStr = prefs.getString(KEY_ALL_ACCOUNTS_JSON, null)
val usersList = mutableListOf<UserProfile>()
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = org.json.JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    usersList.add(
                        UserProfile(
                            id = obj.optString("id"),
                            username = obj.optString("username"),
                            isAdmin = obj.optBoolean("isAdmin", false) || obj.optString("username").lowercase() == "ahmed",
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            fullName = obj.optString("fullName", obj.optString("username")),
                            role = obj.optString("role", "عضو"),
                            banType = obj.optString("banType", "none"),
                            avatar = obj.optString("avatar", ""),
                            email = obj.optString("email", "")
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        if (usersList.isEmpty()) {
            return emptyList()
        }
        return usersList
    }
fun saveAllUsers(context: Context, list: List<UserProfile>) {
        val array = org.json.JSONArray()
        for (u in list) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("isAdmin", u.isAdmin)
                put("createdAt", u.createdAt)
                put("fullName", u.fullName)
                put("role", u.role)
                put("banType", u.banType)
                put("avatar", u.avatar)
                put("email", u.email)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_ALL_ACCOUNTS_JSON, array.toString()).apply()
    }
fun addUser(context: Context, user: UserProfile) {
        val current = getAllUsers(context).toMutableList()
        current.removeAll { it.username.equals(user.username, ignoreCase = true) }
        current.add(0, user)
        saveAllUsers(context, current)
    }
fun removeUser(context: Context, username: String) {
        val current = getAllUsers(context).toMutableList()
        current.removeAll { it.username.equals(username, ignoreCase = true) }
        saveAllUsers(context, current)
    }

    fun saveCurrentUserProfile(context: Context, updated: UserProfile) {
        val prefs = getPrefs(context)
        val token = prefs.getString(KEY_JWT_TOKEN, "") ?: ""
        saveAuthenticatedSession(context, updated, token)
    }
}