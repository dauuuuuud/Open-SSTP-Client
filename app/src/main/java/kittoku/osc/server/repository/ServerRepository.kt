package kittoku.osc.server.repository

import android.util.Log
import kittoku.osc.server.api.SstpServerApiService
import kittoku.osc.server.model.Country
import kittoku.osc.server.model.Server
import kittoku.osc.server.model.ServerWithState
import retrofit2.Response

/**
 * Repository for fetching and managing SSTP servers
 */
class ServerRepository(private val apiService: SstpServerApiService) {
    
    private val TAG = "ServerRepository"
    
    private var cachedServers: List<Server> = emptyList()
    private var cachedCountries: List<Country> = emptyList()
    private var lastFetchTime: Long = 0
    private val CACHE_DURATION = 5 * 60 * 1000 // 5 minutes
    
    /**
     * Fetch servers from API and group them by country
     */
    suspend fun fetchServers(): Result<List<Country>> {
        return try {
            // Check cache first
            val now = System.currentTimeMillis()
            if (cachedCountries.isNotEmpty() && (now - lastFetchTime) < CACHE_DURATION) {
                Log.d(TAG, "Returning cached servers")
                return Result.success(cachedCountries)
            }
            
            Log.d(TAG, "Fetching servers from API")
            val response = apiService.getServers()
            
            if (response.isSuccessful) {
                response.body()?.let { servers ->
                    cachedServers = servers
                    cachedCountries = groupServersByCountry(servers)
                    lastFetchTime = now
                    Log.d(TAG, "Fetched ${servers.size} servers, ${cachedCountries.size} countries")
                    Result.success(cachedCountries)
                } ?: run {
                    Result.failure(Exception("Empty response from server"))
                }
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching servers", e)
            // Return cached data if available
            if (cachedCountries.isNotEmpty()) {
                Result.success(cachedCountries)
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Group servers by country
     */
    private fun groupServersByCountry(servers: List<Server>): List<Country> {
        val countryMap = mutableMapOf<String, Country>()
        
        servers.forEach { server ->
            // Skip servers without SSTP support
            if (!server.isSstpAvailable) return@forEach
            
            val countryCode = server.countryShort
            val countryName = server.countryLong
            
            val country = countryMap.getOrPut(countryCode) {
                Country(name = countryName, code = countryCode)
            }
            
            country.servers.add(server)
        }
        
        // Sort countries by name and servers by score
        val countries = countryMap.values.toMutableList()
        countries.sortBy { it.name }
        countries.forEach { it.sortServers() }
        
        return countries
    }
    
    /**
     * Get all servers (flattened)
     */
    fun getAllServers(): List<Server> = cachedServers
    
    /**
     * Get all countries
     */
    fun getAllCountries(): List<Country> = cachedCountries
    
    /**
     * Get servers for a specific country
     */
    fun getServersForCountry(countryCode: String): List<Server> {
        return cachedCountries.find { it.code == countryCode }?.servers ?: emptyList()
    }
    
    /**
     * Get top N servers across all countries
     */
    fun getTopServers(limit: Int = 10): List<Server> {
        return cachedServers.sortedByDescending { it.score }.take(limit)
    }
    
    /**
     * Get fastest servers (by ping)
     */
    fun getFastestServers(limit: Int = 10): List<Server> {
        return cachedServers.sortedBy { it.ping }.take(limit)
    }
    
    /**
     * Search servers by hostname or country
     */
    fun searchServers(query: String): List<Country> {
        val lowerQuery = query.lowercase()
        
        return cachedCountries.map { country ->
            val filteredServers = country.servers.filter {
                it.hostname.contains(lowerQuery, ignoreCase = true) ||
                country.name.contains(lowerQuery, ignoreCase = true) ||
                country.code.equals(lowerQuery, ignoreCase = true)
            }
            country.copy(servers = filteredServers.toMutableList())
        }.filter { it.servers.isNotEmpty() }
    }
    
    /**
     * Clear cache
     */
    fun clearCache() {
        cachedServers = emptyList()
        cachedCountries = emptyList()
        lastFetchTime = 0
    }
}
