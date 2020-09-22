package never.ending.splendor.app.utils

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for the [MediaIDHelper] class. Exercises the helper methods that
 * do MediaID to MusicID conversion and hierarchy (categories) extraction.
 */
@RunWith(JUnit4::class)
class MediaIDHelperTest {

    @Test
    fun testNormalMediaIDStructure() {
        val mediaID = MediaIDHelper.createMediaID("784343", "BY_GENRE", "Classic 70's")
        Assert.assertEquals(
            "Classic 70's",
            MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaID)
        )
        Assert.assertEquals("784343", MediaIDHelper.extractMusicIDFromMediaID(mediaID))
    }

    @Test
    fun testSpecialSymbolsMediaIDStructure() {
        val mediaID = MediaIDHelper.createMediaID("78A_88|X/3", "BY_GENRE", "Classic 70's")
        Assert.assertEquals(
            "Classic 70's",
            MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaID)
        )
        Assert.assertEquals("78A_88|X/3", MediaIDHelper.extractMusicIDFromMediaID(mediaID))
    }

    @Test
    fun testNullMediaIDStructure() {
        val mediaID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Classic 70's")
        Assert.assertEquals(
            "Classic 70's",
            MediaIDHelper.extractBrowseCategoryValueFromMediaID(mediaID)
        )
        Assert.assertNull(MediaIDHelper.extractMusicIDFromMediaID(mediaID))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSymbolsInMediaIDStructure() {
        Assert.fail(MediaIDHelper.createMediaID(null, "BY|GENRE/2", "Classic 70's"))
    }

    @Test
    fun testCreateBrowseCategoryMediaID() {
        val browseMediaID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Rock & Roll")
        Assert.assertEquals(
            "Rock & Roll",
            MediaIDHelper.extractBrowseCategoryValueFromMediaID(browseMediaID)
        )
        val categories = MediaIDHelper.getHierarchy(browseMediaID)
        Assert.assertArrayEquals(categories, arrayOf("BY_GENRE", "Rock & Roll"))
    }

    @Test
    fun testGetParentOfPlayableMediaID() {
        val mediaID = MediaIDHelper.createMediaID("23423423", "BY_GENRE", "Rock & Roll")
        val expectedParentID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Rock & Roll")
        Assert.assertEquals(expectedParentID, MediaIDHelper.getParentMediaID(mediaID))
    }

    @Test
    fun testGetParentOfBrowsableMediaID() {
        val mediaID = MediaIDHelper.createMediaID(null, "BY_GENRE", "Rock & Roll")
        val expectedParentID = MediaIDHelper.createMediaID(null, "BY_GENRE")
        Assert.assertEquals(expectedParentID, MediaIDHelper.getParentMediaID(mediaID))
    }

    @Test
    fun testGetParentOfCategoryMediaID() {
        Assert.assertEquals(
            MediaIDHelper.MEDIA_ID_ROOT,
            MediaIDHelper.getParentMediaID(MediaIDHelper.createMediaID(null, "BY_GENRE"))
        )
    }

    @Test
    fun testGetParentOfRoot() {
        Assert.assertEquals(
            MediaIDHelper.MEDIA_ID_ROOT,
            MediaIDHelper.getParentMediaID(MediaIDHelper.MEDIA_ID_ROOT)
        )
    }
}
