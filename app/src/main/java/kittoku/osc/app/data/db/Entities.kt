package kittoku.osc.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val hostname: String,
    val ip: String,
    val port: Int,
    val countryLong: String,
    val countryShort: String,
    val ping: Int,
    val speed: Long,
    val score: Long,
    val sessions: Int,
    val uptime: Long,
    val isFavorite: Boolean,
    val fetchedAt: Long,
)

@Entity(tableName = "custom_servers")
data class CustomServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hostname: String,
    val port: Int,
    val isFavorite: Boolean,
    val createdAt: Long,
)
