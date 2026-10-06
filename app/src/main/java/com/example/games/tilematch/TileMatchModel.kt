package com.example.games.tilematch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Pentagon
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Token
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.random.Random

data class TileType(
    val id: Int,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

data class GameTile(
    val id: Int,
    val type: TileType,
    val isRemoved: Boolean = false
)

object TileMatchHelper {

    val TILE_TYPES = listOf(
        TileType(1, "Ruby", Icons.Default.Diamond, Color(0xFFFF2A85)),
        TileType(2, "Emerald", Icons.Default.Pentagon, Color(0xFF10B981)),
        TileType(3, "Sapphire", Icons.Default.Hexagon, Color(0xFF00E5FF)),
        TileType(4, "Amethyst", Icons.Default.Square, Color(0xFFA855F7)),
        TileType(5, "Gold Star", Icons.Default.Star, Color(0xFFFFB800)),
        TileType(6, "Heart", Icons.Default.Favorite, Color(0xFFFF4B6E)),
        TileType(7, "Medal", Icons.Default.WorkspacePremium, Color(0xFFFFD166)),
        TileType(8, "Token", Icons.Default.Token, Color(0xFF06D6A0))
    )

    fun generateLevel(level: Int): TileMatchState {
        val distinctTypesCount = when {
            level <= 5 -> 3
            level <= 15 -> 4
            level <= 35 -> 5
            level <= 60 -> 6
            else -> 7
        }

        val tripletsPerType = when {
            level <= 5 -> 2 // 2*3 = 6 per type * 3 types = 18 tiles
            level <= 15 -> 2 // 24 tiles
            level <= 35 -> 3 // 45 tiles
            else -> 3 // 54 tiles
        }

        val rng = Random(level * 1337 + 99)
        val selectedTypes = TILE_TYPES.shuffled(rng).take(distinctTypesCount)

        val tiles = mutableListOf<GameTile>()
        var tileId = 1

        for (type in selectedTypes) {
            val totalOfThisType = tripletsPerType * 3
            repeat(totalOfThisType) {
                tiles.add(GameTile(id = tileId++, type = type))
            }
        }
        tiles.shuffle(rng)

        val timerSeconds = when {
            level <= 10 -> 0 // Unlimited
            level <= 30 -> 120
            level <= 60 -> 90
            else -> 75
        }

        return TileMatchState(
            tiles = tiles,
            dock = emptyList(),
            timerSeconds = timerSeconds,
            totalTiles = tiles.size
        )
    }
}

data class TileMatchState(
    val tiles: List<GameTile>,
    val dock: List<GameTile>, // max 7
    val timerSeconds: Int,
    val totalTiles: Int,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false
) {
    val remainingTilesOnBoard: Int
        get() = tiles.count { !it.isRemoved }
}
