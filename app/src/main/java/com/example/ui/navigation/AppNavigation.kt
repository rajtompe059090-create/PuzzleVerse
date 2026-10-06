package com.example.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.PuzzleApplication
import com.example.core.Constants
import com.example.games.arrowflow.ArrowFlowGameScreen
import com.example.games.blockmerge.BlockMergeGameScreen
import com.example.games.colorpath.ColorPathGameScreen
import com.example.games.tilematch.TileMatchGameScreen
import com.example.ui.screens.GameSelectScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelMapScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TermsScreen
import com.example.ui.screens.WalletScreen
import com.example.ui.theme.DarkBgPrimary
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.Screen

data class NavTab(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)

@Composable
fun AppNavigation(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val app = context.applicationContext as PuzzleApplication
    val adMobManager = app.adMobManager
    val activity = context as? Activity

    val currentScreen by viewModel.currentScreen.collectAsState()
    var activeBottomTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var previousGameId by remember { mutableStateOf<String?>(null) }

    val navTabs = listOf(
        NavTab("Home", Icons.Default.Home, Screen.Home),
        NavTab("Games", Icons.Default.Gamepad, Screen.Games),
        NavTab("Wallet", Icons.Default.AccountBalanceWallet, Screen.Wallet),
        NavTab("Profile", Icons.Default.Person, Screen.Profile),
        NavTab("Settings", Icons.Default.Settings, Screen.Settings)
    )

    val showBottomBar = currentScreen in listOf(
        Screen.Home,
        Screen.Games,
        Screen.Wallet,
        Screen.Profile,
        Screen.Settings
    )

    // BackHandler: Return to Home or previous screen
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            is Screen.Gameplay -> {
                val gameId = (currentScreen as Screen.Gameplay).gameId
                viewModel.navigateTo(Screen.LevelMap(gameId))
            }
            is Screen.LevelMap -> {
                viewModel.navigateTo(Screen.Games)
            }
            is Screen.PrivacyPolicy, is Screen.Terms -> {
                viewModel.navigateTo(Screen.Settings)
            }
            else -> {
                viewModel.navigateTo(Screen.Home)
                activeBottomTab = Screen.Home
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBgPrimary,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .height(64.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                        .background(Color(0xFF0D1424))
                        .border(1.dp, Color(0xFF1B263E), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                    containerColor = Color(0xFF0D1424)
                ) {
                    navTabs.forEach { tab ->
                        val isSelected = (currentScreen == tab.screen)
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                activeBottomTab = tab.screen
                                viewModel.navigateTo(tab.screen)
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ElectricBlue,
                                selectedTextColor = ElectricBlue,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Color(0x3300E5FF)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { target ->
                when (target) {
                    is Screen.Home -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onPlayGame = { gameId ->
                                // Trigger Interstitial ad on game switch if different
                                if (previousGameId != null && previousGameId != gameId) {
                                    adMobManager.showInterstitialOnGameSwitch(activity) {
                                        previousGameId = gameId
                                        viewModel.navigateTo(Screen.LevelMap(gameId))
                                    }
                                } else {
                                    previousGameId = gameId
                                    viewModel.navigateTo(Screen.LevelMap(gameId))
                                }
                            },
                            onOpenWallet = {
                                activeBottomTab = Screen.Wallet
                                viewModel.navigateTo(Screen.Wallet)
                            }
                        )
                    }
                    is Screen.Games -> {
                        GameSelectScreen(
                            viewModel = viewModel,
                            onSelectGame = { gameId ->
                                if (previousGameId != null && previousGameId != gameId) {
                                    adMobManager.showInterstitialOnGameSwitch(activity) {
                                        previousGameId = gameId
                                        viewModel.navigateTo(Screen.LevelMap(gameId))
                                    }
                                } else {
                                    previousGameId = gameId
                                    viewModel.navigateTo(Screen.LevelMap(gameId))
                                }
                            },
                            onOpenWallet = {
                                activeBottomTab = Screen.Wallet
                                viewModel.navigateTo(Screen.Wallet)
                            }
                        )
                    }
                    is Screen.LevelMap -> {
                        LevelMapScreen(
                            gameId = target.gameId,
                            viewModel = viewModel,
                            onBack = { viewModel.navigateTo(Screen.Games) },
                            onPlayLevel = { levelNum ->
                                viewModel.navigateTo(Screen.Gameplay(target.gameId, levelNum))
                            },
                            onOpenWallet = {
                                activeBottomTab = Screen.Wallet
                                viewModel.navigateTo(Screen.Wallet)
                            }
                        )
                    }
                    is Screen.Gameplay -> {
                        when (target.gameId) {
                            Constants.GAME_ARROW_FLOW -> {
                                ArrowFlowGameScreen(
                                    levelNumber = target.levelNumber,
                                    onBack = { viewModel.navigateTo(Screen.LevelMap(target.gameId)) },
                                    onNextLevel = { nextLvl ->
                                        viewModel.navigateTo(Screen.Gameplay(target.gameId, nextLvl))
                                    }
                                )
                            }
                            Constants.GAME_BLOCK_MERGE -> {
                                BlockMergeGameScreen(
                                    levelNumber = target.levelNumber,
                                    onBack = { viewModel.navigateTo(Screen.LevelMap(target.gameId)) },
                                    onNextLevel = { nextLvl ->
                                        viewModel.navigateTo(Screen.Gameplay(target.gameId, nextLvl))
                                    }
                                )
                            }
                            Constants.GAME_COLOR_PATH -> {
                                ColorPathGameScreen(
                                    levelNumber = target.levelNumber,
                                    onBack = { viewModel.navigateTo(Screen.LevelMap(target.gameId)) },
                                    onNextLevel = { nextLvl ->
                                        viewModel.navigateTo(Screen.Gameplay(target.gameId, nextLvl))
                                    }
                                )
                            }
                            else -> {
                                TileMatchGameScreen(
                                    levelNumber = target.levelNumber,
                                    onBack = { viewModel.navigateTo(Screen.LevelMap(target.gameId)) },
                                    onNextLevel = { nextLvl ->
                                        viewModel.navigateTo(Screen.Gameplay(target.gameId, nextLvl))
                                    }
                                )
                            }
                        }
                    }
                    is Screen.Wallet -> {
                        WalletScreen(
                            viewModel = viewModel,
                            onBack = {
                                activeBottomTab = Screen.Home
                                viewModel.navigateTo(Screen.Home)
                            }
                        )
                    }
                    is Screen.Profile -> {
                        ProfileScreen(
                            viewModel = viewModel,
                            onOpenWallet = {
                                activeBottomTab = Screen.Wallet
                                viewModel.navigateTo(Screen.Wallet)
                            }
                        )
                    }
                    is Screen.Settings -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = {
                                activeBottomTab = Screen.Home
                                viewModel.navigateTo(Screen.Home)
                            },
                            onOpenPrivacyPolicy = { viewModel.navigateTo(Screen.PrivacyPolicy) },
                            onOpenTerms = { viewModel.navigateTo(Screen.Terms) }
                        )
                    }
                    is Screen.PrivacyPolicy -> {
                        PrivacyPolicyScreen(
                            onBack = { viewModel.navigateTo(Screen.Settings) }
                        )
                    }
                    is Screen.Terms -> {
                        TermsScreen(
                            onBack = { viewModel.navigateTo(Screen.Settings) }
                        )
                    }
                }
            }
        }
    }
}
