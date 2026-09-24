package com.ps1.netplay.ui.compose

import android.content.Context
import android.content.Intent
import android.widget.*
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ps1.netplay.CallActivity
import com.ps1.netplay.network.CallSignalingManager

@Composable
fun RootAppShell() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Start App-wide Incoming Call Watcher
    LaunchedEffect(Unit) {
        CallSignalingManager.startGlobalIncomingCallWatcher(context)
    }

    val incomingCall = CallSignalingManager.globalIncomingCall
    val activeCallSession = CallSignalingManager.currentSession

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                containerColor = Color(0xFFF8FAFC),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    if (CallActivity.isCallActive || activeCallSession != null) {
                        OngoingCallTopBanner(
                            callerName = activeCallSession?.targetUserName ?: "مكالمة جارية",
                            duration = CallSignalingManager.formatDuration(CallSignalingManager.callDurationSeconds),
                            isVideo = activeCallSession?.isVideo ?: false,
                            onReturnToCall = {
                                val sess = CallSignalingManager.currentSession
                                val intent = Intent(context, CallActivity::class.java).apply {
                                    putExtra("callID", sess?.roomId ?: "call_${System.currentTimeMillis()}")
                                    putExtra("isVideo", sess?.isVideo ?: false)
                                    putExtra("isIncoming", sess?.isOutgoing != true)
                                    putExtra("targetUserId", sess?.targetUserId ?: "")
                                    putExtra("targetUserName", sess?.targetUserName ?: "مستخدم")
                                    putExtra("targetUserAvatar", sess?.targetUserAvatar ?: "")
                                }
                                context.startActivity(intent)
                            },
                            onEndCall = {
                                CallSignalingManager.endCall(context)
                            }
                        )
                    }
                },
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isBottomTab = currentRoute in listOf(
                        BottomTab.Chat.route,
                        BottomTab.Stories.route,
                        BottomTab.Rooms.route,
                        BottomTab.Calls.route,
                        BottomTab.Friends.route
                    )
                    if (isBottomTab) {
                        AppBottomNavigationBar(navController = navController)
                    }
                }
            ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomTab.Chat.route,
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                composable(BottomTab.Chat.route) {
                    ChatListScreen(navController = navController)
                }
                composable(BottomTab.Stories.route) {
                    StoriesCameraScreen(navController = navController)
                }
                composable(BottomTab.Rooms.route) {
                    RoomsScreen(navController = navController)
                }
                composable(BottomTab.Calls.route) {
                    CallsScreen(navController = navController)
                }
                composable(BottomTab.Friends.route) {
                    FriendsScreen(navController = navController)
                }
                composable("friend_requests") {
                    FriendRequestsScreen(navController = navController)
                }
                composable(
                    route = "chat_detail/{chatId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("chatId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val targetUserId = backStackEntry.arguments?.getString("chatId") ?: ""
                    ChatDetailScreen(
                        targetUserId = targetUserId,
                        userName = targetUserId,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenProfile = { uid, uname ->
                            val encoded = try { java.net.URLEncoder.encode(uname, "UTF-8") } catch (_: Exception) { uname }
                            navController.navigate("user_profile/$uid/$encoded")
                        }
                    )
                }
                composable(
                    route = "chat_detail/{chatId}/{chatName}",
                    arguments = listOf(
                        androidx.navigation.navArgument("chatId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("chatName") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val targetUserId = backStackEntry.arguments?.getString("chatId") ?: ""
                    val rawName = backStackEntry.arguments?.getString("chatName") ?: "محادثة"
                    val chatName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
                    ChatDetailScreen(
                        targetUserId = targetUserId,
                        userName = chatName,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenProfile = { uid, uname ->
                            val encoded = try { java.net.URLEncoder.encode(uname, "UTF-8") } catch (_: Exception) { uname }
                            navController.navigate("user_profile/$uid/$encoded")
                        }
                    )
                }
                composable(
                    route = "user_profile/{userId}",
                    arguments = listOf(
                        androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    UserProfileDetailScreen(
                        userId = userId,
                        userName = userId,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenChat = { id, name ->
                            val encoded = try { java.net.URLEncoder.encode(name, "UTF-8") } catch (_: Exception) { name }
                            navController.navigate("chat_detail/$id/$encoded")
                        }
                    )
                }
                composable(
                    route = "user_profile/{userId}/{userName}",
                    arguments = listOf(
                        androidx.navigation.navArgument("userId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("userName") { type = androidx.navigation.NavType.StringType }
                    )
                ) { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    val rawName = backStackEntry.arguments?.getString("userName") ?: userId
                    val userName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (_: Exception) { rawName }
                    UserProfileDetailScreen(
                        userId = userId,
                        userName = userName,
                        onNavigateBack = { navController.popBackStack() },
                        onOpenChat = { id, name ->
                            val encoded = try { java.net.URLEncoder.encode(name, "UTF-8") } catch (_: Exception) { name }
                            navController.navigate("chat_detail/$id/$encoded")
                        }
                    )
                }
                composable("quran_home") {
                    QuranHomeScreen(navController = navController)
                }
                composable("games_room") {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    GamesRoomScreen(
                        onBack = { navController.popBackStack() },
                        onStartPs1 = {
                            val intent = android.content.Intent(context, com.ps1.netplay.MainActivity::class.java)
                            context.startActivity(intent)
                        }
                    )
                }
                composable("admin_dashboard") {
                    AdminDashboardScreen(navController = navController)
                }
                composable("notifications") {
                    NotificationsScreen(navController = navController)
                }
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(BottomTab.Rooms.route) {
                                popUpTo(0)
                            }
                        }
                    )
                }
            }

            // Global Incoming Call Alert Modal for the whole app
            incomingCall?.let { incCall ->
                IncomingCallAlertModal(
                    callData = incCall,
                    onAccept = {
                        val intent = Intent(context, CallActivity::class.java).apply {
                            putExtra("callID", incCall.callRoomId)
                            putExtra("isVideo", incCall.isVideo)
                            putExtra("isIncoming", true)
                            putExtra("targetUserId", incCall.callerId)
                            putExtra("targetUserName", incCall.callerName)
                            putExtra("targetUserAvatar", incCall.callerAvatar)
                        }
                        context.startActivity(intent)
                    },
                    onDecline = {
                        CallSignalingManager.declineIncomingCall(
                            context = context,
                            callerId = incCall.callerId,
                            roomId = incCall.callRoomId,
                            isVideo = incCall.isVideo
                        )
                    }
                )
            }
        }
    }
}