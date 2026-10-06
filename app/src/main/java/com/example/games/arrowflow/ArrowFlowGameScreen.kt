package com.example.games.arrowflow

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
fun ArrowFlowGameScreen(
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
        mutableStateOf(ArrowFlowLevelGenerator.generateLevel(levelNumber))
    }

    var movesUsed by remember(levelNumber) { mutableIntStateOf(0) }
    var elapsedSeconds by remember(levelNumber) { mutableIntStateOf(0) }
    var shakingArrowId by remember { mutableStateOf<Int?>(null) }
    var isWinModalVisible by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }
    var showGameOverDialog by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(gameState.isCompleted, showGameOverDialog) {
        if (!gameState.isCompleted && !showGameOverDialog) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Function to check if arrow can fly unobstructed
    fun canArrowEscape(arrow: ArrowCell, cells: List<ArrowCell>, size: Int): Boolean {
        val occupied = cells.filter { !it.isFlying && it.id != arrow.id }

        return when (arrow.direction) {
            ArrowDirection.UP -> {
                occupied.none { it.col == arrow.col && it.row < arrow.row }
            }
            ArrowDirection.DOWN -> {
                occupied.none { it.col == arrow.col && it.row > arrow.row }
            }
            ArrowDirection.LEFT -> {
                occupied.none { it.row == arrow.row && it.col < arrow.col }
            }
            ArrowDirection.RIGHT -> {
                occupied.none { it.row == arrow.row && it.col > arrow.col }
            }
        }
    }

    // Handle arrow tap
    fun onArrowClicked(arrow: ArrowCell) {
        if (gameState.isCompleted || showGameOverDialog || arrow.isObstacle || arrow.isFlying) return

        val canEscape = canArrowEscape(arrow, gameState.cells, gameState.gridSize)

        if (canEscape) {
            soundHaptic.playMoveSuccess()
            movesUsed++
            val updatedCells = gameState.cells.map {
                if (it.id == arrow.id) it.copy(isFlying = true) else it
            }

            val remainingCount = updatedCells.count { !it.isObstacle && !it.isFlying }
            val isWin = (remainingCount == 0)

            gameState = gameState.copy(
                cells = updatedCells,
                remainingMoves = gameState.remainingMoves - 1,
                isCompleted = isWin
            )

            if (isWin) {
                soundHaptic.playWin()
                isWinModalVisible = true
            } else if (gameState.remainingMoves <= 1 && !isWin) {
                // Out of moves
                soundHaptic.playWrongMove()
                showGameOverDialog = true
            }
        } else {
            // Wrong move feedback
            soundHaptic.playWrongMove()
            shakingArrowId = arrow.id
            coroutineScope.launch {
                delay(350L)
                if (shakingArrowId == arrow.id) shakingArrowId = null
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
                .padding(bottom = 70.dp) // space for banner
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
                        text = "ARROW FLOW",
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

                IconButton(onClick = {
                    gameState = ArrowFlowLevelGenerator.generateLevel(levelNumber)
                    movesUsed = 0
                    elapsedSeconds = 0
                    showGameOverDialog = false
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
                        Text("Moves Left", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${gameState.remainingMoves}",
                            color = if (gameState.remainingMoves <= 3) NeonCoral else NeonEmerald,
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
                        Text("Remaining", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${gameState.remainingArrowCount}",
                            color = NeonPurple,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Board
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF0D1424))
                        .border(2.dp, Color(0xFF1E2B45), RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    val cellSize = maxWidth / gameState.gridSize

                    for (r in 0 until gameState.gridSize) {
                        for (c in 0 until gameState.gridSize) {
                            val cellItem = gameState.cells.find { it.row == r && it.col == c }

                            val xOffset = cellSize * c
                            val yOffset = cellSize * r

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .offset(x = xOffset, y = yOffset)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF141D33)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cellItem != null && !cellItem.isFlying) {
                                    if (cellItem.isObstacle) {
                                        // Obstacle / Blocker
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color(0xFF374151), Color(0xFF1F2937))
                                                    )
                                                )
                                                .border(1.dp, Color(0xFF6B7280), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "Obstacle",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else {
                                        // Arrow
                                        val isShaking = (shakingArrowId == cellItem.id)
                                        val rotation = when (cellItem.direction) {
                                            ArrowDirection.UP -> 0f
                                            ArrowDirection.RIGHT -> 90f
                                            ArrowDirection.DOWN -> 180f
                                            ArrowDirection.LEFT -> 270f
                                        }

                                        val arrowBg = if (isShaking) {
                                            Brush.verticalGradient(listOf(NeonCoral, Color(0xFF991B1B)))
                                        } else {
                                            Brush.verticalGradient(listOf(ElectricBlue, Color(0xFF0077B6)))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(arrowBg)
                                                .border(
                                                    1.dp,
                                                    if (isShaking) NeonCoral else Color(0x66FFFFFF),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onArrowClicked(cellItem) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Navigation,
                                                contentDescription = "Arrow ${cellItem.direction}",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .rotate(rotation)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // Game Over Dialog (moves exhausted)
        if (showGameOverDialog) {
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
                            text = "OUT OF MOVES!",
                            color = NeonCoral,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You ran out of moves. Clear arrows with a clear path to the edge!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Neon3DButton(
                            text = "Try Again",
                            onClick = {
                                gameState = ArrowFlowLevelGenerator.generateLevel(levelNumber)
                                movesUsed = 0
                                elapsedSeconds = 0
                                showGameOverDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Exit to Levels",
                            color = TextSecondary,
                            modifier = Modifier
                                .clickable(onClick = onBack)
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Win Dialog
        if (isWinModalVisible) {
            LevelCompleteDialog(
                gameTitle = "Arrow Flow",
                levelNumber = levelNumber,
                rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
                isRewardClaimed = isRewardClaimed,
                movesUsed = movesUsed,
                timeSeconds = elapsedSeconds,
                isClaimingReward = isClaimingReward,
                onClaimReward = {
                    if (isClaimingReward || isRewardClaimed) return@LevelCompleteDialog
                    isClaimingReward = true

                    val activity = context as? android.app.Activity
                    adMobManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            coroutineScope.launch {
                                val result = repository.completeLevelAndClaimReward(
                                    gameId = Constants.GAME_ARROW_FLOW,
                                    levelNumber = levelNumber,
                                    movesUsed = movesUsed,
                                    timeSeconds = elapsedSeconds,
                                    score = 1000 - movesUsed * 10,
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
                    isWinModalVisible = false
                    if (levelNumber < Constants.TOTAL_LEVELS_PER_GAME) {
                        onNextLevel(levelNumber + 1)
                    } else {
                        onBack()
                    }
                },
                onReturnToMap = {
                    isWinModalVisible = false
                    onBack()
                }
            )
        }
    }
}
