package com.watzem.nativeapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.watzem.nativeapp.data.model.WatchlistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WatchlistManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("watzem_watchlist_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _watchlistFlow = MutableStateFlow<List<WatchlistItem>>(emptyList())
    val watchlistFlow: StateFlow<List<WatchlistItem>> = _watchlistFlow.asStateFlow()

    init {
        loadWatchlist()
    }

    private fun loadWatchlist() {
        val json = prefs.getString(KEY_WATCHLIST, null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<WatchlistItem>>() {}.type
                val items: List<WatchlistItem> = gson.fromJson(json, type) ?: emptyList()
                _watchlistFlow.value = items.sortedByDescending { it.addedAt }
            } catch (e: Exception) {
                _watchlistFlow.value = emptyList()
            }
        } else {
            _watchlistFlow.value = emptyList()
        }
    }

    private fun saveWatchlist(items: List<WatchlistItem>) {
        val json = gson.toJson(items)
        prefs.edit().putString(KEY_WATCHLIST, json).apply()
        _watchlistFlow.value = items.sortedByDescending { it.addedAt }
    }

    fun isWatchlisted(mediaId: Int): Boolean {
        return _watchlistFlow.value.any { it.id == mediaId }
    }

    fun toggleWatchlist(item: WatchlistItem): Boolean {
        val currentList = _watchlistFlow.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == item.id }

        val isAdded: Boolean
        if (existingIndex >= 0) {
            currentList.removeAt(existingIndex)
            isAdded = false
        } else {
            currentList.add(0, item.copy(addedAt = System.currentTimeMillis()))
            isAdded = true
        }

        saveWatchlist(currentList)
        return isAdded
    }

    fun removeFromWatchlist(mediaId: Int) {
        val currentList = _watchlistFlow.value.filterNot { it.id == mediaId }
        saveWatchlist(currentList)
    }

    companion object {
        private const val KEY_WATCHLIST = "saved_watchlist_items"

        @Volatile
        private var INSTANCE: WatchlistManager? = null

        fun getInstance(context: Context): WatchlistManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchlistManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

