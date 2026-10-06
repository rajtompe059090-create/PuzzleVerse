package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
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
import com.example.core.Constants
import com.example.data.entity.GameLevel
import com.example.ui.components.AppHeader
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBgCard
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun LevelMapScreen(
    gameId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayLevel: (levelNumber: Int) -> Unit,
    onOpenWallet: () -> Unit
) {
    val levels by viewModel.getLevelsForGame(gameId).collectAsState(initial = emptyList())
    val walletBalance by viewModel.walletBalance.collectAsState()

    val gameTitle = when (gameId) {
        Constants.GAME_ARROW_FLOW -> "Arrow Flow"
        Constants.GAME_BLOCK_MERGE -> "Block Merge"
        Constants.GAME_COLOR_PATH -> "Color Path"
        else -> "Tile Match"
    }

    val completedCount = levels.count { it.isCompleted }
    val progressFraction = completedCount.toFloat() / Constants.TOTAL_LEVELS_PER_GAME.coerceAtLeast(1)

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
                title = gameTitle.uppercase(),
                balance = walletBalance.availableBalance,
                onWalletClick = onOpenWallet,
                onBackClick = onBack
            )

            // Progress Banner
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                borderColor = GlassBorderCyan,
                backgroundColor = Color(0xFF131B2D)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$completedCount / ${Constants.TOTAL_LEVELS_PER_GAME} Levels Completed",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = RewardGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Total Rewards: ₹$completedCount",
                                color = RewardGoldLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ElectricBlue,
                        trackColor = Color(0xFF22304A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Level Grid (Levels 1 to 100)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 90.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(levels, key = { it.levelNumber }) { level ->
                    LevelCard(
                        level = level,
                        onClick = {
                            if (level.isUnlocked || level.isCompleted) {
                                onPlayLevel(level.levelNumber)
                            }
                        }
                    )
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun LevelCard(
    level: GameLevel,
    onClick: () -> Unit
) {
    val isCompleted = level.isCompleted
    val isUnlocked = level.isUnlocked
    val isCurrent = isUnlocked && !isCompleted

    val (bgColor, borderColor, textColor) = when {
        isCompleted -> Triple(Color(0xFF0F2421), NeonEmerald, NeonEmerald)
        isCurrent -> Triple(Color(0xFF132238), ElectricBlue, ElectricBlue)
        else -> Triple(Color(0xFF101624), Color(0xFF1C273B), Color(0xFF55657E))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isCurrent) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(enabled = isUnlocked || isCompleted, onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Status Icon badge
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> Color(0x3310B981)
                            isCurrent -> Color(0x3300E5FF)
                            else -> Color(0xFF192338)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isCompleted -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = NeonEmerald,
                        modifier = Modifier.size(16.dp)
                    )
                    isCurrent -> Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Current",
                        tint = ElectricBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Level ${level.levelNumber}",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // Reward or best score
            Text(
                text = if (isCompleted) "✓ Claimed" else "₹${level.rewardAmount.toInt()}",
                color = if (isCompleted) NeonEmerald else RewardGoldLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Stars
            if (isCompleted) {
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    repeat(3) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RewardGold,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
