package kittoku.osc.app.data.model

data class Server(
    val hostname: String,
    val ip: String,
    val port: Int,
    val countryLong: String,
    val countryShort: String,
    val ping: Int,
    val speedBps: Long,
    val score: Long,
    val sessions: Int,
    val uptimeSeconds: Long,
    val isCustom: Boolean = false,
    val customId: Long? = null,
    val isFavorite: Boolean = false,
)
