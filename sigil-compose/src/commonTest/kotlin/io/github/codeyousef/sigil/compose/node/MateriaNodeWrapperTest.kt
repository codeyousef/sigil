package io.github.codeyousef.sigil.compose.node

import io.materia.core.scene.Scene
import io.materia.core.scene.Group
import io.materia.core.scene.Object3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

/**
 * Tests for MateriaNodeWrapper class.
 */
class MateriaNodeWrapperTest {

    // ==================== Creation Tests ====================

    @Test
    fun testCreateRootWithScene() {
        val scene = Scene()
        val wrapper = MateriaNodeWrapper.createRoot(scene)

        assertEquals(scene, wrapper.internalNode)
        assertNull(wrapper.parent)
        assertEquals(0, wrapper.childCount)
    }

    @Test
    fun testCreateGroup() {
        val wrapper = MateriaNodeWrapper.createGroup()

        assertTrue(wrapper.internalNode is Group)
        assertNull(wrapper.parent)
        assertEquals(0, wrapper.childCount)
    }

    @Test
    fun testCreateWithCustomObject3D() {
        val obj = Group()
        obj.name = "TestObject"
        val wrapper = MateriaNodeWrapper(obj)

        assertEquals(obj, wrapper.internalNode)
        assertEquals("TestObject", wrapper.internalNode.name)
    }

    // ==================== Insert Tests ====================

    @Test
    fun testInsertSingleChild() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        parent.insert(0, child)

        assertEquals(1, parent.childCount)
        assertEquals(parent, child.parent)
        assertEquals(child, parent.childrenList[0])
    }

    @Test
    fun testInsertMultipleChildrenInOrder() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)

        assertEquals(3, parent.childCount)
        assertEquals(child1, parent.childrenList[0])
        assertEquals(child2, parent.childrenList[1])
        assertEquals(child3, parent.childrenList[2])
    }

    @Test
    fun testInsertAtBeginning() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val existing = MateriaNodeWrapper.createGroup()
        val newChild = MateriaNodeWrapper.createGroup()

        parent.insert(0, existing)
        parent.insert(0, newChild) // Insert at beginning

        assertEquals(2, parent.childCount)
        assertEquals(newChild, parent.childrenList[0])
        assertEquals(existing, parent.childrenList[1])
    }

    @Test
    fun testInsertAtMiddle() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val middle = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(1, middle) // Insert in middle

        assertEquals(3, parent.childCount)
        assertEquals(child1, parent.childrenList[0])
        assertEquals(middle, parent.childrenList[1])
        assertEquals(child2, parent.childrenList[2])
    }

    @Test
    fun testInsertReparentsChild() {
        val parent1 = MateriaNodeWrapper.createRoot(Scene())
        val parent2 = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        parent1.insert(0, child)
        assertEquals(parent1, child.parent)
        assertEquals(1, parent1.childCount)

        parent2.insert(0, child) // Should remove from parent1
        assertEquals(parent2, child.parent)
        assertEquals(0, parent1.childCount)
        assertEquals(1, parent2.childCount)
    }

    @Test
    fun testInsertOutOfBoundsThrows() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        assertFailsWith<IllegalArgumentException> {
            parent.insert(5, child) // Index out of bounds
        }
    }

    @Test
    fun testInsertNegativeIndexThrows() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        assertFailsWith<IllegalArgumentException> {
            parent.insert(-1, child)
        }
    }

    // ==================== Remove Tests ====================

    @Test
    fun testRemoveSingleChild() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        parent.insert(0, child)
        parent.remove(0, 1)

        assertEquals(0, parent.childCount)
        assertNull(child.parent)
    }

    @Test
    fun testRemoveMultipleChildren() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)

        parent.remove(0, 2) // Remove first two

        assertEquals(1, parent.childCount)
        assertEquals(child3, parent.childrenList[0])
        assertNull(child1.parent)
        assertNull(child2.parent)
    }

    @Test
    fun testRemoveFromMiddle() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)

        parent.remove(1, 1) // Remove middle

        assertEquals(2, parent.childCount)
        assertEquals(child1, parent.childrenList[0])
        assertEquals(child3, parent.childrenList[1])
    }

    @Test
    fun testRemoveByReference() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)

        parent.remove(child1)

        assertEquals(1, parent.childCount)
        assertEquals(child2, parent.childrenList[0])
        assertNull(child1.parent)
    }

    @Test
    fun testRemoveNonExistentChildByReference() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        // Should not throw, just do nothing
        parent.remove(child)

        assertEquals(0, parent.childCount)
    }

    @Test
    fun testRemoveOutOfBoundsThrows() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()
        parent.insert(0, child)

        assertFailsWith<IllegalArgumentException> {
            parent.remove(5, 1)
        }
    }

    // ==================== Move Tests ====================

    @Test
    fun testMoveForward() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)

        parent.move(0, 2, 1) // Move child1 from 0 to position 2

        assertEquals(3, parent.childCount)
        assertEquals(child2, parent.childrenList[0])
        assertEquals(child1, parent.childrenList[1])
        assertEquals(child3, parent.childrenList[2])
    }

    @Test
    fun testMoveBackward() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)

        parent.move(2, 0, 1) // Move child3 from 2 to position 0

        assertEquals(3, parent.childCount)
        assertEquals(child3, parent.childrenList[0])
        assertEquals(child1, parent.childrenList[1])
        assertEquals(child2, parent.childrenList[2])
    }

    @Test
    fun testMoveMultiple() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()
        val child4 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)
        parent.insert(3, child4)

        // Initial: [child1, child2, child3, child4]
        // Move 2 items from index 2 to index 0 (move child3,child4 to beginning)
        parent.move(2, 0, 2)

        assertEquals(4, parent.childCount)
        assertEquals(child3, parent.childrenList[0])
        assertEquals(child4, parent.childrenList[1])
        assertEquals(child1, parent.childrenList[2])
        assertEquals(child2, parent.childrenList[3])
    }

    @Test
    fun testMoveSamePosition() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)

        parent.move(0, 0, 1) // Move to same position

        // Should be unchanged
        assertEquals(child1, parent.childrenList[0])
        assertEquals(child2, parent.childrenList[1])
    }

    // ==================== Clear Tests ====================

    @Test
    fun testClearRemovesAllChildren() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        val child3 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)
        parent.insert(2, child3)

        parent.clear()

        assertEquals(0, parent.childCount)
        assertNull(child1.parent)
        assertNull(child2.parent)
        assertNull(child3.parent)
    }

    @Test
    fun testClearDisposesChildren() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()
        parent.insert(0, child)

        parent.clear()

        assertTrue(child.isDisposed)
    }

    @Test
    fun testClearRecursive() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()
        val grandchild = MateriaNodeWrapper.createGroup()

        parent.insert(0, child)
        child.insert(0, grandchild)

        parent.clear()

        assertTrue(child.isDisposed)
        assertTrue(grandchild.isDisposed)
    }

    // ==================== Property Tests ====================

    @Test
    fun testSetPosition() {
        val wrapper = MateriaNodeWrapper.createGroup()

        wrapper.setPosition(1f, 2f, 3f)

        assertEquals(1f, wrapper.internalNode.position.x)
        assertEquals(2f, wrapper.internalNode.position.y)
        assertEquals(3f, wrapper.internalNode.position.z)
    }

    @Test
    fun testSetRotation() {
        val wrapper = MateriaNodeWrapper.createGroup()

        wrapper.setRotation(0.1f, 0.2f, 0.3f)

        assertEquals(0.1f, wrapper.internalNode.rotation.x)
        assertEquals(0.2f, wrapper.internalNode.rotation.y)
        assertEquals(0.3f, wrapper.internalNode.rotation.z)
    }

    @Test
    fun testSetScale() {
        val wrapper = MateriaNodeWrapper.createGroup()

        wrapper.setScale(2f, 3f, 4f)

        assertEquals(2f, wrapper.internalNode.scale.x)
        assertEquals(3f, wrapper.internalNode.scale.y)
        assertEquals(4f, wrapper.internalNode.scale.z)
    }

    @Test
    fun testSetVisible() {
        val wrapper = MateriaNodeWrapper.createGroup()

        wrapper.setVisible(false)
        assertFalse(wrapper.internalNode.visible)

        wrapper.setVisible(true)
        assertTrue(wrapper.internalNode.visible)
    }

    @Test
    fun testSetName() {
        val wrapper = MateriaNodeWrapper.createGroup()

        wrapper.setName("MyNode")

        assertEquals("MyNode", wrapper.internalNode.name)
    }

    // ==================== Lookup Tests ====================

    @Test
    fun testGetChildAt() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()

        parent.insert(0, child1)
        parent.insert(1, child2)

        assertEquals(child1, parent.getChildAt(0))
        assertEquals(child2, parent.getChildAt(1))
        assertNull(parent.getChildAt(5))
    }

    @Test
    fun testFindChild() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child1 = MateriaNodeWrapper.createGroup()
        val child2 = MateriaNodeWrapper.createGroup()
        child1.setName("Target")
        child2.setName("Other")

        parent.insert(0, child1)
        parent.insert(1, child2)

        val found = parent.findChild { it.internalNode.name == "Target" }
        assertEquals(child1, found)

        val notFound = parent.findChild { it.internalNode.name == "NonExistent" }
        assertNull(notFound)
    }

    // ==================== Dispose Tests ====================

    @Test
    fun testDispose() {
        val wrapper = MateriaNodeWrapper.createGroup()

        assertFalse(wrapper.isDisposed)

        wrapper.dispose()

        assertTrue(wrapper.isDisposed)
    }

    @Test
    fun testDisposeIsIdempotent() {
        val wrapper = MateriaNodeWrapper.createGroup()

        wrapper.dispose()
        wrapper.dispose() // Should not throw

        assertTrue(wrapper.isDisposed)
    }

    @Test
    fun testDisposeRemovesFromParent() {
        val parent = MateriaNodeWrapper.createRoot(Scene())
        val child = MateriaNodeWrapper.createGroup()

        parent.insert(0, child)
        child.dispose()

        assertNull(child.parent)
        assertTrue(child.isDisposed)
    }

    @Test
    fun testDisposeRecursive() {
        val parent = MateriaNodeWrapper.createGroup()
        val child = MateriaNodeWrapper.createGroup()
        val grandchild = MateriaNodeWrapper.createGroup()

        parent.insert(0, child)
        child.insert(0, grandchild)

        parent.dispose()

        assertTrue(parent.isDisposed)
        assertTrue(child.isDisposed)
        assertTrue(grandchild.isDisposed)
    }

    // ==================== toString Tests ====================

    @Test
    fun testToString() {
        val wrapper = MateriaNodeWrapper.createGroup()
        wrapper.setName("TestGroup")

        val str = wrapper.toString()

        assertTrue(str.contains("Group"))
        assertTrue(str.contains("TestGroup"))
    }
}
