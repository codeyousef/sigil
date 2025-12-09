package io.github.codeyousef.sigil.compose.applier

import io.materia.core.scene.Scene
import io.github.codeyousef.sigil.compose.node.MateriaNodeWrapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests for MateriaApplier class.
 */
class MateriaApplierTest {

    // ==================== Creation and Current Tests ====================

    @Test
    fun testApplierCreation() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)

        assertEquals(root, applier.current)
    }

    @Test
    fun testCurrentCanBeSet() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val newNode = MateriaNodeWrapper.createGroup()

        applier.current = newNode

        assertEquals(newNode, applier.current)
    }

    // ==================== Navigation Tests ====================

    @Test
    fun testDownNavigatesToChild() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child = MateriaNodeWrapper.createGroup()

        applier.down(child)

        assertEquals(child, applier.current)
    }

    @Test
    fun testUpReturnsToParent() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child = MateriaNodeWrapper.createGroup()

        applier.down(child)
        applier.up()

        assertEquals(root, applier.current)
    }

    @Test
    fun testNestedNavigation() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child = MateriaNodeWrapper.createGroup()
        val grandchild = MateriaNodeWrapper.createGroup()

        applier.down(child)
        assertEquals(child, applier.current)

        applier.down(grandchild)
        assertEquals(grandchild, applier.current)

        applier.up()
        assertEquals(child, applier.current)

        applier.up()
        assertEquals(root, applier.current)
    }

    @Test
    fun testUpFromRootThrows() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)

        assertFailsWith<IllegalArgumentException> {
            applier.up()
        }
    }

    // ==================== Insert Tests ====================

    @Test
    fun testInsertTopDown() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, child)

        assertEquals(1, root.childCount)
        assertEquals(child, root.childrenList[0])
    }

    @Test
    fun testInsertMultipleTopDown() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)
        applier.insertTopDown(2, child3)

        assertEquals(3, root.childCount)
        assertEquals(child1, root.childrenList[0])
        assertEquals(child2, root.childrenList[1])
        assertEquals(child3, root.childrenList[2])
    }

    @Test
    fun testInsertIntoNestedContext() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val group = MateriaNodeWrapper.createGroup()
        val child = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, group)
        applier.down(group)
        applier.insertTopDown(0, child)

        assertEquals(1, group.childCount)
        assertEquals(child, group.childrenList[0])
    }

    @Test
    fun testInsertBottomUpIsNoOp() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child = MateriaNodeWrapper.createGroup()

        // insertBottomUp should be no-op (insertion done in insertTopDown)
        applier.insertBottomUp(0, child)

        // Child should NOT be added (it's a no-op)
        assertEquals(0, root.childCount)
    }

    // ==================== Remove Tests ====================

    @Test
    fun testRemove() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)

        applier.remove(0, 1)

        assertEquals(1, root.childCount)
        assertEquals(child2, root.childrenList[0])
    }

    @Test
    fun testRemoveMultiple() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)
        applier.insertTopDown(2, child3)

        applier.remove(0, 2) // Remove first two

        assertEquals(1, root.childCount)
        assertEquals(child3, root.childrenList[0])
    }

    @Test
    fun testRemoveFromNestedContext() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val group = MateriaNodeWrapper.createGroup()
        val child = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, group)
        applier.down(group)
        applier.insertTopDown(0, child)
        applier.remove(0, 1)

        assertEquals(0, group.childCount)
    }

    // ==================== Move Tests ====================

    @Test
    fun testMove() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)
        applier.insertTopDown(2, child3)

        applier.move(0, 2, 1) // Move first to last

        assertEquals(3, root.childCount)
        assertEquals(child2, root.childrenList[0])
        assertEquals(child1, root.childrenList[1])
        assertEquals(child3, root.childrenList[2])
    }

    // ==================== Clear Tests ====================

    @Test
    fun testClear() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)

        applier.clear()

        assertEquals(0, root.childCount)
        assertEquals(root, applier.current) // Should reset to root
    }

    @Test
    fun testClearResetsNavigationStack() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)
        val group = MateriaNodeWrapper.createGroup()

        applier.insertTopDown(0, group)
        applier.down(group)

        // After clear, should be back at root
        applier.clear()

        assertEquals(root, applier.current)
    }

    // ==================== Lifecycle Tests ====================

    @Test
    fun testOnBeginChanges() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)

        // Should not throw
        applier.onBeginChanges()
    }

    @Test
    fun testOnEndChanges() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)

        // Should not throw
        applier.onEndChanges()
    }

    // ==================== Complex Scenario Tests ====================

    @Test
    fun testComplexSceneBuilding() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)

        // Build a scene with groups and nested children
        val group1 = MateriaNodeWrapper.createGroup()
        val group2 = MateriaNodeWrapper.createGroup()
        val child1a = MateriaNodeWrapper.createGroup()
        val child1b = MateriaNodeWrapper.createGroup()
        val child2a = MateriaNodeWrapper.createGroup()

        // Add group1 and its children
        applier.insertTopDown(0, group1)
        applier.down(group1)
        applier.insertTopDown(0, child1a)
        applier.insertTopDown(1, child1b)
        applier.up()

        // Add group2 and its children
        applier.insertTopDown(1, group2)
        applier.down(group2)
        applier.insertTopDown(0, child2a)
        applier.up()

        // Verify structure
        assertEquals(2, root.childCount)
        assertEquals(2, group1.childCount)
        assertEquals(1, group2.childCount)
    }

    @Test
    fun testRecompositionScenario() {
        val root = MateriaNodeWrapper.createRoot(Scene())
        val applier = MateriaApplier(root)

        // Initial composition
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        applier.onBeginChanges()
        applier.insertTopDown(0, child1)
        applier.insertTopDown(1, child2)
        applier.insertTopDown(2, child3)
        applier.onEndChanges()

        assertEquals(3, root.childCount)

        // Simulate recomposition: remove child2
        applier.onBeginChanges()
        applier.remove(1, 1)
        applier.onEndChanges()

        assertEquals(2, root.childCount)
        assertEquals(child1, root.childrenList[0])
        assertEquals(child3, root.childrenList[1])
    }
}
