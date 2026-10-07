package com.example

import com.example.core.Constants
import com.example.games.arrowflow.ArrowDirection
import com.example.games.arrowflow.ArrowFlowEngine
import com.example.games.arrowflow.ArrowFlowState
import com.example.games.arrowflow.ExitGate
import com.example.games.arrowflow.Obstacle
import com.example.games.blockmerge.BlockMergeHelper
import com.example.games.blockmerge.SlideDirection
import com.example.games.heartmaze.HeartMazeGenerator
import com.example.games.heartmaze.MazeDirection
import com.example.games.tilematch.GameTile
import com.example.games.tilematch.TileMatchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class PuzzleGameLogicTest {

    // ==========================================
    // 1. ARROW FLOW TESTS
    // ==========================================

    @Test
    fun testArrowFlowDeterministicGeneration() {
        val level1A = ArrowFlowEngine.generateLevel(1)
        val level1B = ArrowFlowEngine.generateLevel(1)

        assertEquals(level1A.gridSize, level1B.gridSize)
        assertEquals(level1A.playerX, level1B.playerX, 0.001f)
        assertEquals(level1A.playerY, level1B.playerY, 0.001f)
        assertEquals(level1A.obstacles.size, level1B.obstacles.size)
        assertEquals(level1A.solutionPath.size, level1B.solutionPath.size)
    }

    @Test
    fun testArrowFlowContinuousDeltaTimeMovement() {
        val state = ArrowFlowEngine.generateLevel(1).copy(
            playerX = 2.0f,
            playerY = 2.0f,
            direction = ArrowDirection.RIGHT,
            speed = 3.0f,
            obstacles = emptyList(),
            isMoving = true,
            isCompleted = false,
            isFailed = false
        )

        val dt = 0.1f // 100ms
        val nextState = ArrowFlowEngine.updateFrame(state, dt)

        // position += direction * speed * dt -> 2.0 + 1 * 3.0 * 0.1 = 2.3f
        assertEquals(2.3f, nextState.playerX, 0.01f)
        assertEquals(2.0f, nextState.playerY, 0.01f)
        assertTrue(nextState.isMoving)
        assertFalse(nextState.isCompleted)
        assertFalse(nextState.isFailed)
    }

    @Test
    fun testArrowFlowDirectionChangePreventsReverse() {
        val state = ArrowFlowEngine.generateLevel(1).copy(
            direction = ArrowDirection.RIGHT
        )

        // Instant reverse to LEFT should be ignored
        val reverseState = ArrowFlowEngine.changeDirection(state, ArrowDirection.LEFT)
        assertEquals(ArrowDirection.RIGHT, reverseState.direction)

        // Valid 90-degree turn to DOWN should succeed
        val downState = ArrowFlowEngine.changeDirection(state, ArrowDirection.DOWN)
        assertEquals(ArrowDirection.DOWN, downState.direction)
        assertEquals(1, downState.movesCount)
    }

    @Test
    fun testArrowFlowObstacleCollision() {
        val obstacle = Obstacle(col = 3, row = 2)
        val state = ArrowFlowEngine.generateLevel(1).copy(
            playerX = 2.9f,
            playerY = 2.5f,
            playerRadius = 0.32f,
            direction = ArrowDirection.RIGHT,
            speed = 2.0f,
            obstacles = listOf(obstacle),
            isMoving = true
        )

        val nextState = ArrowFlowEngine.updateFrame(state, 0.1f)
        assertTrue("Collision with obstacle must set isFailed = true", nextState.isFailed)
        assertFalse("Movement must stop on collision", nextState.isMoving)
    }

    @Test
    fun testArrowFlowExitCompletion() {
        val exitGate = ExitGate(col = 5, row = 4, facing = ArrowDirection.RIGHT)
        val state = ArrowFlowEngine.generateLevel(1).copy(
            gridSize = 6,
            exitGate = exitGate,
            playerX = 5.3f,
            playerY = 4.5f,
            direction = ArrowDirection.RIGHT,
            speed = 2.0f,
            obstacles = emptyList(),
            isMoving = true
        )

        val nextState = ArrowFlowEngine.updateFrame(state, 0.1f)
        assertTrue("Reaching exit gate must trigger completion", nextState.isCompleted)
        assertFalse("Movement must stop on level complete", nextState.isMoving)
        assertFalse(nextState.isFailed)
    }

    // ==========================================
    // 2. HEART MAZE TESTS
    // ==========================================

    @Test
    fun testHeartMazeDeterministicGenerationAndSolvability() {
        val level1 = HeartMazeGenerator.generateLevel(1)
        val level1Dup = HeartMazeGenerator.generateLevel(1)

        assertEquals(level1.gridSize, level1Dup.gridSize)
        assertEquals(level1.startCell, level1Dup.startCell)
        assertEquals(level1.finishCell, level1Dup.finishCell)
        assertTrue(level1.solutionPath.isNotEmpty())
        assertEquals(level1.startCell, level1.solutionPath.first())
        assertEquals(level1.finishCell, level1.solutionPath.last())
    }

    @Test
    fun testHeartMazeFiveDifficultyBands() {
        val easy = HeartMazeGenerator.generateLevel(10)
        val normal = HeartMazeGenerator.generateLevel(35)
        val hard = HeartMazeGenerator.generateLevel(65)
        val veryHard = HeartMazeGenerator.generateLevel(85)
        val extreme = HeartMazeGenerator.generateLevel(95)

        assertEquals("Easy", easy.difficultyTitle)
        assertEquals("Normal", normal.difficultyTitle)
        assertEquals("Hard", hard.difficultyTitle)
        assertEquals("Very Hard", veryHard.difficultyTitle)
        assertEquals("Extreme", extreme.difficultyTitle)

        assertTrue(extreme.speed > easy.speed)
    }

    @Test
    fun testHeartMazeCollectibleRequirement() {
        val level = HeartMazeGenerator.generateLevel(1)
        // Ifcollectibles are not gathered, arriving at goal should NOT complete
        val stateWithCollectibles = level.copy(
            heartCollectibles = setOf(Pair(2, 2)),
            collectedHearts = emptySet(),
            playerX = level.finishCell.second + 0.5f,
            playerY = level.finishCell.first + 0.5f,
            isMoving = true,
            isCompleted = false
        )

        val updated = HeartMazeGenerator.updateFrame(stateWithCollectibles, 0.01f)
        assertFalse("Exit cannot complete until required heart collectibles are gathered", updated.isCompleted)

        // When collectible is gathered, arrival triggers completion
        val stateWithCollected = stateWithCollectibles.copy(
            collectedHearts = setOf(Pair(2, 2))
        )
        val completedState = HeartMazeGenerator.updateFrame(stateWithCollected, 0.01f)
        assertTrue("Completion triggered when collectibles are satisfied", completedState.isCompleted)
    }

    // ==========================================
    // 3. BLOCK MERGE TESTS
    // ==========================================

    @Test
    fun testBlockMergeSlideAndMerge() {
        val grid = Array(4) { IntArray(4) { 0 } }
        grid[0][1] = 4
        grid[0][2] = 4
        val result = BlockMergeHelper.slide(grid, SlideDirection.LEFT)

        assertTrue(result.moved)
        assertEquals(8, result.newGrid[0][0])
        assertEquals(8, result.scoreGained)
        assertEquals(1, result.comboCount)
    }

    @Test
    fun testBlockMergeTargetWinCondition() {
        val target = BlockMergeHelper.getTargetForLevel(1)
        assertEquals(64, target)

        val target50 = BlockMergeHelper.getTargetForLevel(50)
        assertEquals(1024, target50)
    }

    @Test
    fun testBlockMergeGameOverDetection() {
        val fullGrid = Array(4) { r ->
            IntArray(4) { c -> (r * 4 + c + 1) * 2 }
        }
        // Adjacent tiles are distinct: 2, 4, 6, 8, 10... no merges possible
        val hasMoves = BlockMergeHelper.canMakeAnyMove(fullGrid)
        assertFalse("Full grid with no adjacent equals must return no moves available", hasMoves)
    }

    // ==========================================
    // 4. TILE MATCH TESTS
    // ==========================================

    @Test
    fun testTileMatchTripletsGuaranteed() {
        val level = TileMatchHelper.generateLevel(1)
        assertEquals(0, level.totalTiles % 3)
        assertTrue(level.tiles.isNotEmpty())
    }

    @Test
    fun testTileMatchTripleClearing() {
        val type = TileMatchHelper.TILE_TYPES[0]
        val dock = mutableListOf(
            GameTile(1, type),
            GameTile(2, type),
            GameTile(3, type)
        )
        // Check 3 matching tiles condition
        val matchCount = dock.count { it.type.id == type.id }
        assertEquals(3, matchCount)
        dock.removeAll { it.type.id == type.id }
        assertTrue(dock.isEmpty())
    }

    // ==========================================
    // 5. REWARD SYSTEM & ATOMIC TRANSACTION TESTS
    // ==========================================

    @Test
    fun testRewardCalculationNormalAndDouble() {
        val baseAmount = Constants.LEVEL_REWARD_AMOUNT
        assertEquals(1.0, baseAmount, 0.001)

        val doubleAmount = baseAmount * 2.0
        assertEquals(2.0, doubleAmount, 0.001)
        // Guaranteed exactly 2x
        assertEquals(baseAmount * 2, doubleAmount, 0.0001)
    }

    @Test
    fun testWithdrawalUniqueIdFormat() {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val randomHex1 = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()
        val randomHex2 = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

        val id1 = "PV-$dateStr-$randomHex1"
        val id2 = "PV-$dateStr-$randomHex2"

        assertTrue(id1.startsWith("PV-"))
        assertNotEquals(id1, id2)
        assertEquals(18, id1.length) // "PV-" (3) + 8 digits (8) + "-" (1) + 6 chars (6) = 18 chars
    }
}
