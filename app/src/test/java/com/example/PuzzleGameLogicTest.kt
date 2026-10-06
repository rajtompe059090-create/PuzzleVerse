package com.example

import com.example.games.arrowflow.ArrowFlowLevelGenerator
import com.example.games.blockmerge.BlockMergeHelper
import com.example.games.blockmerge.SlideDirection
import com.example.games.colorpath.ColorPathGenerator
import com.example.games.tilematch.TileMatchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGameLogicTest {

    @Test
    fun testArrowFlowLevelGeneration() {
        val level1 = ArrowFlowLevelGenerator.generateLevel(1)
        assertEquals(4, level1.gridSize)
        assertTrue(level1.totalArrows > 0)
        assertTrue(level1.remainingMoves > level1.totalArrows)
        assertTrue(level1.cells.isNotEmpty())
    }

    @Test
    fun testBlockMergeTargetAndSlide() {
        val target = BlockMergeHelper.getTargetForLevel(1)
        assertEquals(64, target)

        // Test slide left
        val grid = Array(4) { IntArray(4) { 0 } }
        grid[0][1] = 2
        grid[0][2] = 2
        val result = BlockMergeHelper.slide(grid, SlideDirection.LEFT)
        assertTrue(result.moved)
        assertEquals(4, result.newGrid[0][0])
        assertEquals(4, result.scoreGained)
    }

    @Test
    fun testColorPathGeneration() {
        val level1 = ColorPathGenerator.generateLevel(1)
        assertEquals(4, level1.gridSize)
        assertTrue(level1.dots.isNotEmpty())
        for (dot in level1.dots) {
            assertTrue(dot.startRow in 0 until 4)
            assertTrue(dot.startCol in 0 until 4)
            assertTrue(dot.endRow in 0 until 4)
            assertTrue(dot.endCol in 0 until 4)
        }
    }

    @Test
    fun testTileMatchTriplets() {
        val level1 = TileMatchHelper.generateLevel(1)
        assertTrue(level1.totalTiles > 0)
        assertEquals(0, level1.totalTiles % 3) // Guaranteed triplets
    }
}
