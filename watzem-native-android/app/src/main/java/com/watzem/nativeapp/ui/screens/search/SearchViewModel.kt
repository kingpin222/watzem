package com.watzem.nativeapp.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.model.PersonSearchItem
import com.watzem.nativeapp.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

data class SearchUiState(
    val query: String = "",
    val category: String = "multi", // "multi", "movie", "tv"
    val isSearching: Boolean = false,
    val results: List<MediaItem> = emptyList(),
    val mediaResults: List<MediaItem> = emptyList(),
    val matchedArtists: List<PersonSearchItem> = emptyList(),
    val activeArtist: PersonSearchItem? = null,
    val artistCredits: List<MediaItem> = emptyList(),
    val searchViewMode: String = "media", // "media" or "artist"
    val currentPage: Int = 1,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = false,
    val recentSearches: List<String> = listOf("Spider-Man", "Batman", "Stranger Things", "Dune"),
    val trendingSuggestions: List<String> = listOf(
        "Tom Cruise", "Cillian Murphy", "Zendaya", "Dune", "Deadpool",
        "Stranger Things", "Oppenheimer", "The Boys", "Interstellar"
    ),
    val error: String? = null
) {
    val movieCount: Int
        get() = (if (searchViewMode == "artist") artistCredits else mediaResults)
            .count { it.rawMediaType == "movie" || it.rawMediaType == null }

    val tvCount: Int
        get() = (if (searchViewMode == "artist") artistCredits else mediaResults)
            .count { it.rawMediaType == "tv" }
}

class SearchViewModel(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun setCategory(category: String) {
        if (_uiState.value.category == category) return
        _uiState.value = _uiState.value.copy(category = category)

        if (_uiState.value.searchViewMode == "artist" && _uiState.value.artistCredits.isNotEmpty()) {
            val filtered = filterCreditsByCategory(_uiState.value.artistCredits, category)
            _uiState.value = _uiState.value.copy(results = filtered)
        } else {
            val query = _uiState.value.query.trim()
            if (query.isNotBlank()) {
                performSearch(query, category, page = 1)
            }
        }
    }

    fun onQueryChange(newQuery: String) {
        _uiState.value = _uiState.value.copy(query = newQuery)

        searchJob?.cancel()
        if (newQuery.isBlank()) {
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                results = emptyList(),
                mediaResults = emptyList(),
                matchedArtists = emptyList(),
                activeArtist = null,
                artistCredits = emptyList(),
                searchViewMode = "media",
                currentPage = 1,
                hasMorePages = false,
                error = null
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(350) // Debounce 350ms
            performSearch(newQuery.trim(), _uiState.value.category, page = 1)
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        addRecentSearch(trimmed)
        _uiState.value = _uiState.value.copy(query = trimmed)
        performSearch(trimmed, _uiState.value.category, page = 1)
    }

    fun selectArtist(artist: PersonSearchItem) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                activeArtist = artist,
                searchViewMode = "artist"
            )
            val creditsRes = repository.getActorCredits(artist.id)
            val credits = creditsRes.getOrDefault(emptyList())
            val filtered = filterCreditsByCategory(credits, _uiState.value.category)
            _uiState.value = _uiState.value.copy(
                isSearching = false,
                activeArtist = artist,
                artistCredits = credits,
                results = filtered,
                searchViewMode = "artist",
                hasMorePages = false
            )
            addRecentSearch(artist.name)
        }
    }

    fun searchActorDirectly(actorId: Int, actorName: String, profilePath: String?) {
        val artist = PersonSearchItem(
            id = actorId,
            name = actorName,
            profilePath = profilePath
        )
        _uiState.value = _uiState.value.copy(
            query = actorName,
            isSearching = true,
            activeArtist = artist,
            searchViewMode = "artist"
        )
        viewModelScope.launch {
            val mediaDeferred = async { repository.searchCategory(actorName, _uiState.value.category, 1) }
            val personDeferred = async { repository.searchPerson(actorName, 1) }
            val creditsDeferred = async { repository.getActorCredits(actorId) }

            val mediaRes = mediaDeferred.await()
            val personRes = personDeferred.await()
            val creditsRes = creditsDeferred.await()

            val rawMedia = mediaRes.getOrDefault(emptyList())
            val matchedPeople = personRes.getOrDefault(emptyList())
            val credits = creditsRes.getOrDefault(emptyList())
            val filtered = filterCreditsByCategory(credits, _uiState.value.category)

            _uiState.value = _uiState.value.copy(
                isSearching = false,
                activeArtist = artist,
                artistCredits = credits,
                results = filtered,
                mediaResults = rawMedia,
                matchedArtists = if (matchedPeople.any { it.id == actorId }) matchedPeople else listOf(artist) + matchedPeople,
                searchViewMode = "artist",
                hasMorePages = false
            )
            addRecentSearch(actorName)
        }
    }

    fun setSearchViewMode(mode: String) {
        if (_uiState.value.searchViewMode == mode) return
        val current = _uiState.value
        if (mode == "artist" && current.activeArtist != null && current.artistCredits.isNotEmpty()) {
            val filtered = filterCreditsByCategory(current.artistCredits, current.category)
            _uiState.value = current.copy(
                searchViewMode = "artist",
                results = filtered,
                hasMorePages = false
            )
        } else if (mode == "media") {
            _uiState.value = current.copy(
                searchViewMode = "media",
                results = current.mediaResults,
                hasMorePages = current.mediaResults.size >= 15
            )
        }
    }

    fun clearActiveArtist() {
        val current = _uiState.value
        _uiState.value = current.copy(
            activeArtist = null,
            artistCredits = emptyList(),
            searchViewMode = "media",
            results = current.mediaResults,
            hasMorePages = current.mediaResults.size >= 15
        )
    }

    private fun normalizeString(str: String): String {
        return Normalizer.normalize(str, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale.ROOT)
            .trim()
    }

    private fun filterCreditsByCategory(credits: List<MediaItem>, category: String): List<MediaItem> {
        return when (category) {
            "movie" -> credits.filter { it.rawMediaType == "movie" || it.rawMediaType == null }
            "tv" -> credits.filter { it.rawMediaType == "tv" }
            else -> credits
        }
    }

    private fun performSearch(query: String, category: String, page: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)

            if (page == 1) {
                val mediaDeferred = async { repository.searchCategory(query, category, 1) }
                val personDeferred = async { repository.searchPerson(query, 1) }

                val mediaRes = mediaDeferred.await()
                val personRes = personDeferred.await()

                val rawMedia = mediaRes.getOrDefault(emptyList())
                val matchedPeople = personRes.getOrDefault(emptyList())

                val cleanQuery = normalizeString(query)
                val firstPerson = matchedPeople.firstOrNull()
                val personName = if (firstPerson != null) normalizeString(firstPerson.name) else ""

                val isMultiWord = cleanQuery.split("\\s+".toRegex()).size >= 2
                val isExactActorName = firstPerson != null && (
                    personName == cleanQuery ||
                    (isMultiWord && (personName.startsWith(cleanQuery) || cleanQuery.startsWith(personName)))
                )

                val hasExactMediaTitleMatch = rawMedia.any {
                    val title = normalizeString(it.displayTitle)
                    title == cleanQuery || title.startsWith("$cleanQuery:") || title.startsWith("$cleanQuery ")
                }

                if (firstPerson != null && (isExactActorName || firstPerson.popularity >= 5.0)) {
                    val creditsRes = repository.getActorCredits(firstPerson.id)
                    val credits = creditsRes.getOrDefault(emptyList())
                    val shouldPrioritizeActor = isExactActorName && !hasExactMediaTitleMatch && credits.isNotEmpty()

                    val displayedResults = if (shouldPrioritizeActor) {
                        filterCreditsByCategory(credits, category)
                    } else {
                        rawMedia
                    }

                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        results = displayedResults,
                        mediaResults = rawMedia,
                        matchedArtists = matchedPeople,
                        activeArtist = firstPerson,
                        artistCredits = credits,
                        searchViewMode = if (shouldPrioritizeActor) "artist" else "media",
                        currentPage = 1,
                        hasMorePages = if (shouldPrioritizeActor) false else rawMedia.size >= 15,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        results = rawMedia,
                        mediaResults = rawMedia,
                        matchedArtists = matchedPeople,
                        activeArtist = null,
                        artistCredits = emptyList(),
                        searchViewMode = "media",
                        currentPage = 1,
                        hasMorePages = rawMedia.size >= 15,
                        error = null
                    )
                }
            } else {
                if (_uiState.value.searchViewMode == "artist") {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false, hasMorePages = false)
                    return@launch
                }

                val res = repository.searchCategory(query, category, page)
                if (res.isSuccess) {
                    val newItems = res.getOrDefault(emptyList())
                    val currentIds = _uiState.value.results.map { it.id }.toSet()
                    val deduplicated = newItems.filter { it.id !in currentIds }
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        results = _uiState.value.results + deduplicated,
                        mediaResults = _uiState.value.mediaResults + deduplicated,
                        currentPage = page,
                        isLoadingMore = false,
                        hasMorePages = newItems.size >= 10
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isSearching = false, isLoadingMore = false)
                }
            }
        }
    }

    fun addRecentSearch(query: String) {
        val current = _uiState.value.recentSearches.filter { !it.equals(query, ignoreCase = true) }
        _uiState.value = _uiState.value.copy(recentSearches = listOf(query) + current.take(7))
    }

    fun removeRecentSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            recentSearches = _uiState.value.recentSearches.filter { it != query }
        )
    }

    fun clearRecentSearches() {
        _uiState.value = _uiState.value.copy(recentSearches = emptyList())
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || _uiState.value.isSearching || !_uiState.value.hasMorePages) return
        if (_uiState.value.searchViewMode == "artist") return
        val currentQuery = _uiState.value.query.trim()
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            val nextPage = _uiState.value.currentPage + 1
            performSearch(currentQuery, _uiState.value.category, nextPage)
        }
    }
}
