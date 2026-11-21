package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class StorageCleanerTest {
    @Test
    public void onlyPlainPathsInsideTheTwoRootsAreAllowed() {
        assertTrue(StorageCleaner.isAllowedPath("/data/local/tmp/impulse-74153.apk"));
        assertTrue(StorageCleaner.isAllowedPath("/storage/emulated/0/Download/Downloader/app-release.apk"));
        assertTrue(StorageCleaner.isAllowedPath("/sdcard/Android/data/x/files/a.TS"));

        assertFalse(StorageCleaner.isAllowedPath("/data/local/tmp"));
        assertFalse(StorageCleaner.isAllowedPath("/data/local/tmp/"));
        assertFalse(StorageCleaner.isAllowedPath("/data/local/tmp/../../system/app/x.apk"));
        assertFalse(StorageCleaner.isAllowedPath("/data/local/tmpx/a.apk"));
        assertFalse(StorageCleaner.isAllowedPath("/data/data/br.com.x/files/a"));
        assertFalse(StorageCleaner.isAllowedPath("/system/app/AndroidAuto.apk"));
        assertFalse(StorageCleaner.isAllowedPath("/data/local/tmp//a"));
        assertFalse(StorageCleaner.isAllowedPath("relative/a.apk"));
        assertFalse(StorageCleaner.isAllowedPath("/data/local/tmp/a\nb"));
    }

    @Test
    public void parsesStatOutputKeepingLargeTmpFilesOnly() {
        List<StorageCleaner.Entry> entries = StorageCleaner.parseStatLines(
                "104475563|1762083780|/data/local/tmp/impulse-74153.apk\n"
                        + "2037911|1755011460|/data/local/tmp/wz.log\n"
                        + "80462235|1750952280|/data/local/tmp/ac3_test.log\n"
                        + "99999999|1|/system/app/x.apk\n"
                        + "garbage line\n",
                StorageCleaner.MIN_SIZE_BYTES);

        assertEquals(2, entries.size());
        assertEquals("impulse-74153.apk", entries.get(0).getName());
        assertEquals("/data/local/tmp", entries.get(0).getFolder());
        assertEquals(1762083780000L, entries.get(0).modifiedMs);
        assertTrue(entries.get(0).viaShizuku);
        assertEquals("Log", entries.get(1).getCategory());
    }

    @Test
    public void categorizesByExtension() {
        assertEquals("Instalador (APK)", StorageCleaner.category("diag5.apk"));
        assertEquals("Vídeo", StorageCleaner.category("20261120140811_035418.TS"));
        assertEquals("Log", StorageCleaner.category("nav2.log"));
        assertEquals("Arquivo", StorageCleaner.category("fridaserver"));
    }
}
