package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdCard
import com.example.data.entity.Achievement
import com.example.ui.components.AppHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberGradient
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onOpenWallet: () -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()
    val totalCompleted by viewModel.totalCompletedLevels.collectAsState()
    val achievements by viewModel.achievements.collectAsState()

    val memberDate = userProfile?.memberSinceTimestamp?.let {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(it))
    } ?: "October 2026"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 75.dp)
        ) {
            AppHeader(
                title = "PLAYER PROFILE",
                balance = walletBalance.availableBalance,
                onWalletClick = onOpenWallet
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile Avatar Card
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = GlassBorderCyan,
                        backgroundColor = Color(0xFF131A2C),
                        cornerRadius = 20.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .background(CyberGradient)
                                    .border(2.dp, ElectricBlue, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = userProfile?.username ?: "PuzzleMaster",
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Puzzle Champion • Tier 1",
                                    color = NeonPurple,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Member since $memberDate",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Stats Grid
                item {
                    Text(
                        text = "CAREER STATISTICS",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Games Played",
                            value = "${userProfile?.gamesPlayed ?: 0}",
                            color = ElectricBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Completed",
                            value = "$totalCompleted",
                            color = NeonEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            label = "Total Earned",
                            value = "₹${walletBalance.totalEarned.toInt()}",
                            color = RewardGoldLight,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Daily Streak",
                            value = "${userProfile?.currentStreak ?: 1} Days",
                            color = NeonPink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Achievements List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACHIEVEMENTS",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val unlockedCount = achievements.count { it.isUnlocked }
                        Text(
                            text = "$unlockedCount / ${achievements.size} Unlocked",
                            color = RewardGoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                items(achievements) { ach ->
                    AchievementItemCard(ach)
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        backgroundColor = Color(0xFF101726),
        cornerRadius = 14.dp
    ) {
        Column {
            Text(text = label, color = TextSecondary, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun AchievementItemCard(ach: Achievement) {
    val progress = (ach.progress.toFloat() / ach.maxProgress.coerceAtLeast(1)).coerceIn(0f, 1f)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 14.dp,
        backgroundColor = Color(0xFF101726),
        borderColor = if (ach.isUnlocked) Color(0x66FFB800) else Color(0xFF1E283C)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (ach.isUnlocked) Color(0x33FFB800) else Color(0xFF192233)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (ach.isUnlocked) Icons.Default.EmojiEvents else Icons.Default.MilitaryTech,
                    contentDescription = null,
                    tint = if (ach.isUnlocked) RewardGold else Color(0xFF6B7280),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = ach.title,
                        color = if (ach.isUnlocked) TextPrimary else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (ach.isUnlocked) {
                        Text(
                            text = "✓ DONE",
                            color = NeonEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = ach.description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (ach.isUnlocked) RewardGold else ElectricBlue,
                    trackColor = Color(0xFF1F2B40)
                )
            }
        }
    }
}
