package com.ps1.netplay.ui.compose
import android.widget.*
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.ps1.netplay.AuthActivity
import com.ps1.netplay.SettingsActivity
import com.ps1.netplay.UserManager

@Composable
fun ProfileScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser = remember { UserManager.getCurrentUser(context) }
    val username = currentUser?.username ?: "AshahMahan"
    val displayName = currentUser?.fullName?.ifEmpty { username } ?: "Ashah Mahan"
    val phoneOrStatus = if (!currentUser?.email.isNullOrEmpty()) currentUser?.email!! else "+964 770 123 4567"
    val roleBadge = currentUser?.role ?: "VIP Pro Player"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1424))
    ) {
        /* Sticky Header / Title */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryHeaderGradient)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الملف الشخصي",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { /* Open Edit Profile */ }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            /* 1. Header Profile Card with 3D Framing & Gradients (#1C2A52 to #3B2B78) */
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(18.dp))
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF1C2A52), Color(0xFF3B2B78))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF6C5CE7).copy(alpha = 0.5f), Color(0xFF00D2D3).copy(alpha = 0.3f))
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        /* 3D Avatar Frame */
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .shadow(8.dp, CircleShape)
                                .background(
                                    brush = Brush.linearGradient(
                                        listOf(Color(0xFF8A2BE2), Color(0xFF00D2D3))
                                    ),
                                    shape = CircleShape
                                )
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = currentUser?.avatar?.ifEmpty { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop" }
                                    ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200&h=200&fit=crop",
                                contentDescription = "Avatar",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        /* Full Name */
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        /* User Handle */
                        Text(
                            text = "@$username",
                            color = Color(0xFF00D2D3),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        /* Primary Phone / Email */
                        Text(
                            text = phoneOrStatus,
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        /* Role Badge */
                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.horizontalGradient(
                                        listOf(Color(0xFF6C5CE7), Color(0xFF4834DF))
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = roleBadge,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            /* 2. NetPlay Stats Card */
            item {
                GlassSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        ProfileStatItem(title = "Matches", value = "128", icon = Icons.Default.SportsEsports)
                        ProfileStatItem(title = "Win Rate", value = "74%", icon = Icons.Default.EmojiEvents)
                        ProfileStatItem(title = "Avg Ping", value = "18ms", icon = Icons.Default.Bolt)
                    }
                }
            }

            /* 3. Primary Actions Group */
            item {
                GlassSurfaceCard {
                    Column {
                        ProfileActionRow(
                            icon = Icons.Default.Bookmark,
                            badgeColor = Color(0xFF6C5CE7),
                            title = "Saved Messages",
                            subtitle = "Cloud scratchpad & pinned roms",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFF283256), thickness = 0.8.dp)
                        ProfileActionRow(
                            icon = Icons.Default.Sync,
                            badgeColor = Color(0xFF00D2D3),
                            title = "Contacts & Sync",
                            subtitle = "Sync friends via NetPlay network",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFF283256), thickness = 0.8.dp)
                        ProfileActionRow(
                            icon = Icons.Default.Insights,
                            badgeColor = Color(0xFFFFA502),
                            title = "NetPlay Stats",
                            subtitle = "Multiplayer records & leaderboard",
                            onClick = {}
                        )
                    }
                }
            }

            /* 4. Security & Settings Group */
            item {
                GlassSurfaceCard {
                    Column {
                        ProfileActionRow(
                            icon = Icons.Default.Security,
                            badgeColor = Color(0xFF2ED573),
                            title = "Privacy & Security",
                            subtitle = "Two-factor auth, cloud encryption",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFF283256), thickness = 0.8.dp)
                        ProfileActionRow(
                            icon = Icons.Default.Settings,
                            badgeColor = Color(0xFF70A1FF),
                            title = "App Settings",
                            subtitle = "Notifications, theme, emulator BIOS",
                            onClick = {
                                val intent = Intent(context, SettingsActivity::class.java)
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(color = Color(0xFF283256), thickness = 0.8.dp)
                        ProfileActionRow(
                            icon = Icons.Default.Logout,
                            badgeColor = Color(0xFFFF4757),
                            title = "Logout",
                            subtitle = "Sign out from this device",
                            isDestructive = true,
                            onClick = {
                                UserManager.logout(context)
                                val intent = Intent(context, AuthActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .background(
                color = Color(0xFF161C33),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = Color(0xFF252D4D),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        content()
    }
}

@Composable
fun ProfileStatItem(title: String, value: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = title, tint = Color(0xFF00D2D3), modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(text = title, color = Color(0xFF94A3B8), fontSize = 11.sp)
    }
}

@Composable
fun ProfileActionRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /* Indigo Icon Badge */
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(badgeColor.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = badgeColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isDestructive) Color(0xFFFF4757) else Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = Color(0xFF8395A7),
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF4A5568),
            modifier = Modifier.size(20.dp)
        )
    }
}