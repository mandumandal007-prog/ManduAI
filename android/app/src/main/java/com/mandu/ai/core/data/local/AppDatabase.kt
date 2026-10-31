package com.mandu.ai.core.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :cid ORDER BY createdAt ASC")
    fun observe(cid: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :cid ORDER BY createdAt ASC")
    suspend fun list(cid: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversationId = :cid")
    suspend fun clear(cid: String)
}

@Database(entities = [ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class ManduDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        fun build(context: Context): ManduDatabase =
            Room.databaseBuilder(context, ManduDatabase::class.java, "mandu.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
