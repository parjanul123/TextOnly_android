package text.only.app

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConversationDao {
    // onConflict = IGNORE previne adăugarea unui duplicat (bazat pe `contactPhone` care e unic)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(conversation: ConversationEntity)

    @Delete
    suspend fun delete(conversation: ConversationEntity)

    @Query("SELECT * FROM conversations ORDER BY contactName ASC")
    suspend fun getAllConversations(): List<ConversationEntity>
}
