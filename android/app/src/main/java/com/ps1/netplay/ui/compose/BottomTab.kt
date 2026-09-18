package com.ps1.netplay.ui.compose
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomTab(val route: String, val title: String, val icon: ImageVector) {
    object Friends : BottomTab("friends", "الأصدقاء", Icons.Default.People)
    object Calls : BottomTab("calls", "المكالمات", Icons.Default.Call)
    object Rooms : BottomTab("rooms", "الغرف", Icons.Default.SportsEsports)
    object Stories : BottomTab("stories", "الحالات", Icons.Default.PhotoCamera)
    object Chat : BottomTab("chat", "الدردشات", Icons.Default.ChatBubble)
    
    companion object {
        val tabs = listOf(Friends, Calls, Rooms, Stories, Chat)
    }
}
