package com.example.games.colorpath

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lightbulb
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun ColorPathGameScreen(
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
        mutableStateOf(ColorPathGenerator.generateLevel(levelNumber))
    }

    var activeColorIndex by remember { mutableStateOf<Int?>(null) }
    var elapsedSeconds by remember(levelNumber) { mutableIntStateOf(0) }
    var movesCount by remember(levelNumber) { mutableIntStateOf(0) }

    var isWon by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(isWon) {
        if (!isWon) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    fun onCellTouch(r: Int, c: Int) {
        if (isWon || r < 0 || r >= gameState.gridSize || c < 0 || c >= gameState.gridSize) return
        val pt = Pair(r, c)
        if (gameState.obstacles.contains(pt)) return

        // Check if touched on a dot endpoint
        val touchedDot = gameState.dots.find {
            (it.startRow == r && it.startCol == c) || (it.endRow == r && it.endCol == c)
        }

        if (touchedDot != null) {
            activeColorIndex = touchedDot.colorIndex
            soundHaptic.playClick()
            val newPaths = gameState.paths.toMutableMap()
            newPaths[touchedDot.colorIndex] = listOf(pt)
            gameState = gameState.copy(paths = newPaths)
            movesCount++
            return
        }

        // Continuing an active path
        val activeIdx = activeColorIndex ?: return
        val currentPath = gameState.paths[activeIdx]?.toMutableList() ?: mutableListOf()
        if (currentPath.isEmpty()) return

        val last = currentPath.last()
        val dist = abs(last.first - r) + abs(last.second - c)

        if (dist == 1) {
            // Adjacent step
            // If stepping back onto previous cell, pop last
            if (currentPath.size > 1 && currentPath[currentPath.size - 2] == pt) {
                currentPath.removeAt(currentPath.size - 1)
            } else if (!currentPath.contains(pt)) {
                // If this cell was used by another color, clear that color's path
                val newPaths = gameState.paths.toMutableMap()
                for ((idx, p) in gameState.paths) {
                    if (idx != activeIdx && p.contains(pt)) {
                        newPaths.remove(idx)
                    }
                }
                currentPath.add(pt)
                newPaths[activeIdx] = currentPath

                val updatedState = gameState.copy(paths = newPaths)
                gameState = updatedState

                // Check if reached destination
                val dot = gameState.dots.find { it.colorIndex == activeIdx }
                if (dot != null) {
                    val destStart = Pair(dot.startRow, dot.startCol)
                    val destEnd = Pair(dot.endRow, dot.endCol)
                    if ((pt == destStart || pt == destEnd) && currentPath.size >= 2) {
                        soundHaptic.playMoveSuccess()
                        activeColorIndex = null // Completed this line!
                    }
                }

                // Check total win
                if (ColorPathGenerator.checkLevelCompletion(updatedState)) {
                    soundHaptic.playWin()
                    isWon = true
                }
            }
        }
    }

    // Hint function: automatically connects an unconnected pair
    fun provideHint() {
        for (dot in gameState.dots) {
            val path = gameState.paths[dot.colorIndex]
            if (path == null || !ColorPathGenerator.isPairConnected(dot, path)) {
                // Generate a path connecting start and end using simple Manhattan path
                val hintPath = mutableListOf<Pair<Int, Int>>()
                var currR = dot.startRow
                var currC = dot.startCol
                hintPath.add(Pair(currR, currC))

                while (currR != dot.endRow) {
                    currR += if (dot.endRow > currR) 1 else -1
                    hintPath.add(Pair(currR, currC))
                }
                while (currC != dot.endCol) {
                    currC += if (dot.endCol > currC) 1 else -1
                    hintPath.add(Pair(currR, currC))
                }

                val newPaths = gameState.paths.toMutableMap()
                newPaths[dot.colorIndex] = hintPath
                val updatedState = gameState.copy(paths = newPaths)
                gameState = updatedState
                soundHaptic.playMoveSuccess()

                if (ColorPathGenerator.checkLevelCompletion(updatedState)) {
                    soundHaptic.playWin()
                    isWon = true
                }
                break
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
                        text = "COLOR PATH",
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
                    gameState = ColorPathGenerator.generateLevel(levelNumber)
                    activeColorIndex = null
                    movesCount = 0
                    elapsedSeconds = 0
                    isWon = false
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
                        Text("Connected", color = TextSecondary, fontSize = 11.sp)
                        val connectedCount = gameState.dots.count {
                            val p = gameState.paths[it.colorIndex]
                            p != null && ColorPathGenerator.isPairConnected(it, p)
                        }
                        Text(
                            text = "$connectedCount / ${gameState.dots.size}",
                            color = NeonEmerald,
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
                        Text("Time", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "${elapsedSeconds}s",
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

            Spacer(modifier = Modifier.height(8.dp))

            // Hint button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF162035))
                        .clickable { provideHint() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = RewardGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-Connect Pair",
                            color = RewardGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Board Canvas & Touch Area
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
                        .background(Color(0xFF0F172A))
                        .border(2.dp, Color(0xFF243452), RoundedCornerShape(20.dp))
                        .padding(8.dp)
                ) {
                    val sizePx = constraints.maxWidth.toFloat()
                    val cellSizePx = sizePx / gameState.gridSize
                    val cellDp = maxWidth / gameState.gridSize

                    // Background Grid Cells & Obstacles
                    for (r in 0 until gameState.gridSize) {
                        for (c in 0 until gameState.gridSize) {
                            val isObstacle = gameState.obstacles.contains(Pair(r, c))
                            val xOffset = cellDp * c
                            val yOffset = cellDp * r
                            val cellWidth = cellDp

                            Box(
                                modifier = Modifier
                                    .size(cellWidth)
                                    .offset(x = xOffset, y = yOffset)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isObstacle) Color(0xFF1E2638) else Color(0xFF141D30))
                                    .border(0.5.dp, Color(0x1AFFFFFF), RoundedCornerShape(8.dp))
                                    .clickable { onCellTouch(r, c) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isObstacle) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = "Obstacle",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Canvas to draw smooth glowing paths
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(gameState.gridSize) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val c = (offset.x / cellSizePx).toInt().coerceIn(0, gameState.gridSize - 1)
                                        val r = (offset.y / cellSizePx).toInt().coerceIn(0, gameState.gridSize - 1)
                                        onCellTouch(r, c)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val c = (change.position.x / cellSizePx).toInt().coerceIn(0, gameState.gridSize - 1)
                                        val r = (change.position.y / cellSizePx).toInt().coerceIn(0, gameState.gridSize - 1)
                                        onCellTouch(r, c)
                                    },
                                    onDragEnd = {
                                        activeColorIndex = null
                                    }
                                )
                            }
                    ) {
                        // Draw paths
                        for ((idx, path) in gameState.paths) {
                            val dot = gameState.dots.find { it.colorIndex == idx } ?: continue
                            if (path.size >= 2) {
                                for (i in 0 until path.size - 1) {
                                    val (r1, c1) = path[i]
                                    val (r2, c2) = path[i + 1]

                                    val start = Offset(
                                        (c1 + 0.5f) * cellSizePx,
                                        (r1 + 0.5f) * cellSizePx
                                    )
                                    val end = Offset(
                                        (c2 + 0.5f) * cellSizePx,
                                        (r2 + 0.5f) * cellSizePx
                                    )

                                    // Outer glow
                                    drawLine(
                                        color = dot.color.copy(alpha = 0.35f),
                                        start = start,
                                        end = end,
                                        strokeWidth = cellSizePx * 0.45f,
                                        cap = StrokeCap.Round
                                    )
                                    // Solid inner path
                                    drawLine(
                                        color = dot.color,
                                        start = start,
                                        end = end,
                                        strokeWidth = cellSizePx * 0.22f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }

                        // Draw Endpoint Dots
                        for (dot in gameState.dots) {
                            val startCenter = Offset(
                                (dot.startCol + 0.5f) * cellSizePx,
                                (dot.startRow + 0.5f) * cellSizePx
                            )
                            val endCenter = Offset(
                                (dot.endCol + 0.5f) * cellSizePx,
                                (dot.endRow + 0.5f) * cellSizePx
                            )

                            // Start Dot
                            drawCircle(
                                color = dot.color.copy(alpha = 0.3f),
                                radius = cellSizePx * 0.38f,
                                center = startCenter
                            )
                            drawCircle(
                                color = dot.color,
                                radius = cellSizePx * 0.26f,
                                center = startCenter
                            )
                            drawCircle(
                                color = Color.White,
                                radius = cellSizePx * 0.10f,
                                center = startCenter
                            )

                            // End Dot
                            drawCircle(
                                color = dot.color.copy(alpha = 0.3f),
                                radius = cellSizePx * 0.38f,
                                center = endCenter
                            )
                            drawCircle(
                                color = dot.color,
                                radius = cellSizePx * 0.26f,
                                center = endCenter
                            )
                            drawCircle(
                                color = Color.White,
                                radius = cellSizePx * 0.10f,
                                center = endCenter
                            )
                        }
                    }
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))

        // Win modal
        if (isWon) {
            LevelCompleteDialog(
                gameTitle = "Color Path",
                levelNumber = levelNumber,
                rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
                isRewardClaimed = isRewardClaimed,
                movesUsed = movesCount,
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
                                repository.completeLevelAndClaimReward(
                                    gameId = Constants.GAME_COLOR_PATH,
                                    levelNumber = levelNumber,
                                    movesUsed = movesCount,
                                    timeSeconds = elapsedSeconds,
                                    score = 1000 - elapsedSeconds * 5,
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
