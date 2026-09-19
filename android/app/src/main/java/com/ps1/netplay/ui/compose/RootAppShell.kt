package com.ps1.netplay.ui.compose
import android.widget.*
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun RootAppShell() {
    val navController = rememberNavController()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = Color(0xFFF8FAFC),
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        onNavigateBack = { navController.popBackStack() }
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
                        onNavigateBack = { navController.popBackStack() }
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
        }
    }
}