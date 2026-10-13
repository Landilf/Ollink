package ru.landilf.ollink.di

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ru.landilf.ollink.data.network.HttpOllamaRepository
import ru.landilf.ollink.data.storage.SharedPreferencesChatRepository
import ru.landilf.ollink.data.storage.SharedPreferencesSettingsRepository
import ru.landilf.ollink.domain.repository.*
import ru.landilf.ollink.presentation.ChatViewModel

/** Composition root, matching DACtune's constructor injection without Hilt. */
class AppContainer(context: Context) {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val settingsRepository: SettingsRepository = SharedPreferencesSettingsRepository(context.applicationContext)
    val chatRepository: ChatRepository = SharedPreferencesChatRepository(context.applicationContext, applicationScope)
    val ollamaRepository: OllamaRepository = HttpOllamaRepository()
    val viewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ChatViewModel::class.java))
            return ChatViewModel(settingsRepository, chatRepository, ollamaRepository) as T
        }
    }
}
