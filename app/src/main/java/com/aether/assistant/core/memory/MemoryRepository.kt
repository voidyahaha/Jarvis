package com.aether.assistant.core.memory

import android.content.Context
import androidx.room.Room
import com.aether.assistant.core.util.AppTime
import kotlinx.coroutines.flow.Flow

/**
 * Local-only memory. Nothing here is uploaded. Entries flagged sensitive are
 * encrypted with the Keystore-backed CryptoBox before being written to SQLite.
 */
class MemoryRepository(context: Context) {
    private val crypto = CryptoBox(context.applicationContext)
    private val db = Room.databaseBuilder(
        context.applicationContext,
        MemoryDatabase::class.java,
        "aether_memory.db"
    ).build()

    private val dao = db.memoryDao()

    fun observeAll(): Flow<List<MemoryEntity>> = dao.observeAll()

    suspend fun search(query: String): List<MemoryEntity> = dao.search(query)

    suspend fun remember(title: String, content: String, sensitive: Boolean, source: String): Long {
        val stored = if (sensitive) crypto.encrypt(content).serialize() else content
        return dao.insert(
            MemoryEntity(
                title = title,
                content = stored,
                sensitive = sensitive,
                source = source,
                createdAt = AppTime.now()
            )
        )
    }

    suspend fun read(memory: MemoryEntity): String =
        if (memory.sensitive) crypto.decrypt(CryptoBox.EncryptedBlob.deserialize(memory.content))
        else memory.content

    suspend fun forget(memory: MemoryEntity) = dao.delete(memory)

    suspend fun forgetAll() = dao.deleteAll()
}
