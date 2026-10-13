package ru.landilf.ollink.data.storage

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.landilf.ollink.domain.model.ConnectionSettings
import ru.landilf.ollink.domain.repository.SettingsRepository

class SharedPreferencesSettingsRepository(context: Context) : SettingsRepository {
    private val preferences = context.getSharedPreferences("connection", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(ConnectionSettings(
        preferences.getString("server", "").orEmpty(), preferences.getString("model", "").orEmpty()
    ))
    override val settings = mutableSettings.asStateFlow()
    override fun save(settings: ConnectionSettings) {
        preferences.edit { putString("server", settings.server); putString("model", settings.model) }
        mutableSettings.value = settings
    }
}
