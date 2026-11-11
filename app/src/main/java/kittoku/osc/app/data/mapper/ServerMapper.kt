package kittoku.osc.app.data.mapper

import kittoku.osc.app.data.api.ServerDto
import kittoku.osc.app.data.api.ServersResponse
import kittoku.osc.app.data.model.Server


internal fun ServersResponse.toSstpServers(): List<Server> {
    if (!success) return emptyList()

    return servers.mapNotNull { it.toSstpServerOrNull() }
}

internal fun ServerDto.toSstpServerOrNull(): Server? {
    val sstp = protocols.sstp ?: return null
    if (!sstp.available) return null
    if (sstp.hostname.isBlank() || sstp.ip.isBlank()) return null

    return Server(
        hostname = sstp.hostname,
        ip = sstp.ip,
        port = sstp.port,
        countryLong = country_long.ifBlank { "Unknown" },
        countryShort = country_short.uppercase().take(2),
        ping = ping,
        speedBps = speed,
        score = score,
        sessions = sessions,
        uptimeSeconds = uptime,
    )
}
