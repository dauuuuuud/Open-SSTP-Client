package kittoku.osc.server.api

import kittoku.osc.server.model.Server
import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit API service for fetching SSTP servers
 */
interface SstpServerApiService {
    
    @GET(".")
    suspend fun getServers(): Response<List<Server>>
    
    companion object {
        const val BASE_URL = "https://mute-frog-166a.da7oud.workers.dev/"
    }
}
