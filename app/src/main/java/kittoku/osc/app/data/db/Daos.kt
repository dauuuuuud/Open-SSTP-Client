package kittoku.osc.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow


@Dao
internal interface ServerDao {
    @Query("SELECT * FROM servers ORDER BY score DESC")
    fun observeAll(): Flow<List<ServerEntity>>

    @Upsert
    suspend fun upsertAll(servers: List<ServerEntity>)

    @Query("DELETE FROM servers WHERE isFavorite = 0 AND fetchedAt < :threshold")
    suspend fun deleteStale(threshold: Long)

    @Query("UPDATE servers SET isFavorite = :favorite WHERE hostname = :hostname")
    suspend fun setFavorite(hostname: String, favorite: Boolean)

    @Query("SELECT COUNT(*) FROM servers")
    suspend fun count(): Int
}

@Dao
internal interface CustomServerDao {
    @Query("SELECT * FROM custom_servers ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CustomServerEntity>>

    @Insert
    suspend fun insert(server: CustomServerEntity): Long

    @Query("DELETE FROM custom_servers WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE custom_servers SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)
}
