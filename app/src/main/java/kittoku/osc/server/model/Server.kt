package kittoku.osc.server.model

import com.google.gson.annotations.SerializedName

/**
 * Represents an SSTP server from the API
 */
data class Server(
    @SerializedName("hostname")
    val hostname: String,
    @SerializedName("ip")
    val ip: String,
    @SerializedName("score")
    val score: Long,
    @SerializedName("ping")
    val ping: Int,
    @SerializedName("speed")
    val speed: Long,
    @SerializedName("country_long")
    val countryLong: String,
    @SerializedName("country_short")
    val countryShort: String,
    @SerializedName("sessions")
    val sessions: Int,
    @SerializedName("uptime")
    val uptime: Long,
    @SerializedName("total_users")
    val totalUsers: Long,
    @SerializedName("total_traffic")
    val totalTraffic: String,
    @SerializedName("log_type")
    val logType: String,
    @SerializedName("operator")
    val operator: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("protocols")
    val protocols: Protocols
) {
    // Extract SSTP connection details
    val sstpHostname: String
        get() = protocols.sstp.hostname
    
    val sstpIp: String
        get() = protocols.sstp.ip
    
    val sstpPort: Int
        get() = protocols.sstp.port
    
    val isSstpAvailable: Boolean
        get() = protocols.sstp.available
    
    // Format speed in human-readable format
    val speedFormatted: String
        get() {
            return when {
                speed >= 1_000_000_000 -> "${"%.2f".format(speed / 1_000_000_000.0)} Gbps"
                speed >= 1_000_000 -> "${"%.2f".format(speed / 1_000_000.0)} Mbps"
                speed >= 1_000 -> "${"%.2f".format(speed / 1_000.0)} Kbps"
                else -> "$speed bps"
            }
        }
    
    // Format uptime in human-readable format
    val uptimeFormatted: String
        get() {
            val seconds = uptime
            val days = seconds / 86400
            val hours = (seconds % 86400) / 3600
            val minutes = (seconds % 3600) / 60
            
            return when {
                days > 0 -> "$days d, $hours h"
                hours > 0 -> "$hours h, $minutes m"
                else -> "$minutes m"
            }
        }
}

data class Protocols(
    @SerializedName("sstp")
    val sstp: SstpProtocol
)

data class SstpProtocol(
    @SerializedName("available")
    val available: Boolean,
    @SerializedName("ip")
    val ip: String,
    @SerializedName("hostname")
    val hostname: String,
    @SerializedName("port")
    val port: Int
)

/**
 * Represents a country with its servers
 */
data class Country(
    val name: String,
    val code: String,
    val servers: MutableList<Server> = mutableListOf(),
    var isExpanded: Boolean = false
) {
    val serverCount: Int
        get() = servers.size
    
    // Sort servers by score (higher is better)
    fun sortServers() {
        servers.sortByDescending { it.score }
    }
}

/**
 * Connection status for a server
 */
enum class ConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED,
    RETRYING
}

/**
 * Server with connection state
 */
data class ServerWithState(
    val server: Server,
    var connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    var attemptCount: Int = 0,
    var lastError: String? = null
)
