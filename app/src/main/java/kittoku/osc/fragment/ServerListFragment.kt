package kittoku.osc.fragment

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.card.MaterialCardView
import kittoku.osc.R
import kittoku.osc.server.adapter.ServerListAdapter
import kittoku.osc.server.model.ConnectionStatus
import kittoku.osc.server.model.Country
import kittoku.osc.server.model.Server
import kittoku.osc.server.viewmodel.ServerViewModel
import kittoku.osc.service.ACTION_VPN_CONNECT
import kittoku.osc.service.ACTION_VPN_DISCONNECT
import kittoku.osc.service.SstpVpnService

/**
 * Fragment for displaying and managing SSTP servers
 */
class ServerListFragment : Fragment() {
    
    private val TAG = "ServerListFragment"
    private val viewModel: ServerViewModel by viewModels()
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var fabConnect: FloatingActionButton
    private lateinit var connectionStatusCard: MaterialCardView
    private lateinit var statusText: TextView
    private lateinit var statusDetails: TextView
    private lateinit var attemptsLayout: View
    private lateinit var attemptCount: TextView
    
    private lateinit var adapter: ServerListAdapter
    
    private var isConnected = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_server_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        fabConnect = view.findViewById(R.id.fabConnect)
        connectionStatusCard = view.findViewById(R.id.connectionStatusCard)
        statusText = view.findViewById(R.id.statusText)
        statusDetails = view.findViewById(R.id.statusDetails)
        attemptsLayout = view.findViewById(R.id.attemptsLayout)
        attemptCount = view.findViewById(R.id.attemptCount)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup SwipeRefresh
        swipeRefresh.setOnRefreshListener {
            viewModel.refreshServers()
        }
        
        // Setup FAB
        fabConnect.setOnClickListener {
            if (isConnected) {
                disconnect()
            } else {
                connect()
            }
        }
        
        // Observe ViewModel
        observeViewModel()
        
        // Load servers
        viewModel.fetchServers()
    }
    
    private fun setupRecyclerView() {
        adapter = ServerListAdapter(
            onCountryClick = { country ->
                toggleCountry(country)
            },
            onServerClick = { server ->
                selectServer(server)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)
    }
    
    private fun observeViewModel() {
        // Observe countries
        viewModel.countries.observe(viewLifecycleOwner, Observer { countries ->
            adapter.updateCountries(countries)
            swipeRefresh.isRefreshing = false
        })
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            swipeRefresh.isRefreshing = isLoading
        })
        
        // Observe error
        viewModel.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Log.e(TAG, "Error: $it")
                // Show error to user
            }
        })
        
        // Observe connection status
        viewModel.connectionStatus.observe(viewLifecycleOwner, Observer { status ->
            updateConnectionStatus(status)
        })
        
        // Observe current server
        viewModel.currentServer.observe(viewLifecycleOwner, Observer { serverWithState ->
            serverWithState?.let {
                statusDetails.text = "Connecting to: ${it.server.hostname}"
            } ?: run {
                statusDetails.text = "Ready to connect"
            }
        })
        
        // Observe attempt count
        viewModel.attemptCount.observe(viewLifecycleOwner, Observer { count ->
            attemptCount.text = "$count/5"
            attemptsLayout.visibility = if (count > 0) View.VISIBLE else View.GONE
        })
        
        // Observe servers tried
        viewModel.serversTried.observe(viewLifecycleOwner, Observer { servers ->
            // Update adapter to show connection states
            updateServerStates(servers)
        })
    }
    
    private fun toggleCountry(country: Country) {
        adapter.toggleCountry(country.code)
    }
    
    private fun selectServer(server: Server) {
        // Store selected server and try to connect
        viewModel.stopConnection()
        // In a real implementation, we'd set the server details in preferences
        // and then start the connection
        connectToServer(server)
    }
    
    private fun connectToServer(server: Server) {
        // Set server details in ViewModel
        // Start connection
        viewModel.startConnection()
        
        // Start VPN service
        VpnService.prepare(requireContext())?.also { intent ->
            startActivityForResult(intent, REQUEST_CODE_VPN_PREPARE)
        } ?: startVpnService(ACTION_VPN_CONNECT)
    }
    
    private fun connect() {
        viewModel.startConnection()
        
        VpnService.prepare(requireContext())?.also { intent ->
            startActivityForResult(intent, REQUEST_CODE_VPN_PREPARE)
        } ?: startVpnService(ACTION_VPN_CONNECT)
    }
    
    private fun disconnect() {
        viewModel.stopConnection()
        startVpnService(ACTION_VPN_DISCONNECT)
    }
    
    private fun startVpnService(action: String) {
        val intent = Intent(requireContext(), SstpVpnService::class.java).setAction(action)
        requireContext().startService(intent)
    }
    
    private fun updateConnectionStatus(status: ConnectionStatus) {
        isConnected = when (status) {
            ConnectionStatus.CONNECTED -> true
            ConnectionStatus.DISCONNECTED, ConnectionStatus.FAILED -> false
            else -> isConnected
        }
        
        when (status) {
            ConnectionStatus.IDLE -> {
                connectionStatusCard.visibility = View.GONE
                fabConnect.setImageResource(R.drawable.ic_connect)
            }
            ConnectionStatus.CONNECTING -> {
                connectionStatusCard.visibility = View.VISIBLE
                statusText.text = "Connecting"
                statusText.setTextColor(requireContext().getColor(R.color.status_connecting))
                fabConnect.setImageResource(R.drawable.ic_disconnect)
            }
            ConnectionStatus.CONNECTED -> {
                connectionStatusCard.visibility = View.VISIBLE
                statusText.text = "Connected"
                statusText.setTextColor(requireContext().getColor(R.color.status_connected))
                fabConnect.setImageResource(R.drawable.ic_disconnect)
            }
            ConnectionStatus.DISCONNECTED -> {
                connectionStatusCard.visibility = View.VISIBLE
                statusText.text = "Disconnected"
                statusText.setTextColor(requireContext().getColor(R.color.status_disconnected))
                fabConnect.setImageResource(R.drawable.ic_connect)
            }
            ConnectionStatus.FAILED -> {
                connectionStatusCard.visibility = View.VISIBLE
                statusText.text = "Connection Failed"
                statusText.setTextColor(requireContext().getColor(R.color.status_failed))
                fabConnect.setImageResource(R.drawable.ic_connect)
            }
            ConnectionStatus.RETRYING -> {
                connectionStatusCard.visibility = View.VISIBLE
                statusText.text = "Retrying"
                statusText.setTextColor(requireContext().getColor(R.color.status_retrying))
                fabConnect.setImageResource(R.drawable.ic_disconnect)
            }
        }
    }
    
    private fun updateServerStates(serversTried: List<kittoku.osc.server.model.ServerWithState>) {
        // Update the adapter to show which servers have been tried
        // This would be more sophisticated in a real implementation
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.server_list_menu, menu)
        
        // Setup search
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchServers(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { 
                    if (it.isNotEmpty()) {
                        viewModel.searchServers(it)
                    } else {
                        viewModel.refreshServers()
                    }
                }
                return true
            }
        })
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.refreshServers()
                return true
            }
            R.id.action_sort -> {
                // Show sort dialog
                showSortDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun showSortDialog() {
        // In a real implementation, show a dialog with sort options
        // For now, just sort by name
        viewModel.sortCountries(ServerViewModel.SortType.BY_NAME)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_VPN_PREPARE && resultCode == android.app.Activity.RESULT_OK) {
            startVpnService(ACTION_VPN_CONNECT)
        }
    }
    
    companion object {
        private const val REQUEST_CODE_VPN_PREPARE = 1
    }
}
