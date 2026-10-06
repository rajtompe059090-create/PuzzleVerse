package com.example.games.blockmerge

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

@Composable
fun BlockMergeGameScreen(
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

    val targetValue = remember(levelNumber) { BlockMergeHelper.getTargetForLevel(levelNumber) }
    val maxMoves = remember(levelNumber) { BlockMergeHelper.getMaxMovesForLevel(levelNumber) }

    var grid by remember(levelNumber) {
        val initialGrid = Array(4) { IntArray(4) { 0 } }
        val rng = Random(levelNumber * 101)
        val s1 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
        if (s1 != null) initialGrid[s1.first][s1.second] = 2
        val s2 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
        if (s2 != null) initialGrid[s2.first][s2.second] = if (levelNumber > 10) 4 else 2
        mutableStateOf(initialGrid)
    }

    var score by remember(levelNumber) { mutableIntStateOf(0) }
    var movesLeft by remember(levelNumber) { mutableIntStateOf(maxMoves) }
    var elapsedSeconds by remember(levelNumber) { mutableIntStateOf(0) }
    var hintDirection by remember { mutableStateOf<SlideDirection?>(null) }

    var isWon by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }

    // Timer
    LaunchedEffect(isWon, isGameOver) {
        if (!isWon && !isGameOver) {
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    fun makeSlide(dir: SlideDirection) {
        if (isWon || isGameOver) return
        val result = BlockMergeHelper.slide(grid, dir)
        if (result.moved) {
            soundHaptic.playMoveSuccess()
            score += result.scoreGained
            movesLeft--
            hintDirection = null

            // Spawn next tile
            val rng = Random(System.currentTimeMillis())
            val spawn = BlockMergeHelper.spawnNewTile(result.newGrid, rng)
            if (spawn != null) {
                result.newGrid[spawn.first][spawn.second] = if (rng.nextFloat() < 0.2f && levelNumber > 5) 4 else 2
            }
            grid = result.newGrid

            // Check Win Condition: Any tile reached target!
            var hasTarget = false
            for (r in 0 until 4) {
                for (c in 0 until 4) {
                    if (grid[r][c] >= targetValue) {
                        hasTarget = true
                        break
                    }
                }
            }

            if (hasTarget) {
                soundHaptic.playWin()
                isWon = true
            } else if (movesLeft <= 0 || !BlockMergeHelper.canMakeAnyMove(grid)) {
                soundHaptic.playWrongMove()
                isGameOver = true
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
                        text = "BLOCK MERGE",
                        color = NeonPurple,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Level $levelNumber • Target $targetValue",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    val initialGrid = Array(4) { IntArray(4) { 0 } }
                    val rng = Random(System.currentTimeMillis())
                    val s1 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                    if (s1 != null) initialGrid[s1.first][s1.second] = 2
                    val s2 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                    if (s2 != null) initialGrid[s2.first][s2.second] = 2
                    grid = initialGrid
                    score = 0
                    movesLeft = maxMoves
                    elapsedSeconds = 0
                    isWon = false
                    isGameOver = false
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
                            text = "$movesLeft",
                            color = if (movesLeft <= 5) NeonCoral else NeonEmerald,
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
                        Text("Target Block", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "$targetValue",
                            color = NeonPink,
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
                        Text("Score", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = "$score",
                            color = RewardGoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hint bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF162035))
                        .clickable {
                            hintDirection = BlockMergeHelper.getBestHint(grid)
                            soundHaptic.playClick()
                        }
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
                            text = if (hintDirection != null) "Swipe $hintDirection!" else "Show Hint",
                            color = if (hintDirection != null) RewardGold else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Board (Swipe gesture support)
            var totalDragX by remember { mutableStateOf(0f) }
            var totalDragY by remember { mutableStateOf(0f) }

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
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    totalDragX = 0f
                                    totalDragY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                },
                                onDragEnd = {
                                    val threshold = 40f
                                    if (abs(totalDragX) > abs(totalDragY)) {
                                        if (totalDragX > threshold) makeSlide(SlideDirection.RIGHT)
                                        else if (totalDragX < -threshold) makeSlide(SlideDirection.LEFT)
                                    } else {
                                        if (totalDragY > threshold) makeSlide(SlideDirection.DOWN)
                                        else if (totalDragY < -threshold) makeSlide(SlideDirection.UP)
                                    }
                                }
                            )
                        }
                ) {
                    val cellSize = maxWidth / 4

                    for (r in 0 until 4) {
                        for (c in 0 until 4) {
                            val value = grid[r][c]
                            val xOffset = cellSize * c
                            val yOffset = cellSize * r

                            Box(
                                modifier = Modifier
                                    .size(cellSize)
                                    .offset(x = xOffset, y = yOffset)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(getBlockColor(value))
                                    .border(
                                        1.dp,
                                        if (value >= targetValue) RewardGold else Color(0x22FFFFFF),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (value > 0) {
                                    Text(
                                        text = "$value",
                                        color = if (value <= 4) Color.White else Color(0xFFFFFFFF),
                                        fontSize = if (value >= 1024) 18.sp else 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Directional On-Screen Controls (for easy touch access)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = { makeSlide(SlideDirection.LEFT) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkBgCard)
                        .border(1.dp, Color(0xFF2E3D5C), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Slide Left",
                        tint = ElectricBlue
                    )
                }
                IconButton(
                    onClick = { makeSlide(SlideDirection.UP) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkBgCard)
                        .border(1.dp, Color(0xFF2E3D5C), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Slide Up",
                        tint = NeonPurple
                    )
                }
                IconButton(
                    onClick = { makeSlide(SlideDirection.DOWN) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkBgCard)
                        .border(1.dp, Color(0xFF2E3D5C), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Slide Down",
                        tint = NeonPink
                    )
                }
                IconButton(
                    onClick = { makeSlide(SlideDirection.RIGHT) },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(DarkBgCard)
                        .border(1.dp, Color(0xFF2E3D5C), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Slide Right",
                        tint = ElectricBlue
                    )
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))

        // Game Over modal
        if (isGameOver) {
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
                            text = "NO MORE MOVES!",
                            color = NeonCoral,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "You couldn't reach $targetValue. Try again with a new strategy!",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Neon3DButton(
                            text = "Try Again",
                            onClick = {
                                val initialGrid = Array(4) { IntArray(4) { 0 } }
                                val rng = Random(System.currentTimeMillis())
                                val s1 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                                if (s1 != null) initialGrid[s1.first][s1.second] = 2
                                val s2 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                                if (s2 != null) initialGrid[s2.first][s2.second] = 2
                                grid = initialGrid
                                score = 0
                                movesLeft = maxMoves
                                isWon = false
                                isGameOver = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Win modal
        if (isWon) {
            LevelCompleteDialog(
                gameTitle = "Block Merge",
                levelNumber = levelNumber,
                rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
                isRewardClaimed = isRewardClaimed,
                movesUsed = maxMoves - movesLeft,
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
                                    gameId = Constants.GAME_BLOCK_MERGE,
                                    levelNumber = levelNumber,
                                    movesUsed = maxMoves - movesLeft,
                                    timeSeconds = elapsedSeconds,
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

private fun getBlockColor(value: Int): Color {
    return when (value) {
        0 -> Color(0xFF131C2E)
        2 -> Color(0xFF1E293B)
        4 -> Color(0xFF0284C7)
        8 -> Color(0xFF0284C7)
        16 -> Color(0xFF7C3AED)
        32 -> Color(0xFF9333EA)
        64 -> Color(0xFFDB2777)
        128 -> Color(0xFFE11D48)
        256 -> Color(0xFFD97706)
        512 -> Color(0xFFEA580C)
        1024 -> Color(0xFF10B981)
        2048 -> Color(0xFFFFB800)
        else -> Color(0xFFFF2A85)
    }
}
