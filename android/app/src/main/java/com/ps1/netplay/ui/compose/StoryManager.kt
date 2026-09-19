package com.ps1.netplay.ui.compose

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class UserStory(
    val id: String,
    val userId: String,
    val userName: String,
    val avatarUrl: String,
    val mediaUrl: String,
    val caption: String,
    val timestamp: Long
)

object StoryManager {
    private const val PREFS_NAME = "user_stories_storage"
    private const val KEY_STORIES = "active_stories_list"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAllStories(context: Context): List<UserStory> {
        val raw = getPrefs(context).getString(KEY_STORIES, null) ?: return emptyList()
        val list = mutableListOf<UserStory>()
        try {
            val array = JSONArray(raw)
            val now = System.currentTimeMillis()
            val expiryLimit = 24 * 60 * 60 * 1000L // 24 hours
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val time = obj.optLong("timestamp", 0)
                // Filter out stories older than 24 hours if needed
                if (now - time < expiryLimit || time == 0L) {
                    list.add(
                        UserStory(
                            id = obj.optString("id"),
                            userId = obj.optString("userId"),
                            userName = obj.optString("userName"),
                            avatarUrl = obj.optString("avatarUrl"),
                            mediaUrl = obj.optString("mediaUrl"),
                            caption = obj.optString("caption"),
                            timestamp = time
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun saveStory(
        context: Context,
        userId: String,
        userName: String,
        avatarUrl: String,
        mediaUrl: String,
        caption: String
    ) {
        val current = getAllStories(context).toMutableList()
        // Remove previous story for this user to keep latest
        current.removeAll { it.userId == userId || (it.userName.isNotBlank() && it.userName.equals(userName, ignoreCase = true)) }
        val newStory = UserStory(
            id = "story_${System.currentTimeMillis()}",
            userId = userId,
            userName = userName,
            avatarUrl = avatarUrl,
            mediaUrl = mediaUrl,
            caption = caption,
            timestamp = System.currentTimeMillis()
        )
        current.add(0, newStory)

        val array = JSONArray()
        current.forEach { s ->
            val obj = JSONObject().apply {
                put("id", s.id)
                put("userId", s.userId)
                put("userName", s.userName)
                put("avatarUrl", s.avatarUrl)
                put("mediaUrl", s.mediaUrl)
                put("caption", s.caption)
                put("timestamp", s.timestamp)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_STORIES, array.toString()).apply()
    }

    fun getStoryForUser(context: Context, userId: String, userName: String): UserStory? {
        val stories = getAllStories(context)
        return stories.firstOrNull { 
            (userId.isNotBlank() && it.userId == userId) || 
            (userName.isNotBlank() && it.userName.equals(userName, ignoreCase = true)) 
        }
    }

    fun hasActiveStory(context: Context, userId: String, userName: String): Boolean {
        return getStoryForUser(context, userId, userName) != null
    }
}
