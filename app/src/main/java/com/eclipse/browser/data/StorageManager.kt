package com.eclipse.browser.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "eclipse_settings")

class StorageManager(private val context: Context) {

    companion object {
        val SEARCH_ENGINE = stringPreferencesKey("searchEngine")
        val BG_THEME = stringPreferencesKey("bgTheme")
        val ACCENT_COLOR = stringPreferencesKey("accentColor")
        val ACCENT_COLOR_LIGHT = stringPreferencesKey("accentColorLight")
        val PARTICLES_ON = booleanPreferencesKey("particlesOn")
        val STARS_ON = booleanPreferencesKey("starsOn")
        val ORB_ON = booleanPreferencesKey("orbOn")
        val WEATHER_ON = booleanPreferencesKey("weatherOn")
        val UI_STYLE = stringPreferencesKey("uiStyle")
        val QUICK_SITES = stringPreferencesKey("quickSites")
        val BOOKMARKS = stringPreferencesKey("bookmarks")
        val HISTORY = stringPreferencesKey("history")
        val AD_BLOCK_ON = booleanPreferencesKey("adBlockOn")
        val ADS_BLOCKED = intPreferencesKey("adsBlocked")
        val TRACKERS_BLOCKED = intPreferencesKey("trackersBlocked")
    }

    val searchEngine: Flow<String> = context.dataStore.data.map { it[SEARCH_ENGINE] ?: "duckduckgo" }
    val bgTheme: Flow<String> = context.dataStore.data.map { it[BG_THEME] ?: "eclipse" }
    val accentColor: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR] ?: "#FF6B1A" }
    val accentColorLight: Flow<String> = context.dataStore.data.map { it[ACCENT_COLOR_LIGHT] ?: "#FFB347" }
    val particlesOn: Flow<Boolean> = context.dataStore.data.map { it[PARTICLES_ON] ?: true }
    val starsOn: Flow<Boolean> = context.dataStore.data.map { it[STARS_ON] ?: true }
    val orbOn: Flow<Boolean> = context.dataStore.data.map { it[ORB_ON] ?: true }
    val weatherOn: Flow<Boolean> = context.dataStore.data.map { it[WEATHER_ON] ?: true }
    val uiStyle: Flow<String> = context.dataStore.data.map { it[UI_STYLE] ?: "normal" }
    val quickSites: Flow<String> = context.dataStore.data.map { it[QUICK_SITES] ?: DEFAULT_SITES }
    val bookmarks: Flow<String> = context.dataStore.data.map { it[BOOKMARKS] ?: "[]" }
    val history: Flow<String> = context.dataStore.data.map { it[HISTORY] ?: "[]" }
    val adBlockOn: Flow<Boolean> = context.dataStore.data.map { it[AD_BLOCK_ON] ?: true }
    val adsBlocked: Flow<Int> = context.dataStore.data.map { it[ADS_BLOCKED] ?: 0 }
    val trackersBlocked: Flow<Int> = context.dataStore.data.map { it[TRACKERS_BLOCKED] ?: 0 }

    suspend fun <T> save(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun clearHistory() {
        context.dataStore.edit { it[HISTORY] = "[]" }
    }

    suspend fun clearBookmarks() {
        context.dataStore.edit { it[BOOKMARKS] = "[]" }
    }
}

const val DEFAULT_SITES = """[
    {"url":"https://www.google.com","label":"Google"},
    {"url":"https://www.youtube.com","label":"YouTube"},
    {"url":"https://twitter.com","label":"Twitter"},
    {"url":"https://www.reddit.com","label":"Reddit"},
    {"url":"https://github.com","label":"GitHub"},
    {"url":"https://www.instagram.com","label":"Instagram"},
    {"url":"https://www.wikipedia.org","label":"Wikipedia"}
]"""
