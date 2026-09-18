package com.ps1.netplay.ui.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomTab.Chat.route

    fun navigateTo(tab: BottomTab) {
        if (currentRoute != tab.route) {
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main Navigation Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(Color.White)
                .border(
                    width = 1.dp,
                    color = Color(0xFFE2E8F0)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 1. الدردشات (Chat) - Right side in RTL
                BottomNavItem(
                    tab = BottomTab.Chat,
                    isSelected = currentRoute == BottomTab.Chat.route,
                    onClick = { navigateTo(BottomTab.Chat) }
                )

                // 2. الحالات (Stories) - Next to Chat
                BottomNavItem(
                    tab = BottomTab.Stories,
                    isSelected = currentRoute == BottomTab.Stories.route,
                    onClick = { navigateTo(BottomTab.Stories) }
                )

                // Space for Center FAB (الغرف)
                Spacer(modifier = Modifier.width(64.dp))

                // 4. المكالمات (Calls)
                BottomNavItem(
                    tab = BottomTab.Calls,
                    isSelected = currentRoute == BottomTab.Calls.route,
                    onClick = { navigateTo(BottomTab.Calls) }
                )

                // 5. الأصدقاء (Friends) - Left side in RTL
                BottomNavItem(
                    tab = BottomTab.Friends,
                    isSelected = currentRoute == BottomTab.Friends.route,
                    onClick = { navigateTo(BottomTab.Friends) }
                )
            }
        }

        // Center Floating Action Button (الغرف)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = (-12).dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navigateTo(BottomTab.Rooms)
                }
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF2563EB)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MeetingRoom,
                    contentDescription = "الغرف",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "الغرف",
                color = if (currentRoute == BottomTab.Rooms.route) Color(0xFF2563EB) else Color(0xFF64748B),
                fontSize = 11.sp,
                fontFamily = TajawalFontFamily,
                fontWeight = if (currentRoute == BottomTab.Rooms.route) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: BottomTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
        label = "nav_color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = tab.title,
            color = contentColor,
            fontSize = 11.sp,
            fontFamily = TajawalFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
