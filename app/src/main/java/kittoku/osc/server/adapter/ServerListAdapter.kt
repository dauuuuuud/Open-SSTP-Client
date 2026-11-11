package kittoku.osc.server.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import kittoku.osc.R
import kittoku.osc.server.model.ConnectionStatus
import kittoku.osc.server.model.Country
import kittoku.osc.server.model.Server
import kittoku.osc.server.model.ServerWithState

/**
 * Adapter for displaying countries and servers in a RecyclerView
 * with expandable/collapsible countries
 */
class ServerListAdapter(
    private var countries: List<Country> = emptyList(),
    private val onCountryClick: (Country) -> Unit,
    private val onServerClick: (Server) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_COUNTRY = 0
        private const val VIEW_TYPE_SERVER = 1
    }

    private var expandedCountryCodes: Set<String> = emptySet()

    override fun getItemViewType(position: Int): Int {
        val flatList = getFlattenedList()
        return if (flatList[position].isCountry) VIEW_TYPE_COUNTRY else VIEW_TYPE_SERVER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_COUNTRY -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_country_header, parent, false)
                CountryViewHolder(view)
            }
            VIEW_TYPE_SERVER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_server, parent, false)
                ServerViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val flatList = getFlattenedList()
        val item = flatList[position]

        when (holder) {
            is CountryViewHolder -> {
                val country = item.country!!
                holder.bind(country, expandedCountryCodes.contains(country.code)) {
                    onCountryClick(country)
                }
            }
            is ServerViewHolder -> {
                val server = item.server!!
                holder.bind(server) {
                    onServerClick(server)
                }
            }
        }
    }

    override fun getItemCount(): Int = getFlattenedList().size

    /**
     * Update the list of countries
     */
    fun updateCountries(newCountries: List<Country>) {
        countries = newCountries
        notifyDataSetChanged()
    }

    /**
     * Toggle country expansion
     */
    fun toggleCountry(countryCode: String) {
        val newSet = if (expandedCountryCodes.contains(countryCode)) {
            expandedCountryCodes - countryCode
        } else {
            expandedCountryCodes + countryCode
        }
        expandedCountryCodes = newSet
        notifyDataSetChanged()
    }

    /**
     * Expand all countries
     */
    fun expandAll() {
        expandedCountryCodes = countries.map { it.code }.toSet()
        notifyDataSetChanged()
    }

    /**
     * Collapse all countries
     */
    fun collapseAll() {
        expandedCountryCodes = emptySet()
        notifyDataSetChanged()
    }

    /**
     * Get flattened list of countries and servers
     */
    private fun getFlattenedList(): List<Item> {
        val result = mutableListOf<Item>()

        countries.forEach { country ->
            result.add(Item(country = country))
            if (expandedCountryCodes.contains(country.code)) {
                country.servers.forEach { server ->
                    result.add(Item(server = server))
                }
            }
        }

        return result
    }

    /**
     * Item wrapper for RecyclerView
     */
    private data class Item(
        val country: Country? = null,
        val server: Server? = null
    ) {
        val isCountry: Boolean = country != null
    }

    /**
     * ViewHolder for Country items
     */
    inner class CountryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val countryName: TextView = itemView.findViewById(R.id.countryName)
        private val serverCount: TextView = itemView.findViewById(R.id.serverCount)
        private val expandIcon: ImageView = itemView.findViewById(R.id.expandIcon)
        private val countryFlag: ImageView = itemView.findViewById(R.id.countryFlag)
        private val cardView: CardView = itemView.findViewById(R.id.cardView)

        fun bind(country: Country, isExpanded: Boolean, onClick: () -> Unit) {
            countryName.text = country.name
            serverCount.text = country.serverCount.toString()
            expandIcon.setImageResource(
                if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand
            )

            // Set flag based on country code (placeholder)
            // In a real app, you'd use a flag library or emoji
            countryFlag.setImageResource(R.drawable.ic_globe)

            cardView.setOnClickListener { onClick() }
        }
    }

    /**
     * ViewHolder for Server items
     */
    inner class ServerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val serverHostname: TextView = itemView.findViewById(R.id.serverHostname)
        private val serverPing: TextView = itemView.findViewById(R.id.serverPing)
        private val serverIp: TextView = itemView.findViewById(R.id.serverIp)
        private val serverPort: TextView = itemView.findViewById(R.id.serverPort)
        private val serverSpeed: TextView = itemView.findViewById(R.id.serverSpeed)
        private val serverSessions: TextView = itemView.findViewById(R.id.serverSessions)
        private val serverUptime: TextView = itemView.findViewById(R.id.serverUptime)
        private val connectionStatusLayout: View = itemView.findViewById(R.id.connectionStatusLayout)
        private val connectionStatusIcon: ImageView = itemView.findViewById(R.id.connectionStatusIcon)
        private val connectionStatusText: TextView = itemView.findViewById(R.id.connectionStatusText)

        fun bind(server: Server, onClick: () -> Unit) {
            serverHostname.text = server.hostname
            serverPing.text = "${server.ping}ms"
            serverIp.text = server.ip
            serverPort.text = ":${server.sstpPort}"
            serverSpeed.text = server.speedFormatted
            serverSessions.text = "${server.sessions} sessions"
            serverUptime.text = "${server.uptimeFormatted} uptime"

            // Set click listener
            itemView.setOnClickListener { onClick() }

            // Hide connection status by default
            connectionStatusLayout.visibility = View.GONE
        }

        fun bindWithState(serverWithState: ServerWithState, onClick: () -> Unit) {
            bind(serverWithState.server, onClick)

            // Show connection status
            connectionStatusLayout.visibility = View.VISIBLE
            when (serverWithState.connectionStatus) {
                ConnectionStatus.CONNECTING -> {
                    connectionStatusIcon.setImageResource(R.drawable.ic_baseline_vpn_lock_24)
                    connectionStatusIcon.setColorFilter(
                        itemView.context.getColor(R.color.status_connecting)
                    )
                    connectionStatusText.text = "Connecting..."
                    connectionStatusText.setTextColor(itemView.context.getColor(R.color.status_connecting))
                }
                ConnectionStatus.CONNECTED -> {
                    connectionStatusIcon.setImageResource(R.drawable.ic_baseline_vpn_lock_24)
                    connectionStatusIcon.setColorFilter(
                        itemView.context.getColor(R.color.status_connected)
                    )
                    connectionStatusText.text = "Connected"
                    connectionStatusText.setTextColor(itemView.context.getColor(R.color.status_connected))
                }
                ConnectionStatus.FAILED -> {
                    connectionStatusIcon.setImageResource(R.drawable.ic_baseline_close_24)
                    connectionStatusIcon.setColorFilter(
                        itemView.context.getColor(R.color.status_failed)
                    )
                    connectionStatusText.text = "Failed (${serverWithState.attemptCount}/5)"
                    connectionStatusText.setTextColor(itemView.context.getColor(R.color.status_failed))
                }
                ConnectionStatus.RETRYING -> {
                    connectionStatusIcon.setImageResource(R.drawable.ic_refresh)
                    connectionStatusIcon.setColorFilter(
                        itemView.context.getColor(R.color.status_retrying)
                    )
                    connectionStatusText.text = "Retrying... (${serverWithState.attemptCount}/5)"
                    connectionStatusText.setTextColor(itemView.context.getColor(R.color.status_retrying))
                }
                else -> {
                    connectionStatusLayout.visibility = View.GONE
                }
            }
        }
    }
}
