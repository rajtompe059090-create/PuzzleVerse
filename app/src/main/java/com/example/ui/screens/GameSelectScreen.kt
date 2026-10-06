package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.BannerAdCard
import com.example.core.Constants
import com.example.data.entity.Game
import com.example.ui.components.AppHeader
import com.example.ui.components.GlassCard
import com.example.ui.components.Neon3DButton
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderCyan
import com.example.ui.theme.GlassBorderPurple
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel

@Composable
fun GameSelectScreen(
    viewModel: MainViewModel,
    onSelectGame: (gameId: String) -> Unit,
    onOpenWallet: () -> Unit
) {
    val games by viewModel.allGames.collectAsState()
    val walletBalance by viewModel.walletBalance.collectAsState()

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
                title = "PUZZLE GAMES",
                balance = walletBalance.availableBalance,
                onWalletClick = onOpenWallet
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(games) { game ->
                    GameCardItem(
                        game = game,
                        viewModel = viewModel,
                        onPlayClick = { onSelectGame(game.id) }
                    )
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun GameCardItem(
    game: Game,
    viewModel: MainViewModel,
    onPlayClick: () -> Unit
) {
    val completedCount by viewModel.getLevelsForGame(game.id).collectAsState(initial = emptyList())
    val cleared = completedCount.count { it.isCompleted }
    val currentLevel = (cleared + 1).coerceAtMost(Constants.TOTAL_LEVELS_PER_GAME)
    val progress = cleared.toFloat() / Constants.TOTAL_LEVELS_PER_GAME

    val (gradient, accentColor, glowBorder) = when (game.id) {
        Constants.GAME_ARROW_FLOW -> Triple(
            Brush.linearGradient(listOf(Color(0xFF0052D4), Color(0xFF4364F7))),
            ElectricBlue,
            GlassBorderCyan
        )
        Constants.GAME_BLOCK_MERGE -> Triple(
            Brush.linearGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC))),
            NeonPurple,
            GlassBorderPurple
        )
        Constants.GAME_COLOR_PATH -> Triple(
            Brush.linearGradient(listOf(Color(0xFF11998E), Color(0xFF38EF7D))),
            NeonEmerald,
            GlassBorderCyan
        )
        else -> Triple(
            Brush.linearGradient(listOf(Color(0xFFFF0844), Color(0xFFFFB199))),
            NeonPink,
            Color(0x40FF2A85)
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = glowBorder,
        backgroundColor = Color(0xFF131A2C),
        cornerRadius = 20.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = game.name.uppercase(),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = game.description,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$cleared / ${Constants.TOTAL_LEVELS_PER_GAME} Completed",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Current: Level $currentLevel",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accentColor,
                trackColor = Color(0xFF1F2B45)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = RewardGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "₹$cleared Earned",
                        color = RewardGoldLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Neon3DButton(
                    text = "PLAY",
                    onClick = onPlayClick,
                    gradient = gradient
                )
            }
        }
    }
}
