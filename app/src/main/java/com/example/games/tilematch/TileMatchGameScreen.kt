package com.example.games.tilematch

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Lightbulb
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
import androidx.compose.ui.draw.scale
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
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonEmerald
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
    val activity = context as? Activity
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
    var hintsUsed by remember(levelNumber) { mutableIntStateOf(0) }
    var hintedTypeId by remember { mutableStateOf<Int?>(null) }

    var isWon by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "TilePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TilePulseScale"
    )

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

        soundHaptic.playTileSelect()
        movesCount++
        if (hintedTypeId == tile.type.id) hintedTypeId = null

        // Remove tile from board and add to dock
        val updatedTiles = gameState.tiles.map {
            if (it.id == tile.id) it.copy(isRemoved = true) else it
        }

        val updatedDock = gameState.dock.toMutableList()
        val insertIndex = updatedDock.indexOfLast { it.type.id == tile.type.id }.let {
            if (it >= 0) it + 1 else updatedDock.size
        }
        updatedDock.add(insertIndex, tile)

        // Check for 3 matching tiles in dock
        val matchCount = updatedDock.count { it.type.id == tile.type.id }
        if (matchCount == 3) {
            soundHaptic.playTileMatch()
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

    // Hint Logic: 2 free hints, 3+ requires Interstitial Ad
    fun requestHint() {
        if (isWon || isFailed) return
        val availableTiles = gameState.tiles.filter { !it.isRemoved }
        // Find a type with at least one matching in dock, or any triplet available
        val inDockType = gameState.dock.firstOrNull()?.type?.id
        val targetType = inDockType ?: availableTiles.groupBy { it.type.id }.maxByOrNull { it.value.size }?.key

        if (targetType == null) {
            Toast.makeText(context, "No moves remaining!", Toast.LENGTH_SHORT).show()
            return
        }

        if (hintsUsed < Constants.FREE_HINTS_PER_LEVEL) {
            hintsUsed++
            hintedTypeId = targetType
            soundHaptic.playHint()
            Toast.makeText(context, "Free Hint: Pick highlighted jewel! (${Constants.FREE_HINTS_PER_LEVEL - hintsUsed} left)", Toast.LENGTH_SHORT).show()
        } else {
            adMobManager.showInterstitial(activity) {
                hintsUsed++
                hintedTypeId = targetType
                soundHaptic.playHint()
                Toast.makeText(context, "Hint Unlocked via Ad!", Toast.LENGTH_SHORT).show()
            }
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
                        color = ElectricBlue,
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

                Row {
                    IconButton(onClick = { requestHint() }) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = if (hintsUsed < Constants.FREE_HINTS_PER_LEVEL) RewardGold else ElectricBlue
                        )
                    }

                    IconButton(onClick = {
                        gameState = TileMatchHelper.generateLevel(levelNumber)
                        score = 0
                        movesCount = 0
                        remainingTimeSeconds = gameState.timerSeconds
                        hintedTypeId = null
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
            }

            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 12.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Remaining", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${gameState.remainingTilesOnBoard}",
                            color = NeonEmerald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 12.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Score", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "$score",
                            color = RewardGoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                GlassCard(modifier = Modifier.weight(1f), cornerRadius = 12.dp) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (gameState.timerSeconds > 0) "Time Left" else "Moves",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (gameState.timerSeconds > 0) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (remainingTimeSeconds <= 15) NeonCoral else NeonPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${remainingTimeSeconds}s",
                                    color = if (remainingTimeSeconds <= 15) NeonCoral else NeonPurple,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    text = "$movesCount",
                                    color = NeonPurple,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Board (Active Jewel Tiles with 3D Bevel)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxSize(),
                    cornerRadius = 24.dp
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(5),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(gameState.tiles) { tile ->
                            if (!tile.isRemoved) {
                                val isHinted = (hintedTypeId == tile.type.id)
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .scale(if (isHinted) pulseScale else 1f)
                                        .shadow(if (isHinted) 10.dp else 4.dp, RoundedCornerShape(12.dp), spotColor = tile.type.color)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    tile.type.color.copy(alpha = 0.85f),
                                                    tile.type.color.copy(alpha = 0.45f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isHinted) 2.dp else 1.dp,
                                            color = if (isHinted) RewardGold else Color(0x66FFFFFF),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onTileClicked(tile) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.type.icon,
                                        contentDescription = tile.type.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x11FFFFFF))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Match Dock Container (Max 7 slots)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (gameState.dock.size >= 6) NeonCoral else ElectricBlue,
                    cornerRadius = 18.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "HOLDING DOCK (${gameState.dock.size}/7 SLOTS)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (slot in 0 until 7) {
                                val tileInSlot = gameState.dock.getOrNull(slot)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (tileInSlot != null) tileInSlot.type.color.copy(alpha = 0.85f)
                                            else Color(0xFF131C2E)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (tileInSlot != null) Color.White else Color(0xFF23304A),
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (tileInSlot != null) {
                                        Icon(
                                            imageVector = tileInSlot.type.icon,
                                            contentDescription = tileInSlot.type.name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
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
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("LEVEL FAILED!", color = NeonCoral, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (remainingTimeSeconds <= 0 && gameState.timerSeconds > 0) "Time ran out!" else "Dock is full with no 3-in-a-row!",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Neon3DButton(
                            text = "Try Again ↺",
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
                onClaimNormalReward = {
                    if (isClaimingReward || isRewardClaimed) return@LevelCompleteDialog
                    isClaimingReward = true
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
                                    adWatched = true,
                                    isDoubleReward = false
                                )
                                soundHaptic.playReward()
                                isRewardClaimed = true
                                isClaimingReward = false
                            }
                        },
                        onAdClosed = { isClaimingReward = false },
                        onAdUnavailable = {
                            isClaimingReward = false
                            Toast.makeText(context, "Rewarded ad is loading. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onClaimDoubleReward = {
                    if (isClaimingReward || isRewardClaimed) return@LevelCompleteDialog
                    isClaimingReward = true
                    adMobManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            coroutineScope.launch {
                                repository.completeLevelAndClaimReward(
                                    gameId = Constants.GAME_TILE_MATCH,
                                    levelNumber = levelNumber,
                                    movesUsed = movesCount,
                                    timeSeconds = gameState.timerSeconds - remainingTimeSeconds,
                                    score = score * 2,
                                    stars = 3,
                                    adWatched = true,
                                    isDoubleReward = true
                                )
                                soundHaptic.playReward()
                                isRewardClaimed = true
                                isClaimingReward = false
                            }
                        },
                        onAdClosed = { isClaimingReward = false },
                        onAdUnavailable = {
                            isClaimingReward = false
                            Toast.makeText(context, "Rewarded ad is loading. Please try again.", Toast.LENGTH_SHORT).show()
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
