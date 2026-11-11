package kittoku.osc.app.data.api

import kotlinx.serialization.Serializable


@Serializable
internal data class ServersResponse(
    val success: Boolean = false,
    val source: String = "",
    val generated_at: String = "",
    val total_servers: Int = 0,
    val servers: List<ServerDto> = emptyList(),
)

@Serializable
internal data class ServerDto(
    val hostname: String = "",
    val ip: String = "",
    val score: Long = 0,
    val ping: Int = -1,
    val speed: Long = 0,
    val country_long: String = "",
    val country_short: String = "",
    val sessions: Int = 0,
    val uptime: Long = 0,
    val total_users: Long = 0,
    val total_traffic: String = "",
    val log_type: String = "",
    val operator: String = "",
    val message: String = "",
    val protocols: ProtocolsDto = ProtocolsDto(),
)

@Serializable
internal data class ProtocolsDto(
    val sstp: SstpDto? = null,
)

@Serializable
internal data class SstpDto(
    val available: Boolean = false,
    val ip: String = "",
    val hostname: String = "",
    val port: Int = 443,
)
