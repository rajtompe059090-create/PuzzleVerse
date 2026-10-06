package com.example.games.blockmerge

import kotlin.random.Random

data class BlockTile(
    val id: Int,
    val value: Int,
    val row: Int,
    val col: Int,
    val isMerged: Boolean = false
)

enum class SlideDirection {
    UP, DOWN, LEFT, RIGHT
}

object BlockMergeHelper {

    fun getTargetForLevel(level: Int): Int {
        return when {
            level <= 2 -> 64
            level <= 6 -> 128
            level <= 12 -> 256
            level <= 25 -> 512
            level <= 50 -> 1024
            else -> 2048
        }
    }

    fun getMaxMovesForLevel(level: Int): Int {
        return when {
            level <= 2 -> 35
            level <= 6 -> 50
            level <= 12 -> 70
            level <= 25 -> 95
            else -> 120
        }
    }

    fun spawnNewTile(grid: Array<IntArray>, rng: Random): Pair<Int, Int>? {
        val emptySlots = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (grid[r][c] == 0) emptySlots.add(Pair(r, c))
            }
        }
        if (emptySlots.isEmpty()) return null
        return emptySlots[rng.nextInt(emptySlots.size)]
    }

    fun slide(
        currentGrid: Array<IntArray>,
        dir: SlideDirection
    ): SlideResult {
        val grid = Array(4) { r -> currentGrid[r].clone() }
        var moved = false
        var scoreGained = 0
        var mergesCount = 0

        when (dir) {
            SlideDirection.LEFT -> {
                for (r in 0 until 4) {
                    val line = grid[r].filter { it != 0 }.toMutableList()
                    var i = 0
                    while (i < line.size - 1) {
                        if (line[i] == line[i + 1]) {
                            line[i] *= 2
                            scoreGained += line[i]
                            mergesCount++
                            line.removeAt(i + 1)
                        }
                        i++
                    }
                    while (line.size < 4) line.add(0)
                    for (c in 0 until 4) {
                        if (grid[r][c] != line[c]) moved = true
                        grid[r][c] = line[c]
                    }
                }
            }
            SlideDirection.RIGHT -> {
                for (r in 0 until 4) {
                    val line = grid[r].filter { it != 0 }.toMutableList()
                    var i = line.size - 1
                    while (i > 0) {
                        if (line[i] == line[i - 1]) {
                            line[i] *= 2
                            scoreGained += line[i]
                            mergesCount++
                            line.removeAt(i - 1)
                            i--
                        }
                        i--
                    }
                    while (line.size < 4) line.add(0, 0)
                    for (c in 0 until 4) {
                        if (grid[r][c] != line[c]) moved = true
                        grid[r][c] = line[c]
                    }
                }
            }
            SlideDirection.UP -> {
                for (c in 0 until 4) {
                    val line = (0 until 4).map { grid[it][c] }.filter { it != 0 }.toMutableList()
                    var i = 0
                    while (i < line.size - 1) {
                        if (line[i] == line[i + 1]) {
                            line[i] *= 2
                            scoreGained += line[i]
                            mergesCount++
                            line.removeAt(i + 1)
                        }
                        i++
                    }
                    while (line.size < 4) line.add(0)
                    for (r in 0 until 4) {
                        if (grid[r][c] != line[r]) moved = true
                        grid[r][c] = line[r]
                    }
                }
            }
            SlideDirection.DOWN -> {
                for (c in 0 until 4) {
                    val line = (0 until 4).map { grid[it][c] }.filter { it != 0 }.toMutableList()
                    var i = line.size - 1
                    while (i > 0) {
                        if (line[i] == line[i - 1]) {
                            line[i] *= 2
                            scoreGained += line[i]
                            mergesCount++
                            line.removeAt(i - 1)
                            i--
                        }
                        i--
                    }
                    while (line.size < 4) line.add(0, 0)
                    for (r in 0 until 4) {
                        if (grid[r][c] != line[r]) moved = true
                        grid[r][c] = line[r]
                    }
                }
            }
        }

        return SlideResult(grid, moved, scoreGained, mergesCount)
    }

    fun canMakeAnyMove(grid: Array<IntArray>): Boolean {
        for (r in 0 until 4) {
            for (c in 0 until 4) {
                if (grid[r][c] == 0) return true
                if (c < 3 && grid[r][c] == grid[r][c + 1]) return true
                if (r < 3 && grid[r][c] == grid[r + 1][c]) return true
            }
        }
        return false
    }

    fun getBestHint(grid: Array<IntArray>): SlideDirection? {
        val directions = listOf(SlideDirection.UP, SlideDirection.RIGHT, SlideDirection.DOWN, SlideDirection.LEFT)
        var bestDir: SlideDirection? = null
        var maxScore = -1

        for (dir in directions) {
            val res = slide(grid, dir)
            if (res.moved && res.scoreGained > maxScore) {
                maxScore = res.scoreGained
                bestDir = dir
            }
        }
        return bestDir ?: directions.firstOrNull { slide(grid, it).moved }
    }
}

data class SlideResult(
    val newGrid: Array<IntArray>,
    val moved: Boolean,
    val scoreGained: Int,
    val comboCount: Int
)
