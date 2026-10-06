package com.example.games.tilematch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PuzzleApplication
import com.example.ads.BannerAdCard
import com.example.core.Constants
import com.example.games.common.LevelCompleteDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.Neon3DButton
import com.example.ui.theme.DarkBgCard
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TileMatchGameScreen(
    levelNumber: Int,
    onBack: () -> Unit,
    onNextLevel: (Int) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as PuzzleApplication
    val soundHaptic = app.soundHapticManager
    val repository = app.repository
    val adMobManager = app.adMobManager
    val coroutineScope = rememberCoroutineScope()

    var gameState by remember(levelNumber) {
        mutableStateOf(TileMatchHelper.generateLevel(levelNumber))
    }

    var score by remember(levelNumber) { mutableIntStateOf(0) }
    var movesCount by remember(levelNumber) { mutableIntStateOf(0) }
    var remainingTimeSeconds by remember(levelNumber) {
        mutableIntStateOf(gameState.timerSeconds)
    }

    var isWon by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }

    // Countdown timer for timed levels
    LaunchedEffect(gameState.timerSeconds, isWon, isFailed) {
        if (gameState.timerSeconds > 0 && !isWon && !isFailed) {
            while (remainingTimeSeconds > 0) {
                delay(1000L)
                remainingTimeSeconds--
            }
            if (remainingTimeSeconds <= 0 && !isWon) {
                soundHaptic.playWrongMove()
                isFailed = true
            }
        }
    }

    fun onTileClicked(tile: GameTile) {
        if (isWon || isFailed || tile.isRemoved || gameState.dock.size >= 7) return

        soundHaptic.playClick()
        movesCount++

        // Remove tile from board and add to dock
        val updatedTiles = gameState.tiles.map {
            if (it.id == tile.id) it.copy(isRemoved = true) else it
        }

        // Insert into dock, grouping identical types together
        val updatedDock = gameState.dock.toMutableList()
        val insertIndex = updatedDock.indexOfLast { it.type.id == tile.type.id }.let {
            if (it >= 0) it + 1 else updatedDock.size
        }
        updatedDock.add(insertIndex, tile)

        // Check for 3 matching tiles in dock
        val matchCount = updatedDock.count { it.type.id == tile.type.id }
        if (matchCount == 3) {
            soundHaptic.playMoveSuccess()
            score += 150
            updatedDock.removeAll { it.type.id == tile.type.id }
        }

        val remainingOnBoard = updatedTiles.count { !it.isRemoved }
        val won = (remainingOnBoard == 0 && updatedDock.isEmpty())
        val failed = (!won && updatedDock.size >= 7)

        gameState = gameState.copy(
            tiles = updatedTiles,
            dock = updatedDock,
            isCompleted = won,
            isFailed = failed
        )

        if (won) {
            soundHaptic.playWin()
            isWon = true
        } else if (failed) {
            soundHaptic.playWrongMove()
            isFailed = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TILE MATCH",
                        color = NeonPink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Level $levelNumber",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    gameState = TileMatchHelper.generateLevel(levelNumber)
                    score = 0
                    movesCount = 0
                    remainingTimeSeconds = gameState.timerSeconds
                    isWon = false
                    isFailed = false
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = TextPrimary
                    )
                }
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Remaining", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${gameState.remainingTilesOnBoard}",
                            color = ElectricBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (gameState.timerSeconds > 0) "Time Left" else "Score",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (gameState.timerSeconds > 0) "${remainingTimeSeconds}s" else "$score",
                            color = if (remainingTimeSeconds in 1..15) NeonCoral else NeonPurple,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                GlassCard(
                    modifier = Modifier.weight(1f),
                    cornerRadius = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Reward", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "₹${Constants.LEVEL_REWARD_AMOUNT.toInt()}",
                            color = RewardGoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Board Tiles
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val visibleTiles = gameState.tiles.filter { !it.isRemoved }

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF243452), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    items(visibleTiles, key = { it.id }) { tile ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .shadow(6.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                    )
                                )
                                .border(1.5.dp, tile.type.color.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .clickable { onTileClicked(tile) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tile.type.icon,
                                contentDescription = tile.type.name,
                                tint = tile.type.color,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dock Container (Holds up to 7 tiles)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "MATCHING TRAY (${gameState.dock.size}/7)",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0B111E))
                        .border(
                            1.5.dp,
                            if (gameState.dock.size >= 6) NeonCoral else Color(0xFF1E2E4A),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(7) { index ->
                            val dockTile = gameState.dock.getOrNull(index)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (dockTile != null) Color(0xFF19243C) else Color(0xFF101726)
                                    )
                                    .border(
                                        1.dp,
                                        dockTile?.type?.color ?: Color(0xFF1E2B40),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dockTile != null) {
                                    Icon(
                                        imageVector = dockTile.type.icon,
                                        contentDescription = dockTile.type.name,
                                        tint = dockTile.type.color,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))

        // Failed Dialog
        if (isFailed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .padding(20.dp),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (remainingTimeSeconds <= 0 && gameState.timerSeconds > 0) "TIME'S UP!" else "TRAY FULL!",
                            color = NeonCoral,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (remainingTimeSeconds <= 0 && gameState.timerSeconds > 0)
                                "You ran out of time! Try again."
                            else
                                "Your tray filled with 7 tiles without a 3-match! Try again.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Neon3DButton(
                            text = "Try Again",
                            onClick = {
                                gameState = TileMatchHelper.generateLevel(levelNumber)
                                score = 0
                                movesCount = 0
                                remainingTimeSeconds = gameState.timerSeconds
                                isWon = false
                                isFailed = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Win Dialog
        if (isWon) {
            LevelCompleteDialog(
                gameTitle = "Tile Match",
                levelNumber = levelNumber,
                rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
                isRewardClaimed = isRewardClaimed,
                movesUsed = movesCount,
                timeSeconds = gameState.timerSeconds - remainingTimeSeconds,
                isClaimingReward = isClaimingReward,
                onClaimReward = {
                    if (isClaimingReward || isRewardClaimed) return@LevelCompleteDialog
                    isClaimingReward = true

                    val activity = context as? android.app.Activity
                    adMobManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            coroutineScope.launch {
                                repository.completeLevelAndClaimReward(
                                    gameId = Constants.GAME_TILE_MATCH,
                                    levelNumber = levelNumber,
                                    movesUsed = movesCount,
                                    timeSeconds = gameState.timerSeconds - remainingTimeSeconds,
                                    score = score,
                                    stars = 3,
                                    adWatched = true
                                )
                                isRewardClaimed = true
                                isClaimingReward = false
                            }
                        },
                        onAdClosed = {
                            isClaimingReward = false
                        },
                        onAdUnavailable = {
                            isClaimingReward = false
                            android.widget.Toast.makeText(
                                context,
                                "Rewarded ad is not ready. Please try again.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                onNextLevel = {
                    isWon = false
                    if (levelNumber < Constants.TOTAL_LEVELS_PER_GAME) {
                        onNextLevel(levelNumber + 1)
                    } else {
                        onBack()
                    }
                },
                onReturnToMap = {
                    isWon = false
                    onBack()
                }
            )
        }
    }
}
