package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.Constants
import com.example.data.dao.AchievementDao
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.GameDao
import com.example.data.dao.UserProfileDao
import com.example.data.dao.WalletDao
import com.example.data.dao.WithdrawalDao
import com.example.data.entity.Achievement
import com.example.data.entity.AdReward
import com.example.data.entity.AppSettings
import com.example.data.entity.Game
import com.example.data.entity.GameLevel
import com.example.data.entity.LevelProgress
import com.example.data.entity.RewardClaim
import com.example.data.entity.UserProfile
import com.example.data.entity.WalletTransaction
import com.example.data.entity.Withdrawal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        Game::class,
        GameLevel::class,
        LevelProgress::class,
        WalletTransaction::class,
        RewardClaim::class,
        Withdrawal::class,
        AdReward::class,
        Achievement::class,
        AppSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun gameDao(): GameDao
    abstract fun walletDao(): WalletDao
    abstract fun withdrawalDao(): WithdrawalDao
    abstract fun achievementDao(): AchievementDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "puzzleverse_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).populateInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun populateInitialData() {
        // Initial user
        userProfileDao().insertUser(
            UserProfile(
                id = 1,
                username = "PuzzleMaster",
                avatarIndex = 0,
                memberSinceTimestamp = System.currentTimeMillis(),
                joiningBonusClaimed = false,
                currentStreak = 1,
                gamesPlayed = 0
            )
        )

        // Initial settings
        appSettingsDao().insertSettings(
            AppSettings(
                id = 1,
                soundEnabled = true,
                musicEnabled = true,
                vibrationEnabled = true,
                notificationsEnabled = true,
                isDemoMode = true
            )
        )

        // Initial Games
        val games = listOf(
            Game(
                id = Constants.GAME_ARROW_FLOW,
                name = "Arrow Flow",
                description = "Clear arrows in the unobstructed sequence",
                iconName = "arrow_flow",
                totalLevels = Constants.TOTAL_LEVELS_PER_GAME
            ),
            Game(
                id = Constants.GAME_BLOCK_MERGE,
                name = "Block Merge",
                description = "Combine matching blocks to reach high target values",
                iconName = "block_merge",
                totalLevels = Constants.TOTAL_LEVELS_PER_GAME
            ),
            Game(
                id = Constants.GAME_COLOR_PATH,
                name = "Color Path",
                description = "Connect matching colors without crossing lines",
                iconName = "color_path",
                totalLevels = Constants.TOTAL_LEVELS_PER_GAME
            ),
            Game(
                id = Constants.GAME_TILE_MATCH,
                name = "Tile Match",
                description = "Match 3 identical jewel tiles to clear the board",
                iconName = "tile_match",
                totalLevels = Constants.TOTAL_LEVELS_PER_GAME
            )
        )
        gameDao().insertGames(games)

        // Initial Levels (1 to 100 for each game)
        val allLevels = mutableListOf<GameLevel>()
        for (game in games) {
            for (level in 1..Constants.TOTAL_LEVELS_PER_GAME) {
                val difficulty = when {
                    level <= 10 -> "Easy"
                    level <= 30 -> "Medium"
                    level <= 70 -> "Hard"
                    else -> "Expert"
                }
                allLevels.add(
                    GameLevel(
                        gameId = game.id,
                        levelNumber = level,
                        difficulty = difficulty,
                        rewardAmount = Constants.LEVEL_REWARD_AMOUNT,
                        isUnlocked = (level == 1),
                        isCompleted = false,
                        bestScore = 0,
                        bestStars = 0
                    )
                )
            }
        }
        gameDao().insertLevels(allLevels)

        // Initial Achievements
        val achievements = listOf(
            Achievement("first_level", "First Steps", "Complete your first level in any game", "flag", false, null, 0, 1),
            Achievement("level_10", "Puzzle Adept", "Complete 10 levels across all games", "stars", false, null, 0, 10),
            Achievement("level_50", "Brain Master", "Complete 50 levels across all games", "trophy", false, null, 0, 50),
            Achievement("level_100", "Puzzle God", "Complete 100 levels across all games", "crown", false, null, 0, 100),
            Achievement("streak_7", "Daily Champion", "Keep a 7-day gameplay streak", "fire", false, null, 1, 7),
            Achievement("wallet_50", "High Earner", "Earn ₹50 in rewards", "wallet", false, null, 0, 50)
        )
        achievementDao().insertAchievements(achievements)
    }
}
