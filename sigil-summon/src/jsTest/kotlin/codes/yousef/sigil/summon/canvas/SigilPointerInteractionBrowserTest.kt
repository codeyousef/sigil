package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.CameraData
import codes.yousef.sigil.schema.ControlsData
import codes.yousef.sigil.schema.ControlsType
import codes.yousef.sigil.schema.DragMetadata
import codes.yousef.sigil.schema.DropTargetMetadata
import codes.yousef.sigil.schema.GeometryParams
import codes.yousef.sigil.schema.GeometryType
import codes.yousef.sigil.schema.HitVolumeData
import codes.yousef.sigil.schema.HitVolumeShape
import codes.yousef.sigil.schema.InteractionMetadata
import codes.yousef.sigil.schema.MeshData
import codes.yousef.sigil.schema.RendererPreference
import codes.yousef.sigil.schema.SceneSettings
import codes.yousef.sigil.schema.SigilScene
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private external interface BrowserSceneEventDetail {
    val type: String?
    val drag: BrowserSceneDragDetail?
}

private external interface BrowserSceneDragDetail {
    val result: String?
    val accepted: Boolean?
}

class SigilPointerInteractionBrowserTest {
    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun packagePointerOwnsCaptureUntilOutsideCoordinatePointerUp(): Promise<Unit> =
        GlobalScope.promise {
            if (!browserPointerEventsAvailable()) return@promise

            val harness = pointerHarness(usePointerCaptureShim = true)
            try {
                harness.hydrator.initialize()
                val pointerId = 4
                harness.canvas.dispatchEvent(pointerEvent("pointerdown", pointerId, "touch", 140, 90))

                assertTrue(harness.canvas.hasPointerCapture(pointerId))

                harness.canvas.dispatchEvent(
                    pointerEvent("pointerup", pointerId + 1, "touch", 400, 90, isPrimary = false)
                )
                assertTrue(harness.canvas.hasPointerCapture(pointerId))

                // Constructed pointer events cannot trigger the user agent's native capture
                // retargeting. Dispatching the outside-coordinate release on the canvas models
                // the event the browser sends to the capture owner; live-browser acceptance still
                // verifies physically leaving the canvas before release.
                harness.canvas.dispatchEvent(pointerEvent("pointerup", pointerId, "touch", 400, 90))
                assertFalse(harness.canvas.hasPointerCapture(pointerId))
            } finally {
                harness.dispose()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun mouseAndTouchPointersCompleteEquivalentCapturedDropSequences(): Promise<Unit> =
        GlobalScope.promise {
            if (!browserPointerEventsAvailable()) return@promise

            val harness = pointerHarness()
            val dragEnds = mutableListOf<Pair<String?, Boolean?>>()
            val eventTypes = mutableListOf<String>()
            val listener: (Event) -> Unit = { event ->
                val detail = event.asDynamic().detail.unsafeCast<BrowserSceneEventDetail>()
                val type = detail.type
                if (type != null) eventTypes += type
                if (type == "dragend") {
                    dragEnds += Pair(
                        detail.drag?.result,
                        detail.drag?.accepted
                    )
                }
            }
            harness.canvas.addEventListener("sigil:scene-event", listener)

            try {
                harness.hydrator.initialize()
                listOf("mouse" to 5, "touch" to 6).forEach { (pointerType, pointerId) ->
                    harness.canvas.dispatchEvent(pointerEvent("pointerdown", pointerId, pointerType, 140, 90))
                    harness.canvas.dispatchEvent(pointerEvent("pointermove", pointerId, pointerType, 181, 90))
                    // A constructed PointerEvent does not participate in the browser's native
                    // pointer-capture retargeting. Release on the canvas with an outside coordinate;
                    // capture itself is covered by the generated-runtime assertions.
                    harness.canvas.dispatchEvent(pointerEvent("pointerup", pointerId, pointerType, 400, 90))
                    harness.hydrator.applyPatch(
                        codes.yousef.sigil.schema.ScenePatch(
                            nodes = listOf(
                                codes.yousef.sigil.schema.SceneNodePatch(
                                    id = "draggable",
                                    position = listOf(-1f, 0f, 0f)
                                )
                            )
                        )
                    )
                }

                val expectedDragEnds: List<Pair<String?, Boolean?>> =
                    listOf(Pair("accepted", true), Pair("accepted", true))
                assertEquals(expectedDragEnds, dragEnds)
                assertEquals(2, eventTypes.count { it == "drop" })
                assertEquals(2, eventTypes.count { it == "dragend" })
            } finally {
                harness.canvas.removeEventListener("sigil:scene-event", listener)
                harness.dispose()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun secondaryPointerCannotStealPackageDragOrMoveOrbitCamera(): Promise<Unit> =
        GlobalScope.promise {
            if (!browserPointerEventsAvailable()) return@promise

            val harness = pointerHarness(includeOrbitControls = true)
            try {
                harness.hydrator.initialize()
                val cameraStart = requireNotNull(harness.hydrator.cameraPositionForTesting())
                harness.canvas.dispatchEvent(pointerEvent("pointerdown", 11, "touch", 140, 90))
                harness.canvas.dispatchEvent(
                    pointerEvent("pointerdown", 12, "touch", 250, 60, isPrimary = false)
                )
                harness.canvas.dispatchEvent(
                    pointerEvent("pointermove", 12, "touch", 300, 120, isPrimary = false)
                )
                harness.canvas.dispatchEvent(pointerEvent("pointermove", 11, "touch", 180, 90))

                val source = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))
                val cameraAfter = requireNotNull(harness.hydrator.cameraPositionForTesting())
                assertNotEquals(-1f, source.x)
                assertClose(cameraStart.x, cameraAfter.x)
                assertClose(cameraStart.y, cameraAfter.y)
                assertClose(cameraStart.z, cameraAfter.z)

                harness.canvas.dispatchEvent(pointerEvent("pointercancel", 11, "touch", 180, 90))
            } finally {
                harness.dispose()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun pointerCancellationRestoresSourceAndEmitsCancelledDragEndWithoutDrop(): Promise<Unit> =
        GlobalScope.promise {
            if (!browserPointerEventsAvailable()) return@promise

            val harness = pointerHarness()
            val eventTypes = mutableListOf<String>()
            val dragResults = mutableListOf<String?>()
            val listener: (Event) -> Unit = { event ->
                val detail = event.asDynamic().detail.unsafeCast<BrowserSceneEventDetail>()
                val type = detail.type
                if (type != null) eventTypes += type
                if (type == "dragend") dragResults += detail.drag?.result
            }
            harness.canvas.addEventListener("sigil:scene-event", listener)

            try {
                harness.hydrator.initialize()
                val start = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))
                harness.canvas.dispatchEvent(pointerEvent("pointerdown", 9, "touch", 140, 90))
                harness.canvas.dispatchEvent(pointerEvent("pointermove", 9, "touch", 180, 90))
                val moved = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))
                assertNotEquals(start.x, moved.x)

                harness.canvas.dispatchEvent(pointerEvent("pointercancel", 9, "touch", 180, 90))
                val restored = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))

                assertClose(start.x, restored.x)
                assertClose(start.y, restored.y)
                assertClose(start.z, restored.z)
                assertEquals(0, eventTypes.count { it == "drop" })
                assertEquals(1, eventTypes.count { it == "dragend" })
                assertEquals(listOf<String?>("cancelled"), dragResults)
                assertTrue("dragstart" in eventTypes)
            } finally {
                harness.canvas.removeEventListener("sigil:scene-event", listener)
                harness.dispose()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun lostPointerCaptureRestoresSourceAndEmitsCancelledDragEndWithoutDrop(): Promise<Unit> =
        GlobalScope.promise {
            if (!browserPointerEventsAvailable()) return@promise

            val harness = pointerHarness(usePointerCaptureShim = true)
            val eventTypes = mutableListOf<String>()
            val dragResults = mutableListOf<String?>()
            val listener: (Event) -> Unit = { event ->
                val detail = event.asDynamic().detail.unsafeCast<BrowserSceneEventDetail>()
                val type = detail.type
                if (type != null) eventTypes += type
                if (type == "dragend") dragResults += detail.drag?.result
            }
            harness.canvas.addEventListener("sigil:scene-event", listener)

            try {
                harness.hydrator.initialize()
                val pointerId = 19
                val start = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))
                harness.canvas.dispatchEvent(pointerEvent("pointerdown", pointerId, "touch", 140, 90))
                assertTrue(harness.canvas.hasPointerCapture(pointerId))
                harness.canvas.dispatchEvent(pointerEvent("pointermove", pointerId, "touch", 180, 90))
                val moved = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))
                assertNotEquals(start.x, moved.x)

                harness.canvas.dispatchEvent(pointerEvent("lostpointercapture", pointerId, "touch", 180, 90))
                val restored = requireNotNull(harness.hydrator.nodePositionForTesting("draggable"))

                assertClose(start.x, restored.x)
                assertClose(start.y, restored.y)
                assertClose(start.z, restored.z)
                assertFalse(harness.canvas.hasPointerCapture(pointerId))
                assertEquals(0, eventTypes.count { it == "drop" })
                assertEquals(1, eventTypes.count { it == "dragend" })
                assertEquals(listOf<String?>("cancelled"), dragResults)
            } finally {
                harness.canvas.removeEventListener("sigil:scene-event", listener)
                harness.dispose()
            }
        }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun oneFingerTouchOnBlankCanvasOrbitsCamera(): Promise<Unit> =
        GlobalScope.promise {
            if (!browserPointerEventsAvailable()) return@promise

            val harness = pointerHarness(
                includeOrbitControls = true,
                usePointerCaptureShim = true
            )
            try {
                harness.hydrator.initialize()
                harness.hydrator.startRenderLoop()
                val pointerId = 23
                val start = requireNotNull(harness.hydrator.cameraPositionForTesting())

                harness.canvas.dispatchEvent(pointerEvent("pointerdown", pointerId, "touch", 290, 30))
                assertTrue(harness.canvas.hasPointerCapture(pointerId))
                harness.canvas.dispatchEvent(pointerEvent("pointermove", pointerId, "touch", 220, 110))
                delay(50)
                harness.canvas.dispatchEvent(pointerEvent("pointerup", pointerId, "touch", 220, 110))

                val changed = requireNotNull(harness.hydrator.cameraPositionForTesting())
                assertTrue(
                    kotlin.math.abs(start.x - changed.x) +
                        kotlin.math.abs(start.y - changed.y) +
                        kotlin.math.abs(start.z - changed.z) > 0.001f,
                    "Expected a one-finger blank-canvas touch gesture to orbit the camera"
                )
                assertFalse(harness.canvas.hasPointerCapture(pointerId))
            } finally {
                harness.dispose()
            }
        }

    private fun pointerHarness(
        includeOrbitControls: Boolean = false,
        usePointerCaptureShim: Boolean = false
    ): PointerHarness {
        val host = document.createElement("div") as HTMLElement
        host.style.width = "320px"
        host.style.height = "180px"
        val canvas = document.createElement("canvas") as HTMLCanvasElement
        canvas.style.width = "100%"
        canvas.style.height = "100%"
        if (usePointerCaptureShim) installPointerCaptureShim(canvas)
        host.appendChild(canvas)
        document.body?.appendChild(host)

        val rootNodes = mutableListOf(
            CameraData(
                id = "camera",
                position = listOf(0f, 0f, 5f),
                lookAt = listOf(0f, 0f, 0f)
            ),
            MeshData(
                id = "draggable",
                position = listOf(-1f, 0f, 0f),
                geometryType = GeometryType.BOX,
                geometryParams = GeometryParams(width = 1f, height = 1f, depth = 1f),
                interaction = InteractionMetadata(
                    interactionId = "draggable",
                    hitVolume = HitVolumeData(shape = HitVolumeShape.BOX, size = listOf(1f, 1f, 1f)),
                    actions = listOf("package"),
                    drag = DragMetadata(dropGroups = listOf("routing"))
                )
            ),
            MeshData(
                id = "target",
                position = listOf(1f, 0f, 0f),
                geometryType = GeometryType.BOX,
                geometryParams = GeometryParams(width = 1f, height = 1f, depth = 1f),
                interaction = InteractionMetadata(
                    interactionId = "target",
                    hitVolume = HitVolumeData(shape = HitVolumeShape.BOX, size = listOf(1f, 1f, 1f)),
                    dropTarget = DropTargetMetadata(groups = listOf("routing"), accepts = listOf("package"))
                )
            )
        )
        if (includeOrbitControls) {
            rootNodes += ControlsData(
                id = "orbit",
                controlsType = ControlsType.ORBIT,
                target = listOf(0f, 0f, 0f),
                enableDamping = false
            )
        }
        return PointerHarness(
            host = host,
            canvas = canvas,
            hydrator = SigilHydrator(
                canvas,
                SigilScene(rootNodes = rootNodes, settings = SceneSettings(rendererPreference = RendererPreference.WEBGL))
            )
        )
    }

    private fun installPointerCaptureShim(canvas: HTMLCanvasElement) {
        var capturedPointerId: Int? = null
        canvas.asDynamic().setPointerCapture = { pointerId: Int ->
            capturedPointerId = pointerId
        }
        canvas.asDynamic().hasPointerCapture = { pointerId: Int ->
            capturedPointerId == pointerId
        }
        canvas.asDynamic().releasePointerCapture = { pointerId: Int ->
            if (capturedPointerId == pointerId) capturedPointerId = null
        }
    }

    private fun pointerEvent(
        type: String,
        pointerId: Int,
        pointerType: String,
        clientX: Int,
        clientY: Int,
        isPrimary: Boolean = true
    ): Event {
        val init = js("({ bubbles: true, cancelable: true })")
        init.pointerId = pointerId
        init.pointerType = pointerType
        init.isPrimary = isPrimary
        init.button = 0
        init.buttons = if (
            type == "pointerup" || type == "pointercancel" || type == "lostpointercapture"
        ) {
            0
        } else {
            1
        }
        init.clientX = clientX
        init.clientY = clientY
        return js("new PointerEvent(type, init)").unsafeCast<Event>()
    }

    private fun browserPointerEventsAvailable(): Boolean = js(
        "typeof document !== 'undefined' && typeof PointerEvent !== 'undefined' && typeof WebGLRenderingContext !== 'undefined'"
    ) as Boolean

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(kotlin.math.abs(expected - actual) < 0.001f, "Expected $expected, got $actual")
    }

    private data class PointerHarness(
        val host: HTMLElement,
        val canvas: HTMLCanvasElement,
        val hydrator: SigilHydrator
    ) {
        fun dispose() {
            hydrator.dispose()
            host.remove()
        }
    }
}
