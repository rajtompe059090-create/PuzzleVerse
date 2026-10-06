package com.example.games.arrowflow

import kotlin.random.Random

enum class ArrowDirection {
    UP, RIGHT, DOWN, LEFT
}

data class ArrowCell(
    val id: Int,
    val row: Int,
    val col: Int,
    val direction: ArrowDirection,
    val isObstacle: Boolean = false,
    val isFlying: Boolean = false
)

object ArrowFlowLevelGenerator {

    fun generateLevel(level: Int): ArrowFlowState {
        val gridSize = when {
            level <= 5 -> 4
            level <= 25 -> 5
            else -> 6
        }

        val obstacleCount = when {
            level <= 3 -> 0
            level <= 10 -> 1
            level <= 30 -> 2
            level <= 60 -> 3
            else -> 4
        }

        val arrowCount = when {
            level <= 5 -> 6 + level
            level <= 20 -> 10 + (level - 5) / 2
            else -> 16 + (level.coerceAtMost(60) - 20) / 4
        }.coerceAtMost(gridSize * gridSize - obstacleCount - 2)

        // Seeded random for reproducible levels per level number
        val rng = Random(level * 31337 + 42)

        // 1. Pick obstacle positions
        val obstacles = mutableSetOf<Pair<Int, Int>>()
        var tries = 0
        while (obstacles.size < obstacleCount && tries < 50) {
            tries++
            val r = rng.nextInt(gridSize)
            val c = rng.nextInt(gridSize)
            // avoid blocking all borders
            if (r > 0 && r < gridSize - 1 && c > 0 && c < gridSize - 1) {
                obstacles.add(Pair(r, c))
            }
        }

        // 2. Reverse placement to guarantee solvability
        val grid = Array(gridSize) { BooleanArray(gridSize) { false } }
        obstacles.forEach { (r, c) -> grid[r][c] = true }

        val arrows = mutableListOf<ArrowCell>()
        var arrowId = 1

        val emptyCells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (!grid[r][c]) emptyCells.add(Pair(r, c))
            }
        }
        emptyCells.shuffle(rng)

        for ((r, c) in emptyCells) {
            if (arrows.size >= arrowCount) break

            // Check which directions are unobstructed to the outside border
            val validDirs = mutableListOf<ArrowDirection>()

            // Can fly UP?
            var upClear = true
            for (currR in r - 1 downTo 0) {
                if (grid[currR][c]) { upClear = false; break }
            }
            if (upClear) validDirs.add(ArrowDirection.UP)

            // Can fly DOWN?
            var downClear = true
            for (currR in r + 1 until gridSize) {
                if (grid[currR][c]) { downClear = false; break }
            }
            if (downClear) validDirs.add(ArrowDirection.DOWN)

            // Can fly LEFT?
            var leftClear = true
            for (currC in c - 1 downTo 0) {
                if (grid[r][currC]) { leftClear = false; break }
            }
            if (leftClear) validDirs.add(ArrowDirection.LEFT)

            // Can fly RIGHT?
            var rightClear = true
            for (currC in c + 1 until gridSize) {
                if (grid[r][currC]) { rightClear = false; break }
            }
            if (rightClear) validDirs.add(ArrowDirection.RIGHT)

            if (validDirs.isNotEmpty()) {
                val dir = validDirs[rng.nextInt(validDirs.size)]
                arrows.add(
                    ArrowCell(
                        id = arrowId++,
                        row = r,
                        col = c,
                        direction = dir
                    )
                )
                grid[r][c] = true // Occupy cell for subsequent reverse placements
            }
        }

        // Add obstacle cells into list for rendering
        val allItems = mutableListOf<ArrowCell>()
        allItems.addAll(arrows)
        obstacles.forEach { (r, c) ->
            allItems.add(
                ArrowCell(
                    id = arrowId++,
                    row = r,
                    col = c,
                    direction = ArrowDirection.UP,
                    isObstacle = true
                )
            )
        }

        val maxMoves = (arrows.size * 1.5).toInt() + 2

        return ArrowFlowState(
            gridSize = gridSize,
            cells = allItems,
            maxMoves = maxMoves,
            remainingMoves = maxMoves,
            totalArrows = arrows.size
        )
    }
}

data class ArrowFlowState(
    val gridSize: Int,
    val cells: List<ArrowCell>,
    val maxMoves: Int,
    val remainingMoves: Int,
    val totalArrows: Int,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false
) {
    val remainingArrowCount: Int
        get() = cells.count { !it.isObstacle && !it.isFlying }
}
