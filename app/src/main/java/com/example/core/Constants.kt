package com.example.core

object Constants {
    const val APP_NAME = "PuzzleVerse"
    const val DEVELOPER_NAME = "PuzzleVerse Studio"
    const val SUPPORT_EMAIL = "support@puzzleverse.games"
    const val SUPPORT_WHATSAPP_NUMBER = "+919876543210" // Admin WhatsApp for manual withdrawal processing
    const val PRIVACY_POLICY_URL = "https://puzzleverse.games/privacy-policy"
    const val TERMS_URL = "https://puzzleverse.games/terms"

    // Rewards (Coins / Reward Units)
    const val JOINING_BONUS_AMOUNT = 10.0
    const val LEVEL_REWARD_AMOUNT = 1.0 // Standard level completion reward (₹1)
    const val MINIMUM_WITHDRAWAL_AMOUNT = 50.0

    // Games
    const val GAME_ARROW_FLOW = "arrow_flow"
    const val GAME_BLOCK_MERGE = "block_merge"
    const val GAME_HEART_MAZE = "heart_maze"
    const val GAME_TILE_MATCH = "tile_match"

    const val TOTAL_LEVELS_PER_GAME = 100

    // AdMob Settings & Caps
    const val INTERSTITIAL_COOLDOWN_MS = 60_000L // 1 minute cooldown between game switches

    // Hints
    const val FREE_HINTS_PER_LEVEL = 2
}
