package codes.yousef.sigil.summon.canvas

import codes.yousef.sigil.schema.StorageBackend
import codes.yousef.sigil.schema.StoragePatch
import codes.yousef.sigil.schema.StoragePatchAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SigilPersistenceTest {
    @Test
    fun storageCommandsUseTheRequestedBackendAndRemoveValues() {
        val local = FakeStorageAdapter()
        val cookies = FakeStorageAdapter()
        val runtime = SigilPersistenceRuntime(local, cookies)

        runtime.apply(
            StoragePatch(
                action = StoragePatchAction.SET,
                key = "checkpoint",
                value = "bay-4",
                backend = StorageBackend.LOCAL_STORAGE,
                expiresDays = 30
            )
        )
        runtime.apply(
            StoragePatch(
                action = StoragePatchAction.SET,
                key = "sound",
                value = "low",
                backend = StorageBackend.COOKIE,
                expiresDays = 7
            )
        )

        assertEquals("bay-4", runtime.read("checkpoint"))
        assertEquals("low", runtime.read("sound", StorageBackend.COOKIE))
        assertEquals(7, cookies.expirations["sound"])

        runtime.apply(StoragePatch(StoragePatchAction.REMOVE, key = "checkpoint"))
        assertNull(runtime.read("checkpoint"))
    }

    private class FakeStorageAdapter : SigilStorageAdapter {
        val values = mutableMapOf<String, String>()
        val expirations = mutableMapOf<String, Int>()

        override fun read(key: String): String? = values[key]

        override fun write(key: String, value: String, expiresDays: Int) {
            values[key] = value
            expirations[key] = expiresDays
        }

        override fun remove(key: String) {
            values.remove(key)
            expirations.remove(key)
        }
    }
}
