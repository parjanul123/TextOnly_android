package text.only.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Un ID unic pentru conversație/canal, ex: "private_0722123456" sau "channel_12"
    val contextId: String, 
    
    val content: String,
    val isSent: Boolean,
    val timestamp: Long = System.currentTimeMillis()
    
    // TODO: Adaugă aici câmpuri pentru fișiere (fileName, price, remoteUrl etc.)
)
