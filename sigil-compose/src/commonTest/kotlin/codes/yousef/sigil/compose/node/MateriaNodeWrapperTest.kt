package codes.yousef.sigil.compose.node

import io.materia.core.scene.Scene
import io.materia.core.scene.Group
import io.materia.core.scene.Object3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertNotNull

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
