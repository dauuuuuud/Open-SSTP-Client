# SSTP VPN — Modern Android SSTP Client

A production-ready Android VPN client for the **Secure Socket Tunneling Protocol (MS-SSTP)**, built on the
[Open-SSTP-Client](https://github.com/kittoku/Open-SSTP-Client) protocol engine and rebuilt as a modern
Jetpack Compose application with Material Design 3.

The app ships with a built-in, lazily-loaded directory of public SSTP servers (sourced from VPN Gate via a
worker endpoint) and fully supports arbitrary custom SSTP servers (host, port, username, password).

## Features

### VPN & Connection
- One-tap connect / disconnect with a large animated central button and live status (Connecting / Connected / Disconnecting / Error)
- Persistent foreground notification with live status, speed readout, and Connect/Disconnect quick actions
- Real-time download / upload speed and total data-usage statistics
- Kill Switch (aggressive reconnection mode — see notes below)
- Split tunneling (per-app allow / disallow lists, powered by the engine's app rules)
- Auto-connect on device boot and optional reconnection on network change
- IPv6 leak blocking (routes `::/0` into the tunnel by default when PPP IPv6 is disabled)
- Connection log page with the engine's full event history

### Servers
- Remote server directory fetched from `https://mute-frog-166a.da7oud.workers.dev/`
- Only servers with `protocols.sstp.available = true` are shown
- **Lazy / incremental loading**: the first 20 servers render instantly, further batches of 15 are appended on scroll
- Pull-to-refresh plus 30-minute cache in Room; stale caches refresh automatically
- Search by hostname, filter by country / favorites, sort by ping, speed, or score
- Server cards show hostname, country with flag emoji, IP, SSTP port, ping (color-coded), speed, score, and sessions
- Add custom servers with host, port, username, and password; favorites are stored locally

### UI / UX
- Material Design 3, Jetpack Compose, edge-to-edge layout
- Light / Dark / System theme with Material You dynamic color on Android 12+
- Calm deep-blue palette with color-coded ping indicators
- Full English and Persian (فارسی) localization

### Security
- Credentials stored with `EncryptedSharedPreferences` (Android Keystore master key)
- Custom servers and profiles stored locally only; nothing is collected or sent anywhere except the configured VPN server
- Network Security Config restricting cleartext traffic
- VPN permission is always requested through the system dialog before connecting
- No sensitive values (passwords, keys) are written to the connection log

## Architecture

```
kittoku.osc                    ← SSTP protocol engine (upstream, largely untouched)
├── unit / io / client / control    PPP, SSTP, SSL layers
├── terminal                        IPTerminal / SSLTerminal (tunnel + stats hooks)
├── service                         SstpVpnService (foreground VpnService)
├── preference                      OscPrefKey engine settings (SharedPreferences-driven)
└── bus                             NEW: ConnectionBus / StatsBus / LogBus (StateFlow bridges)

kittoku.osc.app                ← New application layer
├── data       Retrofit API (kotlinx-serialization), Room DB, repository, mappers
├── connection VpnConnector (writes engine prefs, starts the service)
├── security   CredentialsStore (EncryptedSharedPreferences)
├── di         Koin modules
├── service    BootReceiver, SaveCertReceiver
└── ui         Compose screens: Home / Servers / Settings / Split Tunnel / Log
               ViewModels (MVVM) with Kotlin Coroutines + Flow
```

- **Pattern**: MVVM — Compose UI → ViewModel → Repository/Connector → engine buses
- **DI**: Koin
- **Networking**: Retrofit + OkHttp + kotlinx-serialization
- **Storage**: Room (server cache, favorites, custom servers), EncryptedSharedPreferences (credentials)
- **Engine bridge**: the engine remains driven by `SharedPreferences` (`OscPrefKey`); the app layer reads/writes
  those keys and observes connection state, traffic, and logs through `StateFlow` buses fed directly by the engine

## Building

Requirements: JDK 17+, Android SDK with platform `android-37.1` (compileSdk 37).

```bash
git clone https://github.com/<your-fork>/Open-SSTP-Client.git
cd Open-SSTP-Client
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:assembleRelease        # unsigned release APK (R8 minified)
./gradlew :app:testDebugUnitTest      # unit tests
```

Output: `app/build/outputs/apk/{debug,release}/`. The release build is minified with R8 and needs to be
signed with your own keystore before distribution.

## Usage

1. Pick a server from the **Servers** tab (or add a custom one via the FAB) and tap it to select it.
2. On the **Home** tab, tap the large central button to connect. Grant the VPN permission when asked.
3. Watch live status, speed, and data usage; disconnect with the same button or from the notification.
4. Tune behavior in **Settings**: custom DNS, IPv6 blocking, cipher suites, keep-alive, timeouts/retries,
   reconnection, kill switch, split tunneling, theme, and default credentials.

## Kill Switch note

The in-app kill switch currently works by forcing aggressive reconnection when the tunnel drops, and the
app routes all IPv4 (and `::/0` when IPv6 tunneling is disabled) traffic through the VPN. For a
kernel-enforced guarantee, enable **Settings → VPN → Always-On VPN + Block connections without VPN** for
this app in the Android system settings.

## Testing

- `./gradlew :app:testDebugUnitTest` — 15 unit tests, all passing:
  - `ServerMapperTest` (3): real-payload parsing, SSTP-only filtering, failed-response handling
  - `ServerFilterTest` (8): search, country and favorite filters, all sort modes, country dedup, lazy pager batching, empty state
  - `FormatUtilsTest` (4): flag emoji, byte/speed/bit formatters, duration, error mapping
- `:app:assembleDebug` and `:app:assembleRelease` (R8) both build successfully.
- Live SSTP connections to real servers were not exercised in the build sandbox (no emulator/device);
  the protocol engine itself is the stable upstream implementation and was kept intact.

## Credits

The SSTP/PPP protocol engine is the work of the original
[Open-SSTP-Client](https://github.com/kittoku/Open-SSTP-Client) by [kittoku](https://github.com/kittoku).
This fork replaces the original View-based UI with a Jetpack Compose application, adds the server
directory, statistics, and modern Android architecture on top of it.
