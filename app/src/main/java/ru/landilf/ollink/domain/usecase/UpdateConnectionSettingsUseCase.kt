package ru.landilf.ollink.domain.usecase

import ru.landilf.ollink.domain.model.ConnectionSettings
import ru.landilf.ollink.domain.model.ServerAddress
import ru.landilf.ollink.domain.repository.SettingsRepository

class UpdateConnectionSettingsUseCase(private val repository: SettingsRepository) {
    operator fun invoke(server: String, model: String) {
        repository.save(ConnectionSettings(ServerAddress.normalize(server), model))
    }
}
