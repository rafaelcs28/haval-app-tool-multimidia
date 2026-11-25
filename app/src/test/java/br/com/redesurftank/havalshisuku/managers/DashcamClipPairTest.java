package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DashcamClipPairTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private File file(String name, int bytes) throws IOException {
        File f = new File(tmp.getRoot(), name);
        try (FileOutputStream out = new FileOutputStream(f)) { out.write(new byte[bytes]); }
        return f;
    }

    @Test
    public void originalIsRemovedOnlyWhenTheStampedCopyIsReal() throws IOException {
        File original = file("2026_1125_080000_001.mp4", 10);
        file("2026_1125_080000_001.mp4.sha256", 1);
        File emptyStamp = file("2026_1125_080000_001_carimbo.mp4", 0);
        assertFalse(DashcamClipStore.removeOriginalAfterStamp(original, emptyStamp));
        assertTrue(original.exists());

        File stamped = file("2026_1125_080000_001_carimbo.mp4", 10);
        assertTrue(DashcamClipStore.removeOriginalAfterStamp(original, stamped));
        assertFalse(original.exists());
        assertFalse(new File(tmp.getRoot(), "2026_1125_080000_001.mp4.sha256").exists());
        assertTrue(stamped.exists());
    }

    @Test
    public void sharedSidecarsBelongToWhicheverHalfRemains() throws IOException {
        File original = file("2026_1125_080100_002.mp4", 10);
        File stamped = file("2026_1125_080100_002_carimbo.mp4", 10);
        assertTrue(DashcamClipStore.hasPartner(original));
        assertTrue(DashcamClipStore.hasPartner(stamped));
        assertTrue(original.delete());
        assertFalse(DashcamClipStore.hasPartner(stamped));
    }

    @Test
    public void stampedCopyFindsTheOriginalsTrail() {
        assertTrue(DashcamClipStore.gpxName("2026_1125_080100_002_carimbo.mp4")
                .equals(DashcamClipStore.gpxName("2026_1125_080100_002.mp4")));
    }
}
