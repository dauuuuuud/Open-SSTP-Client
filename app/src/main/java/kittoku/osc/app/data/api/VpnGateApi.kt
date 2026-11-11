package kittoku.osc.app.data.api

import retrofit2.http.GET


internal interface VpnGateApi {
    @GET(".")
    suspend fun fetchServers(): ServersResponse

    companion object {
        const val BASE_URL = "https://mute-frog-166a.da7oud.workers.dev/"
    }
}
