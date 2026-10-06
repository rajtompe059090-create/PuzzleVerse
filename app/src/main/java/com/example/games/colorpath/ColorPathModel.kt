package com.example.games.colorpath

import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ColorDot(
    val colorIndex: Int,
    val color: Color,
    val name: String,
    val startRow: Int,
    val startCol: Int,
    val endRow: Int,
    val endCol: Int
)

data class ColorPathState(
    val gridSize: Int,
    val dots: List<ColorDot>,
    val obstacles: Set<Pair<Int, Int>>,
    val paths: Map<Int, List<Pair<Int, Int>>> = emptyMap(), // colorIndex -> list of (row, col)
    val isCompleted: Boolean = false
)

object ColorPathGenerator {

    val PALETTE = listOf(
        Pair("Blue", Color(0xFF00E5FF)),
        Pair("Purple", Color(0xFFA855F7)),
        Pair("Pink", Color(0xFFFF2A85)),
        Pair("Gold", Color(0xFFFFB800)),
        Pair("Emerald", Color(0xFF10B981)),
        Pair("Coral", Color(0xFFFF5252))
    )

    fun generateLevel(level: Int): ColorPathState {
        val gridSize = when {
            level <= 5 -> 4
            level <= 25 -> 5
            else -> 6
        }

        val pairCount = when {
            level <= 3 -> 3
            level <= 15 -> 4
            level <= 40 -> 5
            else -> 5
        }.coerceAtMost(gridSize)

        val rng = Random(level * 777 + 13)

        // Seeded endpoints generator that guarantees simple non-crossing topological paths
        val dots = mutableListOf<ColorDot>()
        val usedCoords = mutableSetOf<Pair<Int, Int>>()

        // Generate distinct valid pairs
        for (i in 0 until pairCount) {
            val (colorName, colorVal) = PALETTE[i % PALETTE.size]

            var start: Pair<Int, Int>
            var end: Pair<Int, Int>
            var attempts = 0

            do {
                val sr = rng.nextInt(gridSize)
                val sc = rng.nextInt(gridSize)
                val er = rng.nextInt(gridSize)
                val ec = rng.nextInt(gridSize)
                start = Pair(sr, sc)
                end = Pair(er, ec)
                attempts++
            } while ((start == end || usedCoords.contains(start) || usedCoords.contains(end) ||
                    kotlin.math.abs(start.first - end.first) + kotlin.math.abs(start.second - end.second) < 2) && attempts < 100)

            usedCoords.add(start)
            usedCoords.add(end)

            dots.add(
                ColorDot(
                    colorIndex = i,
                    color = colorVal,
                    name = colorName,
                    startRow = start.first,
                    startCol = start.second,
                    endRow = end.first,
                    endCol = end.second
                )
            )
        }

        // Obstacles (levels > 10)
        val obstacles = mutableSetOf<Pair<Int, Int>>()
        if (level > 10) {
            val obsCount = if (level > 30) 2 else 1
            var tries = 0
            while (obstacles.size < obsCount && tries < 30) {
                tries++
                val r = rng.nextInt(gridSize)
                val c = rng.nextInt(gridSize)
                val pt = Pair(r, c)
                if (!usedCoords.contains(pt)) {
                    obstacles.add(pt)
                }
            }
        }

        return ColorPathState(
            gridSize = gridSize,
            dots = dots,
            obstacles = obstacles
        )
    }

    /**
     * Check if a path for colorIndex connects start and end dots
     */
    fun isPairConnected(dot: ColorDot, path: List<Pair<Int, Int>>): Boolean {
        if (path.size < 2) return false
        val first = path.first()
        val last = path.last()

        val start = Pair(dot.startRow, dot.startCol)
        val end = Pair(dot.endRow, dot.endCol)

        return (first == start && last == end) || (first == end && last == start)
    }

    /**
     * Check if all pairs are connected with zero cell overlap
     */
    fun checkLevelCompletion(state: ColorPathState): Boolean {
        if (state.paths.size < state.dots.size) return false

        val occupiedCells = mutableMapOf<Pair<Int, Int>, Int>() // cell -> colorIndex

        for (dot in state.dots) {
            val path = state.paths[dot.colorIndex] ?: return false
            if (!isPairConnected(dot, path)) return false

            // Check continuity
            for (i in 0 until path.size - 1) {
                val (r1, c1) = path[i]
                val (r2, c2) = path[i + 1]
                val dist = kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2)
                if (dist != 1) return false // Must be adjacent steps
            }

            // Check cell overlaps
            for (cell in path) {
                if (state.obstacles.contains(cell)) return false
                val prevOwner = occupiedCells[cell]
                if (prevOwner != null && prevOwner != dot.colorIndex) {
                    return false // Intersected!
                }
                occupiedCells[cell] = dot.colorIndex
            }
        }

        return true
    }
}
