package com.example.games.heartmaze

import kotlin.math.hypot
import kotlin.random.Random

enum class MazeDirection(val dRow: Int, val dCol: Int, val dx: Float, val dy: Float) {
    UP(-1, 0, 0f, -1f),
    DOWN(1, 0, 0f, 1f),
    LEFT(0, -1, -1f, 0f),
    RIGHT(0, 1, 1f, 0f);

    fun opposite(): MazeDirection = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

data class HeartMazeState(
    val levelNumber: Int,
    val gridSize: Int,
    val startCell: Pair<Int, Int>,
    val finishCell: Pair<Int, Int>,
    val heartCells: Set<Pair<Int, Int>>,
    val pathCells: Set<Pair<Int, Int>>,
    val wallCells: Set<Pair<Int, Int>>,
    val obstacleCells: Set<Pair<Int, Int>>,
    val heartCollectibles: Set<Pair<Int, Int>>,
    val collectedHearts: Set<Pair<Int, Int>> = emptySet(),
    val solutionPath: List<Pair<Int, Int>>,
    val speed: Float, // grid units per second
    val difficultyTitle: String,
    // Dynamic continuous state
    val playerX: Float,
    val playerY: Float,
    val playerRadius: Float = 0.30f,
    val currentDirection: MazeDirection,
    val nextDirection: MazeDirection,
    val isMoving: Boolean = true,
    val isCompleted: Boolean = false,
    val isCollided: Boolean = false,
    val hintsUsed: Int = 0,
    val showHintPath: Boolean = false
) {
    val allCollectiblesGathered: Boolean
        get() = collectedHearts.containsAll(heartCollectibles)
}

object HeartMazeGenerator {

    /**
     * Mathematical check whether (r, c) is inside the heart shape
     */
    fun isInsideHeart(r: Int, c: Int, size: Int): Boolean {
        val cx = (size - 1) / 2.0
        val cy = (size - 1) / 2.0 - 0.3
        val scale = (size - 1) / 2.4

        val x = (c - cx) / scale
        val y = -(r - cy) / scale // invert Y for screen coordinates

        val x2 = x * x
        val y2 = y * y
        val term = x2 + y2 - 1.0
        return (term * term * term - x2 * y * y2) <= 0.08
    }

    fun generateLevel(level: Int): HeartMazeState {
        val (gridSize, speed, difficultyTitle) = when {
            level <= 25 -> Triple(7, 2.5f, "Easy")
            level <= 50 -> Triple(9, 3.2f, "Normal")
            level <= 75 -> Triple(9, 3.9f, "Hard")
            level <= 90 -> Triple(11, 4.6f, "Very Hard")
            else -> Triple(11, 5.2f, "Extreme")
        }

        val rng = Random(level * 4099 + 17)

        // 1. Identify all heart cells
        val heartCells = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (isInsideHeart(r, c, gridSize)) {
                    heartCells.add(Pair(r, c))
                }
            }
        }

        if (heartCells.isEmpty()) {
            for (r in 1 until gridSize - 1) {
                for (c in 1 until gridSize - 1) {
                    heartCells.add(Pair(r, c))
                }
            }
        }

        // 2. Start in top-left lobe, Finish at bottom tip of heart
        val centerCol = (gridSize - 1) / 2
        val finishCell = heartCells
            .filter { it.second in (centerCol - 1)..(centerCol + 1) }
            .maxByOrNull { it.first } ?: Pair(gridSize - 2, centerCol)

        val startCell = heartCells
            .filter { it.first in 1..(gridSize / 3) && it.second in 1 until centerCol }
            .minByOrNull { it.first + it.second } ?: Pair(1, 1)

        // 3. Guaranteed Solvable Path
        val solutionPath = carveGuaranteedPath(startCell, finishCell, heartCells, rng)

        // 4. Carve secondary paths/branches based on difficulty
        val pathCells = mutableSetOf<Pair<Int, Int>>()
        pathCells.addAll(solutionPath)

        val branchChance = when {
            level <= 25 -> 0.20f
            level <= 50 -> 0.40f
            level <= 75 -> 0.60f
            else -> 0.75f
        }

        val availableHeartCells = heartCells - pathCells
        val potentialBranches = availableHeartCells.shuffled(rng)
        for (cell in potentialBranches) {
            if (rng.nextFloat() < branchChance) {
                val adjacentPathCount = listOf(
                    Pair(cell.first - 1, cell.second),
                    Pair(cell.first + 1, cell.second),
                    Pair(cell.first, cell.second - 1),
                    Pair(cell.first, cell.second + 1)
                ).count { pathCells.contains(it) }

                if (adjacentPathCount == 1) {
                    pathCells.add(cell)
                }
            }
        }

        // 5. Walls are heart cells not part of path
        val wallCells = (heartCells - pathCells).toMutableSet()

        // 6. Obstacles in dead-ends (never in solution path)
        val obstacleCells = mutableSetOf<Pair<Int, Int>>()
        if (level >= 51) {
            val nonSolutionPathCells = (pathCells - solutionPath.toSet()).toList()
            val maxObs = if (level >= 76) 3 else 1
            val obsCount = maxObs.coerceAtMost(nonSolutionPathCells.size)
            if (obsCount > 0) {
                obstacleCells.addAll(nonSolutionPathCells.shuffled(rng).take(obsCount))
            }
        }

        // 7. Heart collectibles along the solution path
        val collectibleCount = when {
            level <= 25 -> 1
            level <= 60 -> 2
            else -> 3
        }
        val heartCollectibles = mutableSetOf<Pair<Int, Int>>()
        if (solutionPath.size > 3) {
            val intermediatePath = solutionPath.subList(1, solutionPath.size - 1)
            val step = (intermediatePath.size / (collectibleCount + 1)).coerceAtLeast(1)
            for (i in 1..collectibleCount) {
                val idx = (i * step).coerceAtMost(intermediatePath.size - 1)
                heartCollectibles.add(intermediatePath[idx])
            }
        }

        // Initial direction
        val initialDir = if (solutionPath.size >= 2) {
            val next = solutionPath[1]
            when {
                next.first > startCell.first -> MazeDirection.DOWN
                next.first < startCell.first -> MazeDirection.UP
                next.second > startCell.second -> MazeDirection.RIGHT
                else -> MazeDirection.LEFT
            }
        } else {
            MazeDirection.RIGHT
        }

        return HeartMazeState(
            levelNumber = level,
            gridSize = gridSize,
            startCell = startCell,
            finishCell = finishCell,
            heartCells = heartCells,
            pathCells = pathCells,
            wallCells = wallCells,
            obstacleCells = obstacleCells,
            heartCollectibles = heartCollectibles,
            collectedHearts = emptySet(),
            solutionPath = solutionPath,
            speed = speed,
            difficultyTitle = difficultyTitle,
            playerX = startCell.second + 0.5f,
            playerY = startCell.first + 0.5f,
            currentDirection = initialDir,
            nextDirection = initialDir,
            isMoving = true,
            isCompleted = false,
            isCollided = false
        )
    }

    private fun carveGuaranteedPath(
        start: Pair<Int, Int>,
        finish: Pair<Int, Int>,
        bounds: Set<Pair<Int, Int>>,
        rng: Random
    ): List<Pair<Int, Int>> {
        val queue = ArrayDeque<Pair<Int, Int>>()
        val visited = mutableSetOf<Pair<Int, Int>>()
        val parentMap = mutableMapOf<Pair<Int, Int>, Pair<Int, Int>>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == finish) break

            val neighbors = listOf(
                Pair(current.first - 1, current.second),
                Pair(current.first + 1, current.second),
                Pair(current.first, current.second - 1),
                Pair(current.first, current.second + 1)
            ).filter { bounds.contains(it) && !visited.contains(it) }
                .shuffled(rng)

            for (neighbor in neighbors) {
                visited.add(neighbor)
                parentMap[neighbor] = current
                queue.add(neighbor)
            }
        }

        val path = mutableListOf<Pair<Int, Int>>()
        var curr: Pair<Int, Int>? = finish
        while (curr != null) {
            path.add(0, curr)
            if (curr == start) break
            curr = parentMap[curr]
        }

        if (path.isEmpty() || path.first() != start) {
            return generateFallbackPath(start, finish)
        }
        return path
    }

    private fun generateFallbackPath(start: Pair<Int, Int>, finish: Pair<Int, Int>): List<Pair<Int, Int>> {
        val path = mutableListOf<Pair<Int, Int>>()
        var r = start.first
        var c = start.second
        path.add(Pair(r, c))

        while (r != finish.first || c != finish.second) {
            if (r < finish.first) r++
            else if (r > finish.first) r--
            else if (c < finish.second) c++
            else if (c > finish.second) c--
            path.add(Pair(r, c))
        }
        return path
    }

    /**
     * Continuous frame update for Heart Maze:
     * player position advances along current direction with speed * dt.
     * Collects hearts on overlap.
     * Win only if reached finish and all required collectibles gathered.
     */
    fun updateFrame(state: HeartMazeState, dt: Float): HeartMazeState {
        if (!state.isMoving || state.isCompleted) return state

        val dir = state.currentDirection
        val newX = state.playerX + dir.dx * state.speed * dt
        val newY = state.playerY + dir.dy * state.speed * dt
        val r = state.playerRadius

        // Current cell row and col
        val currentR = newY.toInt().coerceIn(0, state.gridSize - 1)
        val currentC = newX.toInt().coerceIn(0, state.gridSize - 1)
        val currentCoord = Pair(currentR, currentC)

        // 1. Collectible check
        val newCollected = if (state.heartCollectibles.contains(currentCoord) && !state.collectedHearts.contains(currentCoord)) {
            val dist = hypot(newX - (currentC + 0.5f), newY - (currentR + 0.5f))
            if (dist < 0.45f) state.collectedHearts + currentCoord else state.collectedHearts
        } else {
            state.collectedHearts
        }

        // 2. Goal completion check
        val finishX = state.finishCell.second + 0.5f
        val finishY = state.finishCell.first + 0.5f
        val distToGoal = hypot(newX - finishX, newY - finishY)

        if (distToGoal <= 0.42f && newCollected.containsAll(state.heartCollectibles)) {
            return state.copy(
                playerX = finishX,
                playerY = finishY,
                collectedHearts = newCollected,
                isMoving = false,
                isCompleted = true,
                isCollided = false
            )
        }

        // 3. Collision with walls or obstacles
        // A wall is an occupied cell not in pathCells, or cell outside heart
        val testX = newX + dir.dx * r
        val testY = newY + dir.dy * r
        val probeR = testY.toInt().coerceIn(0, state.gridSize - 1)
        val probeC = testX.toInt().coerceIn(0, state.gridSize - 1)
        val probeCoord = Pair(probeR, probeC)

        val isWallOrObstacle = !state.pathCells.contains(probeCoord) || state.obstacleCells.contains(probeCoord)

        if (isWallOrObstacle && (probeCoord != Pair(state.playerY.toInt(), state.playerX.toInt()))) {
            // Stop at border of wall
            return state.copy(
                isMoving = false,
                isCollided = true,
                collectedHearts = newCollected
            )
        }

        // Safe move
        return state.copy(
            playerX = newX,
            playerY = newY,
            collectedHearts = newCollected,
            isCollided = false
        )
    }

    fun changeDirection(state: HeartMazeState, newDir: MazeDirection): HeartMazeState {
        if (state.isCompleted) return state
        if (newDir == state.currentDirection.opposite()) return state

        return state.copy(
            currentDirection = newDir,
            nextDirection = newDir,
            isMoving = true,
            isCollided = false
        )
    }

    fun restartLevel(state: HeartMazeState): HeartMazeState {
        return generateLevel(state.levelNumber)
    }
}
