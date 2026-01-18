package text.only.app

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM messages WHERE contextId = :contextId ORDER BY timestamp ASC")
    suspend fun getMessagesForContext(contextId: String): List<MessageEntity>
}
