package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.Achievement
import com.example.data.entity.AppSettings
import com.example.data.entity.Game
import com.example.data.entity.GameLevel
import com.example.data.entity.LevelProgress
import com.example.data.entity.RewardClaim
import com.example.data.entity.UserProfile
import com.example.data.entity.WalletTransaction
import com.example.data.entity.Withdrawal
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUser(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserProfile)

    @Update
    suspend fun updateUser(user: UserProfile)

    @Query("UPDATE user_profile SET gamesPlayed = gamesPlayed + 1 WHERE id = 1")
    suspend fun incrementGamesPlayed()
}

@Dao
interface GameDao {
    @Query("SELECT * FROM games")
    fun getAllGames(): Flow<List<Game>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGames(games: List<Game>)

    @Query("SELECT * FROM game_levels WHERE gameId = :gameId ORDER BY levelNumber ASC")
    fun getLevelsForGame(gameId: String): Flow<List<GameLevel>>

    @Query("SELECT COUNT(*) FROM game_levels WHERE gameId = :gameId AND isCompleted = 1")
    fun getCompletedLevelsCount(gameId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM game_levels WHERE isCompleted = 1")
    fun getTotalCompletedLevelsCount(): Flow<Int>

    @Query("SELECT * FROM game_levels WHERE gameId = :gameId AND levelNumber = :levelNumber LIMIT 1")
    fun getLevel(gameId: String, levelNumber: Int): Flow<GameLevel?>

    @Query("SELECT * FROM game_levels WHERE gameId = :gameId AND levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevelSync(gameId: String, levelNumber: Int): GameLevel?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLevels(levels: List<GameLevel>)

    @Query("UPDATE game_levels SET isUnlocked = 1 WHERE gameId = :gameId AND levelNumber = :levelNumber")
    suspend fun unlockLevel(gameId: String, levelNumber: Int)

    @Query("UPDATE game_levels SET isCompleted = 1, bestScore = CASE WHEN :score > bestScore THEN :score ELSE bestScore END, bestStars = CASE WHEN :stars > bestStars THEN :stars ELSE bestStars END WHERE gameId = :gameId AND levelNumber = :levelNumber")
    suspend fun markLevelCompleted(gameId: String, levelNumber: Int, score: Int, stars: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: LevelProgress)

    @Query("UPDATE game_levels SET isCompleted = 0, isUnlocked = CASE WHEN levelNumber = 1 THEN 1 ELSE 0 END, bestScore = 0, bestStars = 0")
    suspend fun resetAllLevelsProgress()
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<WalletTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(tx: WalletTransaction)

    @Query("SELECT * FROM reward_claims WHERE gameId = :gameId AND levelNumber = :levelNumber LIMIT 1")
    suspend fun getRewardClaim(gameId: String, levelNumber: Int): RewardClaim?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRewardClaim(claim: RewardClaim)

    @Query("SELECT SUM(amount) FROM wallet_transactions WHERE type IN ('JOINING_BONUS', 'LEVEL_REWARD', 'REFUND') AND status = 'COMPLETED'")
    fun getTotalEarned(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM wallet_transactions WHERE type = 'JOINING_BONUS' AND status = 'COMPLETED'")
    fun getJoiningBonusTotal(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM wallet_transactions WHERE type = 'LEVEL_REWARD' AND status = 'COMPLETED'")
    fun getLevelRewardsTotal(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM wallet_transactions WHERE type = 'WITHDRAWAL_REQUEST' AND status != 'REJECTED'")
    fun getWithdrawnTotal(): Flow<Double?>

    @Query("DELETE FROM wallet_transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM reward_claims")
    suspend fun clearRewardClaims()
}

@Dao
interface WithdrawalDao {
    @Query("SELECT * FROM withdrawals ORDER BY requestTimestamp DESC")
    fun getAllWithdrawals(): Flow<List<Withdrawal>>

    @Query("SELECT * FROM withdrawals WHERE id = :id LIMIT 1")
    suspend fun getWithdrawalById(id: String): Withdrawal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(withdrawal: Withdrawal)

    @Query("UPDATE withdrawals SET status = :status, remarks = :remarks WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: String, status: String, remarks: String)

    @Query("DELETE FROM withdrawals")
    suspend fun clearWithdrawals()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)

    @Query("UPDATE achievements SET progress = :progress, isUnlocked = CASE WHEN :progress >= maxProgress THEN 1 ELSE isUnlocked END, unlockedAt = CASE WHEN :progress >= maxProgress AND unlockedAt IS NULL THEN :now ELSE unlockedAt END WHERE id = :id")
    suspend fun updateProgress(id: String, progress: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE achievements SET isUnlocked = 0, unlockedAt = NULL, progress = 0")
    suspend fun resetAchievements()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: AppSettings)

    @Update
    suspend fun updateSettings(settings: AppSettings)
}
