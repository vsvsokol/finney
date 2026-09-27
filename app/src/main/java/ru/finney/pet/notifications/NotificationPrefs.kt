package ru.finney.pet.notifications

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first

/** Что уведомлениям нужно помнить между запусками. Тот же DataStore настроек, свои ключи. */
class NotificationPrefs(private val dataStore: DataStore<Preferences>) {

    /** Когда приложение последний раз открывали, мс; null — ещё не знаем. */
    suspend fun lastOpened(): Long? = dataStore.data.first()[LAST_OPENED]

    suspend fun setLastOpened(millis: Long) {
        dataStore.edit { it[LAST_OPENED] = millis }
    }

    /** Разрешение на уведомления уже спрашивали: второй раз не надоедаем. */
    suspend fun permissionAsked(): Boolean = dataStore.data.first()[PERMISSION_ASKED] ?: false

    suspend fun setPermissionAsked() {
        dataStore.edit { it[PERMISSION_ASKED] = true }
    }

    private companion object {
        val LAST_OPENED = longPreferencesKey("notifications_last_opened")
        val PERMISSION_ASKED = booleanPreferencesKey("notifications_permission_asked")
    }
}
