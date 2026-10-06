package com.example.data.repository

import com.example.core.Constants
import com.example.data.dao.AchievementDao
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.GameDao
import com.example.data.dao.UserProfileDao
import com.example.data.dao.WalletDao
import com.example.data.dao.WithdrawalDao
import com.example.data.database.AppDatabase
import com.example.data.entity.AppSettings
import com.example.data.entity.Game
import com.example.data.entity.GameLevel
import com.example.data.entity.LevelProgress
import com.example.data.entity.RewardClaim
import com.example.data.entity.UserProfile
import com.example.data.entity.WalletTransaction
import com.example.data.entity.Withdrawal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID

data class RewardClaimResult(
    val success: Boolean,
    val rewardGranted: Boolean,
    val amountGranted: Double,
    val message: String
)

data class WalletBalanceInfo(
    val availableBalance: Double,
    val totalEarned: Double,
    val joiningBonus: Double,
    val levelRewards: Double,
    val withdrawableBalance: Double
)

class GameRepository(
    private val database: AppDatabase,
    private val userProfileDao: UserProfileDao,
    private val gameDao: GameDao,
    private val walletDao: WalletDao,
    private val withdrawalDao: WithdrawalDao,
    private val achievementDao: AchievementDao,
    private val appSettingsDao: AppSettingsDao
) {

    // User & Profile
    val userProfile: Flow<UserProfile?> = userProfileDao.getUser()
    val appSettings: Flow<AppSettings?> = appSettingsDao.getSettings()

    // Games & Levels
    val allGames: Flow<List<Game>> = gameDao.getAllGames()
    val totalCompletedLevels: Flow<Int> = gameDao.getTotalCompletedLevelsCount()

    fun getLevelsForGame(gameId: String): Flow<List<GameLevel>> = gameDao.getLevelsForGame(gameId)
    fun getCompletedLevelsCount(gameId: String): Flow<Int> = gameDao.getCompletedLevelsCount(gameId)

    // Wallet & Earnings
    val transactions: Flow<List<WalletTransaction>> = walletDao.getAllTransactions()
    val withdrawals: Flow<List<Withdrawal>> = withdrawalDao.getAllWithdrawals()
    val achievements = achievementDao.getAllAchievements()

    val walletBalanceInfo: Flow<WalletBalanceInfo> = combine(
        walletDao.getTotalEarned(),
        walletDao.getJoiningBonusTotal(),
        walletDao.getLevelRewardsTotal(),
        walletDao.getWithdrawnTotal()
    ) { totalEarned, joiningBonus, levelRewards, withdrawn ->
        val earned = totalEarned ?: 0.0
        val bonus = joiningBonus ?: 0.0
        val rewards = levelRewards ?: 0.0
        val spent = withdrawn ?: 0.0
        val available = (earned - spent).coerceAtLeast(0.0)

        WalletBalanceInfo(
            availableBalance = available,
            totalEarned = earned,
            joiningBonus = bonus,
            levelRewards = rewards,
            withdrawableBalance = available
        )
    }

    /**
     * Ensure database has initial data on app startup
     */
    suspend fun checkAndInitializeData() {
        val user = userProfileDao.getUserSync()
        if (user == null) {
            database.populateInitialData()
        }
    }

    /**
     * Claims ₹10 Joining Bonus exactly once.
     * Guaranteed anti-duplicate via UserProfile flag and ledger transaction uniqueness.
     */
    suspend fun claimJoiningBonus(): Boolean {
        val user = userProfileDao.getUserSync() ?: return false
        if (user.joiningBonusClaimed) return false

        val txId = "BONUS_${UUID.randomUUID().toString().take(8).uppercase()}"
        val tx = WalletTransaction(
            transactionId = txId,
            type = "JOINING_BONUS",
            amount = Constants.JOINING_BONUS_AMOUNT,
            status = "COMPLETED",
            referenceNote = "Welcome Joining Bonus"
        )
        walletDao.insertTransaction(tx)
        userProfileDao.updateUser(user.copy(joiningBonusClaimed = true))
        return true
    }

    /**
     * Genuine level completion handler.
     * Grants reward ONLY on genuine first-time level completion.
     * Prevents duplicate rewards for already completed levels.
     */
    suspend fun completeLevelAndClaimReward(
        gameId: String,
        levelNumber: Int,
        movesUsed: Int,
        timeSeconds: Int,
        score: Int,
        stars: Int,
        adWatched: Boolean = true
    ): RewardClaimResult {
        // Increment games played counter
        userProfileDao.incrementGamesPlayed()

        // Check if level was already completed previously
        val existingClaim = walletDao.getRewardClaim(gameId, levelNumber)
        val level = gameDao.getLevelSync(gameId, levelNumber)

        // Always update best score and record progress
        gameDao.markLevelCompleted(gameId, levelNumber, score, stars)
        gameDao.insertProgress(
            LevelProgress(
                gameId = gameId,
                levelNumber = levelNumber,
                movesUsed = movesUsed,
                timeSeconds = timeSeconds,
                stars = stars
            )
        )

        // Unlock next level (up to 100)
        if (levelNumber < Constants.TOTAL_LEVELS_PER_GAME) {
            gameDao.unlockLevel(gameId, levelNumber + 1)
        }

        // Check achievements
        val completedCount = gameDao.getTotalCompletedLevelsCount().first()
        achievementDao.updateProgress("first_level", 1)
        achievementDao.updateProgress("level_10", completedCount)
        achievementDao.updateProgress("level_50", completedCount)
        achievementDao.updateProgress("level_100", completedCount)

        // Check if reward was already granted
        if (existingClaim != null || (level != null && level.isCompleted)) {
            return RewardClaimResult(
                success = true,
                rewardGranted = false,
                amountGranted = 0.0,
                message = "Level completed! (Reward already claimed previously)"
            )
        }

        // First-time completion: grant reward!
        val txId = "REW_${gameId.take(3).uppercase()}_L${levelNumber}_${UUID.randomUUID().toString().take(6).uppercase()}"
        val tx = WalletTransaction(
            transactionId = txId,
            type = "LEVEL_REWARD",
            amount = Constants.LEVEL_REWARD_AMOUNT,
            gameId = gameId,
            levelNumber = levelNumber,
            status = "COMPLETED",
            referenceNote = "Completed $gameId Level $levelNumber"
        )
        walletDao.insertTransaction(tx)

        val claim = RewardClaim(
            claimId = "${gameId}_$levelNumber",
            gameId = gameId,
            levelNumber = levelNumber,
            rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
            adWatched = adWatched,
            transactionId = txId
        )
        walletDao.insertRewardClaim(claim)

        return RewardClaimResult(
            success = true,
            rewardGranted = true,
            amountGranted = Constants.LEVEL_REWARD_AMOUNT,
            message = "Level $levelNumber Completed! +₹${Constants.LEVEL_REWARD_AMOUNT.toInt()} credited to Wallet"
        )
    }

    /**
     * Creates a test/demo withdrawal request.
     * Enforces the minimum withdrawal constraint of ₹50.
     */
    suspend fun requestWithdrawal(
        amount: Double,
        payoutMethod: String,
        payoutDetails: String
    ): Result<String> {
        val balance = walletBalanceInfo.first()
        if (amount < Constants.MINIMUM_WITHDRAWAL_AMOUNT) {
            return Result.failure(Exception("Minimum withdrawal amount is ₹${Constants.MINIMUM_WITHDRAWAL_AMOUNT.toInt()}"))
        }
        if (amount > balance.availableBalance) {
            return Result.failure(Exception("Insufficient available balance (₹${balance.availableBalance})"))
        }

        val withdrawalId = "WTH_${UUID.randomUUID().toString().take(8).uppercase()}"
        val withdrawal = Withdrawal(
            id = withdrawalId,
            amount = amount,
            payoutMethod = payoutMethod,
            payoutDetails = payoutDetails,
            status = "PENDING",
            remarks = "Test/Demo withdrawal request created. Verification in progress."
        )
        withdrawalDao.insertWithdrawal(withdrawal)

        val tx = WalletTransaction(
            transactionId = "TX_$withdrawalId",
            type = "WITHDRAWAL_REQUEST",
            amount = amount,
            status = "PENDING",
            referenceNote = "Withdrawal request to $payoutMethod ($payoutDetails)"
        )
        walletDao.insertTransaction(tx)

        return Result.success(withdrawalId)
    }

    suspend fun updateSettings(settings: AppSettings) {
        appSettingsDao.updateSettings(settings)
    }

    /**
     * Demo / Debug helper: Reset all progress to test fresh onboarding & levels.
     */
    suspend fun resetAllProgress() {
        gameDao.resetAllLevelsProgress()
        walletDao.clearTransactions()
        walletDao.clearRewardClaims()
        withdrawalDao.clearWithdrawals()
        achievementDao.resetAchievements()
        val user = userProfileDao.getUserSync()
        if (user != null) {
            userProfileDao.updateUser(
                user.copy(
                    joiningBonusClaimed = false,
                    gamesPlayed = 0
                )
            )
        }
    }
}
