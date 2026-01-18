package text.only.app

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations", indices = [Index(value = ["contactPhone"], unique = true)])
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val contactName: String,
    val contactPhone: String // Folosit ca identificator unic
)
