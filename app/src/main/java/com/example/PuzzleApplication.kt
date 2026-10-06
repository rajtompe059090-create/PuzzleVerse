package com.example

import android.app.Application
import com.example.ads.AdMobManager
import com.example.core.SoundHapticManager
import com.example.data.database.AppDatabase
import com.example.data.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PuzzleApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: GameRepository
        private set

    lateinit var soundHapticManager: SoundHapticManager
        private set

    lateinit var adMobManager: AdMobManager
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        repository = GameRepository(
            database = database,
            userProfileDao = database.userProfileDao(),
            gameDao = database.gameDao(),
            walletDao = database.walletDao(),
            withdrawalDao = database.withdrawalDao(),
            achievementDao = database.achievementDao(),
            appSettingsDao = database.appSettingsDao()
        )
        soundHapticManager = SoundHapticManager(this)
        adMobManager = AdMobManager(this)

        CoroutineScope(Dispatchers.IO).launch {
            repository.checkAndInitializeData()
        }
    }
}
