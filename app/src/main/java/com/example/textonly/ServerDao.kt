package text.only.app

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ServerDao {

    // `suspend` înseamnă că această funcție trebuie apelată dintr-o corutină (un thread secundar)
    // pentru a nu bloca interfața grafică.
    
    @Insert
    suspend fun insert(server: Server)

    @Delete
    suspend fun delete(server: Server)

    @Query("SELECT * FROM servers ORDER BY name ASC")
    suspend fun getAllServers(): List<Server>
}
