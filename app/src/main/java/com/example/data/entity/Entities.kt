package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "PuzzleMaster",
    val avatarIndex: Int = 0,
    val memberSinceTimestamp: Long = System.currentTimeMillis(),
    val joiningBonusClaimed: Boolean = false,
    val currentStreak: Int = 1,
    val gamesPlayed: Int = 0
)

@Entity(tableName = "games")
data class Game(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val totalLevels: Int = 100
)

@Entity(
    tableName = "game_levels",
    primaryKeys = ["gameId", "levelNumber"],
    indices = [Index(value = ["gameId", "levelNumber"])]
)
data class GameLevel(
    val gameId: String,
    val levelNumber: Int,
    val difficulty: String = "Normal",
    val rewardAmount: Double = 1.0,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val bestScore: Int = 0,
    val bestStars: Int = 0
)

@Entity(
    tableName = "level_progress",
    indices = [Index(value = ["gameId", "levelNumber"])]
)
data class LevelProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: String,
    val levelNumber: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val movesUsed: Int = 0,
    val timeSeconds: Int = 0,
    val stars: Int = 3
)

@Entity(
    tableName = "wallet_transactions",
    indices = [Index(value = ["type"]), Index(value = ["timestamp"])]
)
data class WalletTransaction(
    @PrimaryKey val transactionId: String,
    val type: String, // JOINING_BONUS, LEVEL_REWARD, WITHDRAWAL_REQUEST, REFUND
    val amount: Double,
    val gameId: String? = null,
    val levelNumber: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "COMPLETED", // COMPLETED, PENDING, REJECTED
    val referenceNote: String = ""
)

@Entity(
    tableName = "reward_claims",
    indices = [Index(value = ["gameId", "levelNumber"], unique = true)]
)
data class RewardClaim(
    @PrimaryKey val claimId: String, // "${gameId}_${levelNumber}"
    val gameId: String,
    val levelNumber: Int,
    val rewardAmount: Double = 1.0,
    val adWatched: Boolean = false,
    val claimedAt: Long = System.currentTimeMillis(),
    val transactionId: String
)

@Entity(
    tableName = "withdrawals",
    indices = [Index(value = ["requestTimestamp"])]
)
data class Withdrawal(
    @PrimaryKey val id: String,
    val amount: Double,
    val payoutMethod: String, // UPI, PAYTM, BANK
    val payoutDetails: String,
    val requestTimestamp: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, COMPLETED
    val remarks: String = "Demo withdrawal request created"
)

@Entity(tableName = "ad_rewards")
data class AdReward(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val adUnitId: String,
    val placement: String,
    val timestamp: Long = System.currentTimeMillis(),
    val rewardGranted: Boolean = true
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val progress: Int = 0,
    val maxProgress: Int = 1
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val isDemoMode: Boolean = true
)
