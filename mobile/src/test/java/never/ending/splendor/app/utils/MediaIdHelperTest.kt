package never.ending.splendor.app.utils

import never.ending.splendor.app.utils.MediaIdHelper.musicId
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for the [MediaIdHelper] class. Exercises the helper methods that
 * do MediaID to MusicID conversion and hierarchy (categories) extraction.
 */
@RunWith(JUnit4::class)
class MediaIdHelperTest {

    @Test
    fun testNormalMediaIDStructure() {
        val mediaId = MediaIdHelper.createMediaId("784343", "BY_GENRE", "Classic 70's")
        Assert.assertEquals(
            "Classic 70's",
            MediaIdHelper.extractBrowseCategoryValueFromMediaID(mediaId)
        )
        Assert.assertEquals("784343", mediaId.musicId)
    }

    @Test
    fun testSpecialSymbolsMediaIDStructure() {
        val mediaID = MediaIdHelper.createMediaId("78A_88|X/3", "BY_GENRE", "Classic 70's")
        Assert.assertEquals(
            "Classic 70's",
            MediaIdHelper.extractBrowseCategoryValueFromMediaID(mediaID)
        )
        Assert.assertEquals("78A_88|X/3", mediaID.musicId)
    }

    @Test
    fun testNullMediaIDStructure() {
        val mediaID = MediaIdHelper.createMediaId(null, "BY_GENRE", "Classic 70's")
        Assert.assertEquals(
            "Classic 70's",
            MediaIdHelper.extractBrowseCategoryValueFromMediaID(mediaID)
        )
        Assert.assertNull(mediaID.musicId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidSymbolsInMediaIDStructure() {
        Assert.fail(MediaIdHelper.createMediaId(null, "BY|GENRE/2", "Classic 70's"))
    }

    @Test
    fun testCreateBrowseCategoryMediaID() {
        val browseMediaID = MediaIdHelper.createMediaId(null, "BY_GENRE", "Rock & Roll")
        Assert.assertEquals(
            "Rock & Roll",
            MediaIdHelper.extractBrowseCategoryValueFromMediaID(browseMediaID)
        )
        val categories = MediaIdHelper.getHierarchy(browseMediaID)
        Assert.assertArrayEquals(categories, arrayOf("BY_GENRE", "Rock & Roll"))
    }

    @Test
    fun testGetParentOfPlayableMediaID() {
        val mediaID = MediaIdHelper.createMediaId("23423423", "BY_GENRE", "Rock & Roll")
        val expectedParentID = MediaIdHelper.createMediaId(null, "BY_GENRE", "Rock & Roll")
        Assert.assertEquals(expectedParentID, MediaIdHelper.getParentMediaID(mediaID))
    }

    @Test
    fun testGetParentOfBrowsableMediaID() {
        val mediaID = MediaIdHelper.createMediaId(null, "BY_GENRE", "Rock & Roll")
        val expectedParentID = MediaIdHelper.createMediaId(null, "BY_GENRE")
        Assert.assertEquals(expectedParentID, MediaIdHelper.getParentMediaID(mediaID))
    }

    @Test
    fun testGetParentOfCategoryMediaID() {
        Assert.assertEquals(
            MediaIdHelper.MEDIA_ID_ROOT,
            MediaIdHelper.getParentMediaID(MediaIdHelper.createMediaId(null, "BY_GENRE"))
        )
    }

    @Test
    fun testGetParentOfRoot() {
        Assert.assertEquals(
            MediaIdHelper.MEDIA_ID_ROOT,
            MediaIdHelper.getParentMediaID(MediaIdHelper.MEDIA_ID_ROOT)
        )
    }
}
