package com.example.games.heartmaze

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun HeartMazeGameScreen(
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
        mutableStateOf(HeartMazeGenerator.generateLevel(levelNumber))
    }

    var elapsedSeconds by remember(levelNumber) { mutableIntStateOf(0) }
    var hintsUsed by remember(levelNumber) { mutableIntStateOf(0) }
    var showHintGlow by remember { mutableStateOf(false) }

    var isWinModalVisible by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }

    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "HeartGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Elapsed timer
    LaunchedEffect(gameState.isCompleted) {
        if (!gameState.isCompleted) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Continuous Frame Loop with withFrameNanos
    LaunchedEffect(gameState.levelNumber, gameState.isMoving, gameState.isCompleted) {
        if (gameState.isMoving && !gameState.isCompleted) {
            var lastTimeNanos = withFrameNanos { it }
            while (isActive && gameState.isMoving && !gameState.isCompleted) {
                withFrameNanos { nowNanos ->
                    val dt = ((nowNanos - lastTimeNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastTimeNanos = nowNanos

                    val prevCollectedCount = gameState.collectedHearts.size
                    val updated = HeartMazeGenerator.updateFrame(gameState, dt)

                    if (updated.collectedHearts.size > prevCollectedCount) {
                        soundHaptic.playMoveSuccess()
                    }

                    if (updated.isCompleted && !gameState.isCompleted) {
                        soundHaptic.playWin()
                        isWinModalVisible = true
                    } else if (updated.isCollided && !gameState.isCollided) {
                        soundHaptic.playWrongMove()
                    }
                    gameState = updated
                }
            }
        }
    }

    // Direction handler
    fun requestDirectionChange(newDir: MazeDirection) {
        if (gameState.isCompleted) return
        val updated = HeartMazeGenerator.changeDirection(gameState, newDir)
        if (updated != gameState) {
            gameState = updated
            soundHaptic.playClick()
        }
    }

    // Hint Logic: 2 free hints, 3+ requires Interstitial Ad
    fun requestHint() {
        if (gameState.isCompleted) return
        if (hintsUsed < Constants.FREE_HINTS_PER_LEVEL) {
            hintsUsed++
            showHintGlow = true
            soundHaptic.playHint()
            Toast.makeText(context, "Free Hint Used! (${Constants.FREE_HINTS_PER_LEVEL - hintsUsed} left)", Toast.LENGTH_SHORT).show()
        } else {
            adMobManager.showInterstitial(activity) {
                hintsUsed++
                showHintGlow = true
                soundHaptic.playHint()
                Toast.makeText(context, "Hint Unlocked via Ad!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgPrimary)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val (dx, dy) = dragAmount
                        if (abs(dx) > abs(dy)) {
                            if (dx > 10) requestDirectionChange(MazeDirection.RIGHT)
                            else if (dx < -10) requestDirectionChange(MazeDirection.LEFT)
                        } else {
                            if (dy > 10) requestDirectionChange(MazeDirection.DOWN)
                            else if (dy < -10) requestDirectionChange(MazeDirection.UP)
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        text = "HEART MAZE",
                        color = NeonPink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Level $levelNumber • ${gameState.difficultyTitle}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(onClick = { requestHint() }) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = if (hintsUsed < Constants.FREE_HINTS_PER_LEVEL) RewardGold else TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        gameState = HeartMazeGenerator.restartLevel(gameState)
                        soundHaptic.playClick()
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
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HEARTS", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${gameState.collectedHearts.size}/${gameState.heartCollectibles.size}",
                            color = NeonPink,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TIME", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val mins = elapsedSeconds / 60
                        val secs = elapsedSeconds % 60
                        Text(
                            String.format("%02d:%02d", mins, secs),
                            color = ElectricBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                GlassCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SPEED", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${gameState.speed.toInt()}x", color = NeonEmerald, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Heart Maze Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0C0A14))
                        .border(2.dp, Brush.linearGradient(listOf(NeonPink, NeonPurple)), RoundedCornerShape(24.dp))
                        .shadow(16.dp, shape = RoundedCornerShape(24.dp), ambientColor = NeonPink)
                ) {
                    val canvasWidth = constraints.maxWidth.toFloat()
                    val canvasHeight = constraints.maxHeight.toFloat()
                    val boardSize = minOf(canvasWidth, canvasHeight)
                    val cellSize = boardSize / gameState.gridSize

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // 1. Draw Heart Area Cells (path vs walls)
                        for (r in 0 until gameState.gridSize) {
                            for (c in 0 until gameState.gridSize) {
                                val coord = Pair(r, c)
                                val x = c * cellSize
                                val y = r * cellSize
                                val isInside = gameState.heartCells.contains(coord)

                                if (isInside) {
                                    val isPath = gameState.pathCells.contains(coord)
                                    val isObstacle = gameState.obstacleCells.contains(coord)

                                    if (isObstacle) {
                                        drawRoundRect(
                                            color = Color(0xFFFF2A6D),
                                            topLeft = Offset(x + cellSize * 0.1f, y + cellSize * 0.1f),
                                            size = Size(cellSize * 0.8f, cellSize * 0.8f),
                                            cornerRadius = CornerRadius(6.dp.toPx())
                                        )
                                    } else if (isPath) {
                                        drawRect(
                                            color = Color(0xFF1B162C),
                                            topLeft = Offset(x, y),
                                            size = Size(cellSize, cellSize)
                                        )
                                    } else {
                                        // Wall
                                        drawRect(
                                            color = Color(0xFF4A154B),
                                            topLeft = Offset(x, y),
                                            size = Size(cellSize, cellSize)
                                        )
                                        drawRect(
                                            color = Color(0xFF2E082F),
                                            topLeft = Offset(x + 2f, y + 2f),
                                            size = Size(cellSize - 4f, cellSize - 4f)
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Hint Path if enabled
                        if (showHintGlow) {
                            for (coord in gameState.solutionPath) {
                                drawCircle(
                                    color = Color(0x50FFD700),
                                    radius = cellSize * 0.22f,
                                    center = Offset((coord.second + 0.5f) * cellSize, (coord.first + 0.5f) * cellSize)
                                )
                            }
                        }

                        // 3. Heart Collectibles
                        for (coord in gameState.heartCollectibles) {
                            if (!gameState.collectedHearts.contains(coord)) {
                                val cx = (coord.second + 0.5f) * cellSize
                                val cy = (coord.first + 0.5f) * cellSize
                                drawCircle(
                                    color = NeonPink,
                                    radius = cellSize * 0.26f * pulseScale,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = cellSize * 0.12f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }

                        // 4. Finish Goal at bottom: Magnifying Glass / Search Portal
                        val fx = (gameState.finishCell.second + 0.5f) * cellSize
                        val fy = (gameState.finishCell.first + 0.5f) * cellSize
                        val isGoalUnlocked = gameState.allCollectiblesGathered

                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    if (isGoalUnlocked) NeonEmerald else Color(0xFF555555),
                                    Color.Transparent
                                )
                            ),
                            radius = cellSize * 0.55f * pulseScale,
                            center = Offset(fx, fy)
                        )
                        // Search ring
                        drawCircle(
                            color = if (isGoalUnlocked) NeonEmerald else Color.Gray,
                            radius = cellSize * 0.28f,
                            center = Offset(fx - cellSize * 0.05f, fy - cellSize * 0.05f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                        // Search handle
                        drawLine(
                            color = if (isGoalUnlocked) NeonEmerald else Color.Gray,
                            start = Offset(fx + cellSize * 0.12f, fy + cellSize * 0.12f),
                            end = Offset(fx + cellSize * 0.28f, fy + cellSize * 0.28f),
                            strokeWidth = 3.5.dp.toPx()
                        )

                        // 5. Continuous Player
                        val px = gameState.playerX * cellSize
                        val py = gameState.playerY * cellSize
                        val pRadius = gameState.playerRadius * cellSize

                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0x8000E5FF), Color.Transparent)),
                            radius = pRadius * 1.8f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = ElectricBlue,
                            radius = pRadius,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = pRadius * 0.4f,
                            center = Offset(px, py)
                        )
                    }
                }
            }

            // Directional On-screen Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { requestDirectionChange(MazeDirection.UP) },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(DarkBgCard)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, "UP", tint = NeonPink)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(40.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { requestDirectionChange(MazeDirection.LEFT) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkBgCard)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "LEFT", tint = NeonPink)
                    }

                    IconButton(
                        onClick = { requestDirectionChange(MazeDirection.DOWN) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkBgCard)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, "DOWN", tint = NeonPink)
                    }

                    IconButton(
                        onClick = { requestDirectionChange(MazeDirection.RIGHT) },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(DarkBgCard)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "RIGHT", tint = NeonPink)
                    }
                }
            }

            // Bottom Banner Ad
            BannerAdCard()
        }

        // Level Complete Dialog with Real Rewarded Ads
        if (isWinModalVisible) {
            LevelCompleteDialog(
                gameTitle = "Heart Maze",
                levelNumber = levelNumber,
                rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
                isRewardClaimed = isRewardClaimed,
                movesUsed = gameState.solutionPath.size,
                timeSeconds = elapsedSeconds,
                isClaimingReward = isClaimingReward,
                onClaimNormalReward = {
                    if (isRewardClaimed || isClaimingReward) return@LevelCompleteDialog
                    isClaimingReward = true

                    adMobManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            coroutineScope.launch {
                                val result = repository.completeLevelAndClaimReward(
                                    gameId = Constants.GAME_HEART_MAZE,
                                    levelNumber = levelNumber,
                                    movesUsed = gameState.solutionPath.size,
                                    timeSeconds = elapsedSeconds,
                                    score = (1000 - elapsedSeconds * 5).coerceAtLeast(100),
                                    stars = 3,
                                    adWatched = true,
                                    isDoubleReward = false
                                )
                                isRewardClaimed = true
                                isClaimingReward = false
                                if (result.rewardGranted) {
                                    soundHaptic.playReward()
                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onAdClosed = {
                            isClaimingReward = false
                        },
                        onAdUnavailable = {
                            isClaimingReward = false
                            Toast.makeText(context, "Ad unavailable. Reward could not be verified.", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onClaimDoubleReward = {
                    if (isRewardClaimed || isClaimingReward) return@LevelCompleteDialog
                    isClaimingReward = true

                    adMobManager.showRewardedAd(
                        activity = activity,
                        onRewardEarned = {
                            coroutineScope.launch {
                                val result = repository.completeLevelAndClaimReward(
                                    gameId = Constants.GAME_HEART_MAZE,
                                    levelNumber = levelNumber,
                                    movesUsed = gameState.solutionPath.size,
                                    timeSeconds = elapsedSeconds,
                                    score = (1000 - elapsedSeconds * 5).coerceAtLeast(100),
                                    stars = 3,
                                    adWatched = true,
                                    isDoubleReward = true
                                )
                                isRewardClaimed = true
                                isClaimingReward = false
                                if (result.rewardGranted) {
                                    soundHaptic.playReward()
                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onAdClosed = {
                            isClaimingReward = false
                        },
                        onAdUnavailable = {
                            isClaimingReward = false
                            Toast.makeText(context, "Ad unavailable. Reward could not be verified.", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onNextLevel = {
                    isWinModalVisible = false
                    onNextLevel(levelNumber + 1)
                },
                onReturnToMap = onBack
            )
        }
    }
}
