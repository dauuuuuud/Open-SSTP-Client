package kittoku.osc.app.data.mapper

import kittoku.osc.app.data.db.CustomServerEntity
import kittoku.osc.app.data.db.ServerEntity
import kittoku.osc.app.data.model.Server


internal fun ServerEntity.toDomain(): Server {
    return Server(
        hostname = hostname,
        ip = ip,
        port = port,
        countryLong = countryLong,
        countryShort = countryShort,
        ping = ping,
        speedBps = speed,
        score = score,
        sessions = sessions,
        uptimeSeconds = uptime,
        isCustom = false,
        isFavorite = isFavorite,
    )
}

internal fun CustomServerEntity.toDomain(): Server {
    return Server(
        hostname = hostname,
        ip = hostname,
        port = port,
        countryLong = name,
        countryShort = "",
        ping = -1,
        speedBps = 0,
        score = 0,
        sessions = 0,
        uptimeSeconds = 0,
        isCustom = true,
        customId = id,
        isFavorite = isFavorite,
    )
}

internal fun Server.toEntity(fetchedAt: Long): ServerEntity {
    return ServerEntity(
        hostname = hostname,
        ip = ip,
        port = port,
        countryLong = countryLong,
        countryShort = countryShort,
        ping = ping,
        speed = speedBps,
        score = score,
        sessions = sessions,
        uptime = uptimeSeconds,
        isFavorite = isFavorite,
        fetchedAt = fetchedAt,
    )
}
