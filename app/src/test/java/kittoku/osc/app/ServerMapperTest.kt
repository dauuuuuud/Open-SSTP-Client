package kittoku.osc.app

import kittoku.osc.app.data.api.ServersResponse
import kittoku.osc.app.data.mapper.toSstpServers
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class ServerMapperTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `parses real endpoint payload and keeps only sstp-available servers`() {
        val payload = """
        {
          "success": true,
          "source": "vpngate.net",
          "generated_at": "2026-11-11T19:44:14.861Z",
          "total_servers": 2,
          "servers": [
            {
              "hostname": "public-vpn-226",
              "ip": "219.100.37.201",
              "score": 2950264,
              "ping": 12,
              "speed": 201695822,
              "country_long": "Japan",
              "country_short": "JP",
              "sessions": 95,
              "uptime": 1600094909,
              "protocols": {
                "sstp": {
                  "available": true,
                  "ip": "219.100.37.201",
                  "hostname": "public-vpn-226.opengw.net",
                  "port": 443
                }
              }
            },
            {
              "hostname": "public-vpn-999",
              "ip": "1.2.3.4",
              "score": 10,
              "ping": 50,
              "speed": 1000,
              "country_long": "Nowhere",
              "country_short": "NW",
              "sessions": 1,
              "uptime": 10,
              "protocols": {
                "sstp": {
                  "available": false,
                  "ip": "1.2.3.4",
                  "hostname": "public-vpn-999.opengw.net",
                  "port": 443
                }
              }
            }
          ]
        }
        """.trimIndent()

        val response = json.decodeFromString<ServersResponse>(payload)
        val servers = response.toSstpServers()

        assertEquals(1, servers.size)
        assertEquals("public-vpn-226.opengw.net", servers[0].hostname)
        assertEquals("219.100.37.201", servers[0].ip)
        assertEquals(443, servers[0].port)
        assertEquals("JP", servers[0].countryShort)
        assertEquals(12, servers[0].ping)
        assertEquals(201695822L, servers[0].speedBps)
        assertEquals(95, servers[0].sessions)
        assertTrue(servers[0].score > 0)
    }

    @Test
    fun `servers without sstp protocol entry are dropped`() {
        val payload = """
        {
          "success": true,
          "servers": [
            {
              "hostname": "plain",
              "ip": "1.1.1.1",
              "protocols": {}
            }
          ]
        }
        """.trimIndent()

        val response = json.decodeFromString<ServersResponse>(payload)

        assertEquals(0, response.toSstpServers().size)
    }

    @Test
    fun `unsuccessful response yields empty list`() {
        val payload = """{"success": false, "servers": []}"""
        val response = json.decodeFromString<ServersResponse>(payload)

        assertEquals(0, response.toSstpServers().size)
    }
}
