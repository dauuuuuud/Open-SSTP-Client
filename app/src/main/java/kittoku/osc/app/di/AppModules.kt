package kittoku.osc.app.di

import android.content.Context
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import kittoku.osc.app.data.api.VpnGateApi
import kittoku.osc.app.data.db.AppDatabase
import kittoku.osc.app.data.prefs.AppPrefs
import kittoku.osc.app.data.repo.ServerRepository
import kittoku.osc.app.connection.VpnConnector
import kittoku.osc.app.security.CredentialsStore
import kittoku.osc.app.ui.home.HomeViewModel
import kittoku.osc.app.ui.logs.LogViewModel
import kittoku.osc.app.ui.servers.ServerListViewModel
import kittoku.osc.app.ui.settings.SettingsViewModel
import kittoku.osc.app.ui.split.SplitTunnelViewModel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit


private val dataModule = module {
    single {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(VpnGateApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(VpnGateApi::class.java)
    }

    single { AppDatabase.build(androidContext()) }
    single { get<AppDatabase>().serverDao() }
    single { get<AppDatabase>().customServerDao() }

    single { ServerRepository(androidContext(), get(), get()) }
    single { CredentialsStore(androidContext()) }
    single { AppPrefs(androidContext()) }
    single { VpnConnector(androidContext()) }
}

private val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::ServerListViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SplitTunnelViewModel)
    viewModelOf(::LogViewModel)
}

val appModules = listOf(dataModule, viewModelModule)
