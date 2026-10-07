package com.example.games.blockmerge

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.scale
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
    val activity = context as? Activity
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
    var hintsUsed by remember(levelNumber) { mutableIntStateOf(0) }
    var hintDirection by remember { mutableStateOf<SlideDirection?>(null) }

    var isWon by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isRewardClaimed by remember { mutableStateOf(false) }
    var isClaimingReward by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "HintArrowPulse")
    val hintPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HintScale"
    )

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
            if (result.scoreGained > 0) {
                soundHaptic.playBlockMerge()
            } else {
                soundHaptic.playBlockSlide()
            }

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

    // Hint Logic: 2 free hints, 3+ requires Interstitial Ad
    fun requestHint() {
        if (isWon || isGameOver) return
        val best = BlockMergeHelper.getBestHint(grid)
        if (best == null) {
            Toast.makeText(context, "No moves possible!", Toast.LENGTH_SHORT).show()
            return
        }

        if (hintsUsed < Constants.FREE_HINTS_PER_LEVEL) {
            hintsUsed++
            hintDirection = best
            soundHaptic.playHint()
            Toast.makeText(context, "Free Hint: Slide $best! (${Constants.FREE_HINTS_PER_LEVEL - hintsUsed} left)", Toast.LENGTH_SHORT).show()
        } else {
            adMobManager.showInterstitial(activity) {
                hintsUsed++
                hintDirection = best
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
                        text = "BLOCK MERGE",
                        color = NeonPurple,
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
                        val initialGrid = Array(4) { IntArray(4) { 0 } }
                        val rng = Random(levelNumber * 101)
                        val s1 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                        if (s1 != null) initialGrid[s1.first][s1.second] = 2
                        val s2 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                        if (s2 != null) initialGrid[s2.first][s2.second] = if (levelNumber > 10) 4 else 2
                        grid = initialGrid
                        score = 0
                        movesLeft = maxMoves
                        elapsedSeconds = 0
                        hintDirection = null
                        isGameOver = false
                        isWon = false
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

            Spacer(modifier = Modifier.height(10.dp))

            // Board (Swipe gesture support + 3D depth)
            var totalDragX by remember { mutableStateOf(0f) }
            var totalDragY by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F1526))
                        .border(2.dp, Color(0xFF1B2640), RoundedCornerShape(24.dp))
                        .padding(12.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    if (abs(totalDragX) > abs(totalDragY)) {
                                        if (totalDragX > 30) makeSlide(SlideDirection.RIGHT)
                                        else if (totalDragX < -30) makeSlide(SlideDirection.LEFT)
                                    } else {
                                        if (totalDragY > 30) makeSlide(SlideDirection.DOWN)
                                        else if (totalDragY < -30) makeSlide(SlideDirection.UP)
                                    }
                                    totalDragX = 0f
                                    totalDragY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDragX += dragAmount.x
                                    totalDragY += dragAmount.y
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (r in 0 until 4) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (c in 0 until 4) {
                                    val value = grid[r][c]
                                    val blockColor = getBlockColor(value)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(4.dp)
                                            .shadow(if (value > 0) 8.dp else 0.dp, RoundedCornerShape(12.dp), spotColor = blockColor)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (value == 0) Brush.verticalGradient(listOf(Color(0xFF151D30), Color(0xFF0E1322)))
                                                else Brush.verticalGradient(listOf(blockColor, blockColor.copy(alpha = 0.75f)))
                                            )
                                            .border(
                                                width = if (value >= targetValue) 2.dp else 1.dp,
                                                color = if (value >= targetValue) RewardGold else Color(0x33FFFFFF),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (value > 0) {
                                            Text(
                                                text = "$value",
                                                color = Color.White,
                                                fontSize = if (value >= 1024) 18.sp else if (value >= 128) 22.sp else 26.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Directional controls for accessible gameplay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { makeSlide(SlideDirection.UP) },
                    modifier = Modifier
                        .size(46.dp)
                        .scale(if (hintDirection == SlideDirection.UP) hintPulseScale else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hintDirection == SlideDirection.UP) Color(0x66FFB800) else Color(0xFF161F33))
                        .border(1.dp, if (hintDirection == SlideDirection.UP) RewardGold else Color(0xFF263554), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = if (hintDirection == SlideDirection.UP) RewardGold else NeonPurple)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(0.55f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { makeSlide(SlideDirection.LEFT) },
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (hintDirection == SlideDirection.LEFT) hintPulseScale else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hintDirection == SlideDirection.LEFT) Color(0x66FFB800) else Color(0xFF161F33))
                            .border(1.dp, if (hintDirection == SlideDirection.LEFT) RewardGold else Color(0xFF263554), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = if (hintDirection == SlideDirection.LEFT) RewardGold else NeonPurple)
                    }

                    IconButton(
                        onClick = { makeSlide(SlideDirection.RIGHT) },
                        modifier = Modifier
                            .size(46.dp)
                            .scale(if (hintDirection == SlideDirection.RIGHT) hintPulseScale else 1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (hintDirection == SlideDirection.RIGHT) Color(0x66FFB800) else Color(0xFF161F33))
                            .border(1.dp, if (hintDirection == SlideDirection.RIGHT) RewardGold else Color(0xFF263554), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = if (hintDirection == SlideDirection.RIGHT) RewardGold else NeonPurple)
                    }
                }

                IconButton(
                    onClick = { makeSlide(SlideDirection.DOWN) },
                    modifier = Modifier
                        .size(46.dp)
                        .scale(if (hintDirection == SlideDirection.DOWN) hintPulseScale else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (hintDirection == SlideDirection.DOWN) Color(0x66FFB800) else Color(0xFF161F33))
                        .border(1.dp, if (hintDirection == SlideDirection.DOWN) RewardGold else Color(0xFF263554), RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = if (hintDirection == SlideDirection.DOWN) RewardGold else NeonPurple)
                }
            }
        }

        // Bottom Banner Ad
        BannerAdCard(modifier = Modifier.align(Alignment.BottomCenter))

        // Game over modal
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
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NO MOVES LEFT!", color = NeonCoral, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You reached Score: $score without reaching $targetValue", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Neon3DButton(
                            text = "Try Again ↺",
                            onClick = {
                                val initialGrid = Array(4) { IntArray(4) { 0 } }
                                val rng = Random(levelNumber * 101)
                                val s1 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                                if (s1 != null) initialGrid[s1.first][s1.second] = 2
                                val s2 = BlockMergeHelper.spawnNewTile(initialGrid, rng)
                                if (s2 != null) initialGrid[s2.first][s2.second] = if (levelNumber > 10) 4 else 2
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
                onClaimNormalReward = {
                    if (isClaimingReward || isRewardClaimed) return@LevelCompleteDialog
                    isClaimingReward = true
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
                                    gameId = Constants.GAME_BLOCK_MERGE,
                                    levelNumber = levelNumber,
                                    movesUsed = maxMoves - movesLeft,
                                    timeSeconds = elapsedSeconds,
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
