package text.only.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String
    // Poți adăuga ulterior:
    // val iconUrl: String?
)
