package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.StorageBackend
import codes.yousef.sigil.schema.StoragePatch
import codes.yousef.sigil.schema.StoragePatchAction
import kotlinx.browser.document
import kotlinx.browser.window

internal interface SigilStorageAdapter {
    fun read(key: String): String?
    fun write(key: String, value: String, expiresDays: Int)
    fun remove(key: String)
}

internal class SigilPersistenceRuntime(
    private val localStorage: SigilStorageAdapter,
    private val cookies: SigilStorageAdapter
) {
    fun read(key: String, backend: StorageBackend = StorageBackend.LOCAL_STORAGE): String? =
        adapter(backend).read(key)

    fun apply(patch: StoragePatch) {
        val storage = adapter(patch.backend)
        when (patch.action) {
            StoragePatchAction.SET -> storage.write(patch.key, requireNotNull(patch.value), patch.expiresDays)
            StoragePatchAction.REMOVE -> storage.remove(patch.key)
        }
    }

    private fun adapter(backend: StorageBackend): SigilStorageAdapter = when (backend) {
        StorageBackend.LOCAL_STORAGE -> localStorage
        StorageBackend.COOKIE -> cookies
    }

    companion object {
        fun browser(): SigilPersistenceRuntime = SigilPersistenceRuntime(
            localStorage = BrowserLocalStorageAdapter,
            cookies = BrowserCookieStorageAdapter
        )
    }
}

private object BrowserLocalStorageAdapter : SigilStorageAdapter {
    override fun read(key: String): String? = try {
        window.localStorage.getItem(key)
    } catch (_: Throwable) {
        null
    }

    override fun write(key: String, value: String, expiresDays: Int) {
        try {
            window.localStorage.setItem(key, value)
        } catch (_: Throwable) {
        }
    }

    override fun remove(key: String) {
        try {
            window.localStorage.removeItem(key)
        } catch (_: Throwable) {
        }
    }
}

private object BrowserCookieStorageAdapter : SigilStorageAdapter {
    override fun read(key: String): String? {
        val encodedKey = encodeCookieComponent(key)
        return document.cookie
            .split(';')
            .asSequence()
            .map(String::trim)
            .firstOrNull { it.startsWith("$encodedKey=") }
            ?.substringAfter('=')
            ?.let(::decodeCookieComponent)
    }

    override fun write(key: String, value: String, expiresDays: Int) {
        val maxAgeSeconds = expiresDays * 24 * 60 * 60
        document.cookie = "${encodeCookieComponent(key)}=${encodeCookieComponent(value)}; path=/; max-age=$maxAgeSeconds; SameSite=Lax"
    }

    override fun remove(key: String) {
        document.cookie = "${encodeCookieComponent(key)}=; path=/; max-age=0; SameSite=Lax"
    }
}

@Suppress("UnsafeCastFromDynamic")
private fun encodeCookieComponent(value: String): String = js("encodeURIComponent(value)") as String

@Suppress("UnsafeCastFromDynamic")
private fun decodeCookieComponent(value: String): String = js("decodeURIComponent(value)") as String
