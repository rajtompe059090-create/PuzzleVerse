package com.example.games.arrowflow

import kotlin.math.hypot
import kotlin.random.Random

enum class ArrowDirection(val dx: Float, val dy: Float) {
    UP(0f, -1f),
    RIGHT(1f, 0f),
    DOWN(0f, 1f),
    LEFT(-1f, 0f);

    fun opposite(): ArrowDirection = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

data class Obstacle(
    val col: Int,
    val row: Int,
    val width: Float = 1f,
    val height: Float = 1f
)

data class ExitGate(
    val col: Int,
    val row: Int,
    val facing: ArrowDirection
) {
    val x: Float get() = col.toFloat()
    val y: Float get() = row.toFloat()
}

data class ArrowFlowState(
    val levelNumber: Int,
    val gridSize: Int,
    val playerX: Float,
    val playerY: Float,
    val playerRadius: Float = 0.32f,
    val direction: ArrowDirection,
    val nextDirection: ArrowDirection,
    val speed: Float, // grid cells per second
    val obstacles: List<Obstacle>,
    val exitGate: ExitGate,
    val startCol: Int,
    val startRow: Int,
    val solutionPath: List<Pair<Int, Int>>,
    val difficultyTitle: String,
    val snakeTrail: List<Pair<Float, Float>> = emptyList(),
    val isStarted: Boolean = true,
    val isPaused: Boolean = false,
    val isMoving: Boolean = true,
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val movesCount: Int = 0,
    val hintsUsed: Int = 0,
    val showHintPath: Boolean = false
)

object ArrowFlowEngine {

    fun generateLevel(level: Int): ArrowFlowState {
        val (gridSize, speed, obstacleCount, difficultyTitle) = when {
            level <= 25 -> Tuple4(6, 2.8f, 2, "Easy")
            level <= 50 -> Tuple4(7, 3.5f, 4, "Normal")
            level <= 75 -> Tuple4(8, 4.3f, 6, "Hard")
            level <= 90 -> Tuple4(9, 5.0f, 8, "Very Hard")
            else -> Tuple4(10, 5.8f, 10, "Extreme")
        }

        // 100% deterministic seed per level
        val rng = Random(level * 31337 + 101)

        val startCol = 1
        val startRow = 1

        // Exit gate placed on the outer edge
        val exitCol = gridSize - 1
        val exitRow = gridSize - 2
        val exitGate = ExitGate(exitCol, exitRow, ArrowDirection.RIGHT)

        // Carve guaranteed solvable corridor using BFS from start to exit
        val solutionPath = carveSolutionPath(startCol, startRow, exitCol, exitRow, gridSize, rng)

        // Obstacles placed strictly outside the solution path
        val pathSet = solutionPath.toSet()
        val candidateObstacles = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                val coord = Pair(c, r)
                // Do not block start, exit, or solution path
                if (!pathSet.contains(coord) && coord != Pair(startCol, startRow) && coord != Pair(exitCol, exitRow)) {
                    // avoid placing obstacles right on borders unless desired
                    if (r > 0 && r < gridSize - 1 && c > 0 && c < gridSize - 1) {
                        candidateObstacles.add(coord)
                    }
                }
            }
        }

        candidateObstacles.shuffle(rng)
        val obstacles = candidateObstacles.take(obstacleCount).map {
            Obstacle(col = it.first, row = it.second)
        }

        // Initial direction: first step of solution path
        val initialDir = if (solutionPath.size >= 2) {
            val next = solutionPath[1]
            when {
                next.first > startCol -> ArrowDirection.RIGHT
                next.first < startCol -> ArrowDirection.LEFT
                next.second > startRow -> ArrowDirection.DOWN
                else -> ArrowDirection.UP
            }
        } else {
            ArrowDirection.RIGHT
        }

        val startX = startCol + 0.5f
        val startY = startRow + 0.5f

        return ArrowFlowState(
            levelNumber = level,
            gridSize = gridSize,
            playerX = startX,
            playerY = startY,
            direction = initialDir,
            nextDirection = initialDir,
            speed = speed,
            obstacles = obstacles,
            exitGate = exitGate,
            startCol = startCol,
            startRow = startRow,
            solutionPath = solutionPath,
            difficultyTitle = difficultyTitle,
            snakeTrail = listOf(Pair(startX, startY)),
            isStarted = true,
            isPaused = false,
            isMoving = true,
            isCompleted = false,
            isFailed = false,
            movesCount = 0
        )
    }

    private fun carveSolutionPath(
        startC: Int,
        startR: Int,
        exitC: Int,
        exitR: Int,
        gridSize: Int,
        rng: Random
    ): List<Pair<Int, Int>> {
        val start = Pair(startC, startR)
        val exit = Pair(exitC, exitR)

        val queue = ArrayDeque<Pair<Int, Int>>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val parentMap = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if (curr == exit) break

            val neighbors = listOf(
                Pair(curr.first + 1, curr.second),
                Pair(curr.first - 1, curr.second),
                Pair(curr.first, curr.second + 1),
                Pair(curr.first, curr.second - 1)
            ).filter { (c, r) ->
                c in 0 until gridSize && r in 0 until gridSize && !visited.contains(Pair(c, r))
            }.shuffled(rng)

            for (nbr in neighbors) {
                visited.add(nbr)
                parentMap[nbr] = curr
                queue.add(nbr)
            }
        }

        val path = mutableListOf<Pair<Int, Int>>()
        var cur: Pair<Int, Int>? = exit
        while (cur != null) {
            path.add(0, cur)
            if (cur == start) break
            cur = parentMap[cur]
        }

        if (path.isEmpty() || path.first() != start) {
            // Fallback direct Manhattan path
            return generateDirectPath(start, exit)
        }
        return path
    }

    private fun generateDirectPath(start: Pair<Int, Int>, exit: Pair<Int, Int>): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        var c = start.first
        var r = start.second
        path.add(Pair(c, r))

        while (c != exit.first) {
            c += if (exit.first > c) 1 else -1
            path.add(Pair(c, r))
        }
        while (r != exit.second) {
            r += if (exit.second > r) 1 else -1
            path.add(Pair(c, r))
        }
        return path
    }

    /**
     * Real-time frame update: position += direction * speed * deltaTime
     * Continuous collision checking against obstacles, boundaries, and exit gate.
     * Updates snake trail for the glowing arrow body.
     */
    fun updateFrame(state: ArrowFlowState, dt: Float): ArrowFlowState {
        if (!state.isStarted || state.isPaused || !state.isMoving || state.isCompleted || state.isFailed) return state

        val dir = state.direction
        val newX = state.playerX + dir.dx * state.speed * dt
        val newY = state.playerY + dir.dy * state.speed * dt
        val r = state.playerRadius

        val updatedTrail = (listOf(Pair(newX, newY)) + state.snakeTrail).take(22)

        // 1. Exit gate arrival detection
        val exit = state.exitGate
        val exitCenterX = exit.col + 0.5f
        val exitCenterY = exit.row + 0.5f
        val distToExit = hypot(newX - exitCenterX, newY - exitCenterY)

        if (distToExit <= 0.48f) {
            return state.copy(
                playerX = exitCenterX,
                playerY = exitCenterY,
                snakeTrail = (listOf(Pair(exitCenterX, exitCenterY)) + updatedTrail).take(22),
                isMoving = false,
                isCompleted = true,
                isFailed = false
            )
        }

        // 2. Obstacle collision detection
        for (obs in state.obstacles) {
            val closestX = newX.coerceIn(obs.col.toFloat(), obs.col + obs.width)
            val closestY = newY.coerceIn(obs.row.toFloat(), obs.row + obs.height)
            val distX = newX - closestX
            val distY = newY - closestY
            if (distX * distX + distY * distY < (r * r)) {
                return state.copy(
                    playerX = newX,
                    playerY = newY,
                    snakeTrail = updatedTrail,
                    isMoving = false,
                    isFailed = true
                )
            }
        }

        // 3. Grid boundary collision detection
        val minBound = r
        val maxBound = state.gridSize - r

        if (newX < minBound || newX > maxBound || newY < minBound || newY > maxBound) {
            // Outside board without reaching the exit
            return state.copy(
                playerX = newX.coerceIn(0f, state.gridSize.toFloat()),
                playerY = newY.coerceIn(0f, state.gridSize.toFloat()),
                snakeTrail = updatedTrail,
                isMoving = false,
                isFailed = true
            )
        }

        // Safe continuous move
        return state.copy(
            playerX = newX,
            playerY = newY,
            snakeTrail = updatedTrail
        )
    }

    /**
     * Changes direction on swipe. Prevents instant 180-degree reverse.
     */
    fun changeDirection(state: ArrowFlowState, newDir: ArrowDirection): ArrowFlowState {
        if (!state.isStarted || state.isPaused || !state.isMoving || state.isCompleted || state.isFailed) return state
        if (newDir == state.direction.opposite() || newDir == state.direction) return state

        return state.copy(
            direction = newDir,
            nextDirection = newDir,
            movesCount = state.movesCount + 1
        )
    }

    fun togglePause(state: ArrowFlowState): ArrowFlowState {
        if (!state.isStarted || state.isCompleted || state.isFailed) return state
        val newPaused = !state.isPaused
        return state.copy(isPaused = newPaused, isMoving = !newPaused)
    }

    fun restartLevel(state: ArrowFlowState): ArrowFlowState {
        return generateLevel(state.levelNumber)
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
