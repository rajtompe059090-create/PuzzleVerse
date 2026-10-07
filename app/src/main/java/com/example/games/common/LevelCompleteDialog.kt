package com.example.games.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.ConfettiOverlay
import com.example.ui.components.Neon3DButton
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderGold
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.RewardGold
import com.example.ui.theme.RewardGoldGradient
import com.example.ui.theme.RewardGoldLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LevelCompleteDialog(
    gameTitle: String,
    levelNumber: Int,
    rewardAmount: Double,
    isRewardClaimed: Boolean,
    movesUsed: Int,
    timeSeconds: Int,
    isClaimingReward: Boolean,
    onClaimNormalReward: () -> Unit,
    onClaimDoubleReward: () -> Unit,
    onNextLevel: () -> Unit,
    onReturnToMap: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0C1322))
                .border(2.dp, GlassBorderGold, RoundedCornerShape(24.dp))
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            ConfettiOverlay(modifier = Modifier.fillMaxSize())

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Trophy Icon Glow
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFB800))
                        .border(2.dp, RewardGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Trophy",
                        tint = RewardGold,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "LEVEL COMPLETE!",
                    color = RewardGoldLight,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "🎉 Awesome Victory!",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$gameTitle • Level $levelNumber",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141C2E))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Moves", color = TextSecondary, fontSize = 11.sp)
                        Text("$movesUsed", color = ElectricBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Time", color = TextSecondary, fontSize = 11.sp)
                        Text("${timeSeconds}s", color = NeonPurple, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Stars", color = TextSecondary, fontSize = 11.sp)
                        Row {
                            repeat(3) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = RewardGold,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reward Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF192338))
                        .border(1.dp, if (isRewardClaimed) NeonEmerald else RewardGold, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Reward",
                            tint = RewardGold,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LEVEL REWARDS",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isRewardClaimed) "✓ Reward Claimed" else "Normal ₹${rewardAmount.toInt()} • Double ₹${(rewardAmount * 2).toInt()}",
                                color = if (isRewardClaimed) NeonEmerald else RewardGoldLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: Normal Reward & 2x Double Reward (Both gated by real AdMob Rewarded ad)
                if (!isRewardClaimed) {
                    // 2X Reward (Highlighted Flame button)
                    Neon3DButton(
                        text = if (isClaimingReward) "Loading Ad..." else "🔥 Claim 2× Reward (₹${(rewardAmount * 2).toInt()})",
                        onClick = onClaimDoubleReward,
                        enabled = !isClaimingReward,
                        gradient = Brush.horizontalGradient(listOf(Color(0xFFFF2A85), Color(0xFFFF7A00))),
                        icon = Icons.Default.LocalFireDepartment,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Normal Reward
                    Neon3DButton(
                        text = if (isClaimingReward) "Loading Ad..." else "🎁 Claim ₹${rewardAmount.toInt()}",
                        onClick = onClaimNormalReward,
                        enabled = !isClaimingReward,
                        gradient = RewardGoldGradient,
                        icon = Icons.Default.CardGiftcard,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Neon3DButton(
                    text = "Next Level ▶",
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Back to Level Map",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onReturnToMap)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
