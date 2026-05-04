package codes.yousef.sigil.summon.canvas

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object SigilSceneCallbackRegistry {
    private val callbacks = ConcurrentHashMap<String, () -> SigilSceneEventCallbackResponse?>()
    private val nextId = AtomicLong(1)

    fun registerCallback(callback: () -> SigilSceneEventCallbackResponse?): String {
        val id = "sigil-${nextId.getAndIncrement()}"
        callbacks[id] = callback
        return id
    }

    fun callbackPath(callbackId: String): String = "/sigil/callback/$callbackId"

    fun executeCallback(callbackId: String): SigilSceneCallbackResult {
        val callback = callbacks[callbackId]
            ?: return SigilSceneCallbackResult(
                found = false,
                statusCode = 404,
                response = SigilSceneEventCallbackResponse(action = "noop", status = "missing")
            )

        return try {
            SigilSceneCallbackResult(
                found = true,
                statusCode = 200,
                response = callback() ?: SigilSceneEventCallbackResponse(action = "noop", status = "ok")
            )
        } catch (_: Throwable) {
            SigilSceneCallbackResult(
                found = true,
                statusCode = 500,
                response = SigilSceneEventCallbackResponse(action = "noop", status = "error")
            )
        }
    }

    fun hasCallback(callbackId: String): Boolean = callbacks.containsKey(callbackId)

    fun clear() {
        callbacks.clear()
        nextId.set(1)
    }
}

data class SigilSceneCallbackResult(
    val found: Boolean,
    val statusCode: Int,
    val response: SigilSceneEventCallbackResponse
)
