package text.only.app

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "channels",
        foreignKeys = [ForeignKey(entity = Server::class,
                                  parentColumns = ["id"],
                                  childColumns = ["serverId"],
                                  onDelete = ForeignKey.CASCADE)],
        indices = [Index(value = ["serverId"])])
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serverId: Int, // Cheie externă către Server
    val name: String,
    val type: String // "TEXT" sau "VOICE"
)
