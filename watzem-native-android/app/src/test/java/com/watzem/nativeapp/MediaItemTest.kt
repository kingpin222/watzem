package com.watzem.nativeapp

import com.watzem.nativeapp.data.model.MediaItem
import com.watzem.nativeapp.data.model.MediaType
import com.watzem.nativeapp.data.model.WatchlistItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaItemTest {

    @Test
    fun testMediaItemFormatting() {
        val movie = MediaItem(
            id = 101,
            _title = "Inception",
            voteAverage = 8.35,
            _releaseDate = "2010-07-16",
            posterPath = "/inception.jpg",
            rawMediaType = "movie"
        )

        assertEquals("Inception", movie.displayTitle)
        assertEquals("2010", movie.releaseYear)
        assertEquals(MediaType.MOVIE, movie.mediaType)
        assertEquals("https://image.tmdb.org/t/p/w500/inception.jpg", movie.posterUrl)
        assertEquals("8.4", movie.ratingFormatted)

        val watchlistItem = WatchlistItem.fromMediaItem(movie)
        assertEquals(101, watchlistItem.id)
        assertEquals("Inception", watchlistItem.title)
        assertEquals("2010", watchlistItem.releaseYear)
        assertEquals(MediaType.MOVIE, watchlistItem.mediaType)
    }

    @Test
    fun testTvShowFormatting() {
        val show = MediaItem(
            id = 202,
            _name = "Breaking Bad",
            voteAverage = 9.5,
            _firstAirDate = "2008-01-20",
            rawMediaType = "tv"
        )

        assertEquals("Breaking Bad", show.displayTitle)
        assertEquals("2008", show.releaseYear)
        assertEquals(MediaType.TV, show.mediaType)
    }
}

