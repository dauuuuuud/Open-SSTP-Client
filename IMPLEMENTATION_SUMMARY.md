# Open SSTP Client - Modern UI with Server List Implementation

## Overview
This implementation adds a modern, professional UI to the Open SSTP Client Android app with the following features:

1. **Server List from API** - Fetches SSTP servers from `https://mute-frog-166a.da7oud.workers.dev/`
2. **Modern UI** - Clean, professional interface with Material Design 3
3. **Country-based Organization** - Servers grouped by country with expandable lists
4. **Fast Loading** - Efficient API calls with caching
5. **Automatic Server Switching** - If connection fails, automatically tries next server (up to 5 attempts)
6. **Connection Status Tracking** - Shows connection attempts and status in real-time

## Files Modified

### 1. Build Configuration
- **`app/build.gradle`** - Added dependencies:
  - Retrofit 2.11.0 (API calls)
  - Gson converter
  - OkHttp logging interceptor
  - Kotlin Coroutines
  - Lifecycle ViewModel
  - RecyclerView
  - CardView

### 2. Android Manifest
- **`app/src/main/AndroidManifest.xml`**
  - Changed default launcher activity to `NewMainActivity`
  - Kept `MainActivity` for backward compatibility (Quick Settings tile)

### 3. Colors
- **`app/src/main/res/values/colors.xml`**
  - Added Material Design 3 color palette
  - Added status colors (connected, connecting, disconnected, failed, retrying)

## New Files Created

### Server Module (`kittoku.osc.server`)

#### Models
- **`Server.kt`** - Data models:
  - `Server` - Represents an SSTP server with all properties
  - `Protocols` - Contains SSTP protocol details
  - `SstpProtocol` - SSTP-specific connection info
  - `Country` - Groups servers by country with expandable state
  - `ConnectionStatus` - Enum for connection states
  - `ServerWithState` - Server with connection tracking

#### API Layer
- **`SstpServerApiService.kt`** - Retrofit interface for fetching servers
- **`RetrofitClient.kt`** - Retrofit client configuration with:
  - Connection timeouts (10s connect, 30s read/write)
  - HTTP logging
  - Gson converter

#### Repository
- **`ServerRepository.kt`** - Manages server data:
  - Fetches servers from API
  - Groups servers by country
  - Caches data for 5 minutes
  - Provides search and sorting functionality

#### Manager
- **`ServerConnectionManager.kt`** - Handles connection logic:
  - Tracks current server and attempts
  - Manages retry logic (up to 5 servers)
  - Notifies listeners of status changes

#### Adapter
- **`ServerListAdapter.kt`** - RecyclerView adapter:
  - Displays countries as expandable headers
  - Shows servers under each country
  - Supports click listeners for countries and servers
  - Displays connection status for each server

#### ViewModel
- **`ServerViewModel.kt`** - Manages UI state:
  - Fetches and caches server list
  - Tracks connection status
  - Manages server selection
  - Handles sorting and searching

### UI Components

#### Activities
- **`NewMainActivity.kt`** - New main activity with:
  - Bottom navigation (Servers, Settings)
  - Fragment container
  - Modern Material Design

#### Fragments
- **`ServerListFragment.kt`** - Main server list screen:
  - RecyclerView with countries and servers
  - Swipe-to-refresh
  - FAB for connect/disconnect
  - Connection status card
  - Search functionality
  - Sort options

#### Layouts
- **`activity_new_main.xml`** - Main activity layout with bottom navigation
- **`fragment_server_list.xml`** - Server list fragment with:
  - MaterialToolbar
  - SwipeRefreshLayout
  - RecyclerView
  - FAB
  - Connection status card
- **`item_country_header.xml`** - Country header item:
  - Country name and flag
  - Server count badge
  - Expand/collapse icon
- **`item_server.xml`** - Server item:
  - Hostname
  - IP and port
  - Ping, speed, sessions, uptime
  - Connection status display

#### Menus
- **`bottom_nav_menu.xml`** - Bottom navigation menu
- **`main_menu.xml`** - Main options menu
- **`server_list_menu.xml`** - Server list actions (refresh, sort, search)

#### Drawables
- **Icons**: connect, disconnect, expand, collapse, globe, refresh, sort, search
- **Backgrounds**: server count badge, bottom navigation colors

### Service Enhancements
- **`SstpVpnService.kt`** - Enhanced with:
  - Server repository integration
  - Server switching logic (up to 5 attempts)
  - Server list loading
  - Automatic server selection on connection failure
  - Enhanced reconnection logic

## Features Implemented

### 1. Server List from API
- Fetches JSON from `https://mute-frog-166a.da7oud.workers.dev/`
- Parses server data with Gson
- Caches for 5 minutes
- Handles errors gracefully (returns cached data if available)

### 2. Modern Professional UI
- Material Design 3 components
- Clean, modern look
- Responsive design
- Dark/light theme support (via Material components)

### 3. Country-based Organization
- Servers grouped by country (using `country_short` and `country_long`)
- Expandable/collapsible country headers
- Server count displayed on each country
- Countries sorted alphabetically
- Servers sorted by score (higher is better)

### 4. Fast Loading
- Retrofit for efficient API calls
- 10-second connection timeout
- 30-second read/write timeout
- Caching to avoid repeated network calls
- Background loading with coroutines

### 5. Automatic Server Switching
- Tries up to 5 different servers
- If connection fails, automatically tries next server
- Shows attempt count (e.g., "Attempt 2/5")
- Displays which server is being tried
- Connection status updates in real-time

### 6. Connection Status Tracking
- Shows current connection status:
  - IDLE
  - CONNECTING
  - CONNECTED
  - DISCONNECTED
  - FAILED
  - RETRYING
- Displays connection details in status card
- Shows attempt count and errors

### 7. User Experience
- Swipe down to refresh server list
- Click country to expand/collapse
- Click server to connect
- FAB to connect/disconnect
- Search servers by name or country
- Sort servers by different criteria

## Technical Details

### Architecture
- **MVVM** pattern for UI
- **Repository** pattern for data
- **Clean Architecture** principles
- **Coroutines** for async operations
- **Retrofit** for network calls

### Dependencies Added
```gradle
// Retrofit
implementation 'com.squareup.retrofit2:retrofit:2.11.0'
implementation 'com.squareup.retrofit2:converter-gson:2.11.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'

// Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0'
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.8.6'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6'

// UI
implementation 'androidx.recyclerview:recyclerview:1.3.2'
implementation 'androidx.cardview:cardview:1.0.0'
```

### Data Flow
1. **ServerListFragment** -> Requests data from **ServerViewModel**
2. **ServerViewModel** -> Fetches from **ServerRepository**
3. **ServerRepository** -> Calls **SstpServerApiService** (Retrofit)
4. API returns JSON -> Parsed to Server objects
5. Repository groups by country -> Returns to ViewModel
6. ViewModel updates LiveData -> Fragment observes and updates UI

### Connection Flow
1. User clicks Connect (FAB or server item)
2. **ServerViewModel** starts connection process
3. **SstpVpnService** is started with ACTION_VPN_CONNECT
4. Service loads server list
5. Service sets first server in preferences
6. Service initializes Controller
7. If connection fails:
   - Controller triggers reconnection
   - Service checks if more servers to try
   - If yes, tries next server
   - If no, shows error
8. Connection status updates are sent to UI

## Usage

### Starting the App
The app now launches with `NewMainActivity` which shows:
- Bottom navigation with "Servers" and "Settings" tabs
- Server list on the Servers tab
- Settings on the Settings tab (existing functionality)

### Connecting to a Server
1. Open the app
2. Scroll through countries or use search
3. Click on a country to expand it
4. Click on a server to connect
5. Or click the FAB to connect to the first available server

### Automatic Retry
- If connection fails, the app automatically tries the next server
- Up to 5 servers will be tried
- Connection status is shown at the bottom of the screen
- Each attempt is logged and displayed

### Refreshing Server List
- Pull down on the server list to refresh
- Or click the refresh icon in the menu

### Searching
- Click the search icon in the menu
- Type to search by country name, country code, or server hostname
- Results are filtered in real-time

## Future Enhancements

1. **Server Favorites** - Allow users to mark favorite servers
2. **Connection History** - Track which servers work best
3. **Ping Testing** - Test server latency before connecting
4. **Server Details** - Show more information about each server
5. **Filtering** - Filter by country, ping, speed, etc.
6. **Custom Server** - Allow adding custom servers
7. **Connection Statistics** - Show connection speed, data usage, etc.

## Testing

To test the implementation:

1. **Build the app** - The project should compile with the new dependencies
2. **Run the app** - The new UI should appear with server list
3. **Test connection** - Try connecting to servers (may fail without valid credentials)
4. **Test retry logic** - The app should automatically try next server on failure
5. **Test UI** - Expand/collapse countries, search, sort, refresh

## Notes

- The app fetches servers from a public API
- Server credentials (username/password) are still required from settings
- The connection retry logic works alongside the existing reconnection system
- The old MainActivity is kept for backward compatibility with Quick Settings tile
- All existing functionality is preserved

## Files Summary

**Total Files Modified:** 4
**Total New Files Created:** ~30
**Total Lines of Code Added:** ~2000+

### New Package Structure
```
kittoku.osc.server/
├── adapter/
│   └── ServerListAdapter.kt
├── api/
│   ├── RetrofitClient.kt
│   └── SstpServerApiService.kt
├── manager/
│   └── ServerConnectionManager.kt
├── model/
│   └── Server.kt
├── repository/
│   └── ServerRepository.kt
└── viewmodel/
    └── ServerViewModel.kt
```
