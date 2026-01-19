package text.only.app


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChannelDao {
    @Insert
    suspend fun insert(channel: ChannelEntity)

    @Query("SELECT * FROM channels WHERE serverId = :serverId ORDER BY name ASC")
    suspend fun getChannelsForServer(serverId: Int): List<ChannelEntity>
}