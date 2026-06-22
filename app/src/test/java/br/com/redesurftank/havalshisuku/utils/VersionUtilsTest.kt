package br.com.redesurftank.havalshisuku.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionUtilsTest {

    @Test
    fun trailingZeroEqualsShorter() {
        // O bug original: "67.7.0" era considerado > "67.7" (update fantasma).
        assertEquals(0, VersionUtils.compareVersions("67.7.0", "67.7"))
        assertEquals(0, VersionUtils.compareVersions("67.7", "67.7.0"))
        assertEquals(0, VersionUtils.compareVersions("1.0.0.67.7.0", "1.0.0.67.7"))
    }

    @Test
    fun newerIsGreater() {
        assertTrue(VersionUtils.compareVersions("67.8", "67.7") > 0)
        assertTrue(VersionUtils.compareVersions("67.7", "67.8") < 0)
        assertTrue(VersionUtils.compareVersions("1.0.0.67.10", "1.0.0.67.9") > 0)
    }

    @Test
    fun previewSuffixIgnored() {
        assertEquals(0, VersionUtils.compareVersions("1.0.0.67.7-preview", "1.0.0.67.7"))
        assertTrue(VersionUtils.compareVersions("1.0.0.67.8-preview", "1.0.0.67.7") > 0)
        assertTrue(VersionUtils.compareVersions("1.0.0.67.7-preview", "1.0.0.67.8") < 0)
    }

    @Test
    fun equalVersions() {
        assertEquals(0, VersionUtils.compareVersions("67.7", "67.7"))
        assertEquals(0, VersionUtils.compareVersions("1.0.0.67.7", "1.0.0.67.7"))
    }

    @Test
    fun nullAndBlankSafe() {
        assertTrue(VersionUtils.compareVersions(null, "67.7") < 0)
        assertTrue(VersionUtils.compareVersions("67.7", null) > 0)
        assertEquals(0, VersionUtils.compareVersions(null, null))
        assertEquals(0, VersionUtils.compareVersions("", ""))
    }

    @Test
    fun nonNumericComponentTreatedAsZero() {
        assertEquals(0, VersionUtils.compareVersions("67.x", "67.0"))
        assertEquals(0, VersionUtils.compareVersions("67.0", "67.x"))
    }

    @Test
    fun tagWithVPrefixHandled() {
        // Tags de release vinham com "v" (v1.0.0.67.11); a versao instalada vem sem (1.0.0.67.7).
        assertTrue(VersionUtils.compareVersions("v1.0.0.67.11-preview", "1.0.0.67.7-preview") > 0)
        assertEquals(0, VersionUtils.compareVersions("v1.0.0.67.7", "1.0.0.67.7"))
    }
}
