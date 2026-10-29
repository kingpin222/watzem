package com.watzem.nativeapp

import com.watzem.nativeapp.data.deeplink.DeepLinkHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkHandlerTest {

    @Test
    fun testBuildDeepLink() {
        val movieLink = DeepLinkHandler.buildDeepLink(550, isTv = false)
        assertEquals("watzem://movie/550", movieLink)

        val tvLink = DeepLinkHandler.buildDeepLink(1399, isTv = true)
        assertEquals("watzem://tv/1399", tvLink)
    }

    @Test
    fun testBuildWebLink() {
        val movieWeb = DeepLinkHandler.buildWebLink(550, isTv = false)
        assertEquals("https://watzem.app/movie/550", movieWeb)

        val tvWeb = DeepLinkHandler.buildWebLink(1399, isTv = true)
        assertEquals("https://watzem.app/tv/1399", tvWeb)
    }

    @Test
    fun testBuildShareTextContainsDeepLinkAndWebLink() {
        val shareText = DeepLinkHandler.buildShareText(
            displayTitle = "Fight Club",
            mediaId = 550,
            isTv = false,
            releaseYear = "1999"
        )

        assertTrue(shareText.contains("\"Fight Club\" (1999)"))
        assertTrue(shareText.contains("watzem://movie/550"))
        assertTrue(shareText.contains("https://watzem.app/movie/550"))
        assertTrue(shareText.contains(DeepLinkHandler.DOWNLOAD_URL))
    }

    @Test
    fun testParseCustomSchemeMovieAndTv() {
        // watzem://movie/550
        val target1 = DeepLinkHandler.parse("watzem://movie/550")
        assertNotNull(target1)
        assertEquals(550, target1?.mediaId)
        assertEquals(false, target1?.isTv)

        // watzem://tv/1399
        val target2 = DeepLinkHandler.parse("watzem://tv/1399")
        assertNotNull(target2)
        assertEquals(1399, target2?.mediaId)
        assertEquals(true, target2?.isTv)
    }

    @Test
    fun testParseDetailScheme() {
        // watzem://detail/533535/false
        val target = DeepLinkHandler.parse("watzem://detail/533535/false")
        assertNotNull(target)
        assertEquals(533535, target?.mediaId)
        assertEquals(false, target?.isTv)

        // watzem://detail/60735/true
        val targetTv = DeepLinkHandler.parse("watzem://detail/60735/true")
        assertNotNull(targetTv)
        assertEquals(60735, targetTv?.mediaId)
        assertEquals(true, targetTv?.isTv)
    }

    @Test
    fun testParseWebUrls() {
        // https://watzem.app/movie/550
        val target1 = DeepLinkHandler.parse("https://watzem.app/movie/550")
        assertNotNull(target1)
        assertEquals(550, target1?.mediaId)
        assertEquals(false, target1?.isTv)

        // https://watzem.app/tv/1399
        val target2 = DeepLinkHandler.parse("https://watzem.app/tv/1399")
        assertNotNull(target2)
        assertEquals(1399, target2?.mediaId)
        assertEquals(true, target2?.isTv)
    }

    @Test
    fun testParseQueryParams() {
        // https://watzem.app/?movie=550
        val target1 = DeepLinkHandler.parse("https://watzem.app/?movie=550")
        assertNotNull(target1)
        assertEquals(550, target1?.mediaId)
        assertEquals(false, target1?.isTv)

        // watzem://?tv=1399
        val target2 = DeepLinkHandler.parse("watzem://?tv=1399")
        assertNotNull(target2)
        assertEquals(1399, target2?.mediaId)
        assertEquals(true, target2?.isTv)

        // watzem://?id=789&type=tv
        val target3 = DeepLinkHandler.parse("watzem://?id=789&type=tv")
        assertNotNull(target3)
        assertEquals(789, target3?.mediaId)
        assertEquals(true, target3?.isTv)
    }

    @Test
    fun testParseInvalidUrlsReturnNull() {
        assertNull(DeepLinkHandler.parse(null as String?))
        assertNull(DeepLinkHandler.parse("https://google.com"))
        assertNull(DeepLinkHandler.parse("watzem://invalid"))
    }
}
