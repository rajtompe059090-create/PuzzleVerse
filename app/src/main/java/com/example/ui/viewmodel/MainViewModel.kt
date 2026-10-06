package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.Constants
import com.example.data.entity.Achievement
import com.example.data.entity.AppSettings
import com.example.data.entity.Game
import com.example.data.entity.GameLevel
import com.example.data.entity.UserProfile
import com.example.data.entity.WalletTransaction
import com.example.data.entity.Withdrawal
import com.example.data.repository.GameRepository
import com.example.data.repository.WalletBalanceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Games : Screen("games")
    data class LevelMap(val gameId: String) : Screen("level_map/$gameId")
    data class Gameplay(val gameId: String, val levelNumber: Int) : Screen("gameplay/$gameId/$levelNumber")
    object Wallet : Screen("wallet")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object PrivacyPolicy : Screen("privacy_policy")
    object Terms : Screen("terms")
}

class MainViewModel(
    private val repository: GameRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allGames: StateFlow<List<Game>> = repository.allGames
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val walletBalance: StateFlow<WalletBalanceInfo> = repository.walletBalanceInfo
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WalletBalanceInfo(0.0, 0.0, 0.0, 0.0, 0.0)
        )

    val transactions: StateFlow<List<WalletTransaction>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val withdrawals: StateFlow<List<Withdrawal>> = repository.withdrawals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCompletedLevels: StateFlow<Int> = repository.totalCompletedLevels
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val achievements: StateFlow<List<Achievement>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appSettings: StateFlow<AppSettings?> = repository.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current screen navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Active game id for game switch tracking
    private var lastGameId: String? = null

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun claimJoiningBonus(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val success = repository.claimJoiningBonus()
            if (success) {
                onSuccess()
            }
        }
    }

    fun submitWithdrawal(
        amount: Double,
        method: String,
        details: String,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.requestWithdrawal(amount, method, details)
            onResult(result)
        }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
        }
    }

    fun resetDemoProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
        }
    }

    fun getLevelsForGame(gameId: String) = repository.getLevelsForGame(gameId)
}
