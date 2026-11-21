package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class DashcamCatalogTest {
    private static String norm(String name) {
        return "sd//norm/" + name + ".MP4";
    }

    @Test
    public void groupsContinuousFilesIntoRangesWithEstimatedEnd() {
        DashcamCatalog catalog = DashcamCatalog.fromPaths(Arrays.asList(
                norm("2026_11_21_081800_00"),
                norm("2026_11_21_081200_00"),
                norm("2026_11_21_081500_00"),
                norm("2026_11_21_103600_00"),
                norm("2026_11_21_103900_00")));

        assertEquals(Duration.ofMinutes(3), catalog.getEstimatedClipDuration());
        LocalDate day = LocalDate.of(2026, 11, 21);
        assertTrue(catalog.hasRecordingsOn(day));
        List<DashcamCatalog.Segment> segments = catalog.getSegments(day);
        assertEquals(2, segments.size());
        // Newest first, as in the gallery.
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 36), segments.get(0).getStart());
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 42), segments.get(0).getEstimatedEnd());
        DashcamCatalog.Segment morning = segments.get(1);
        assertEquals(3, morning.getClips().size());
        assertEquals(LocalDateTime.of(2026, 11, 21, 8, 12), morning.getStart());
        assertEquals(LocalDateTime.of(2026, 11, 21, 8, 21), morning.getEstimatedEnd());
        assertEquals(LocalDateTime.of(2026, 11, 21, 8, 15),
                morning.getClips().get(0).getEstimatedEnd());
        assertEquals(day, catalog.getLatestDay());
    }

    @Test
    public void invalidDatesStayVisibleAsUndated() {
        DashcamCatalog catalog = DashcamCatalog.fromPaths(Arrays.asList(
                norm("2026_02_30_101010_00"),
                norm("CLIP0001"),
                norm("2026_11_20_235900_00")));

        assertEquals(2, catalog.getUndated().size());
        assertEquals(1, catalog.getRecordings().size());
        assertNull(DashcamCatalog.parseStart("2026_13_01_000000_00.MP4"));
    }

    @Test
    public void rangesNeverCrossMidnightAndUseDefaultClipWithoutCadence() {
        DashcamCatalog catalog = DashcamCatalog.fromPaths(Arrays.asList(
                norm("2026_11_20_235900_00"),
                "sd//emr/2026_11_21_000100_00.MP4"));

        assertEquals(DashcamCatalog.DEFAULT_CLIP, catalog.getEstimatedClipDuration());
        assertEquals(1, catalog.getSegments(LocalDate.of(2026, 11, 20)).size());
        DashcamCatalog.Segment next = catalog.getSegments(LocalDate.of(2026, 11, 21)).get(0);
        assertTrue(next.hasEmergency());
        assertTrue(next.getClips().get(0).getRecording().isEmergency());
        assertFalse(catalog.hasRecordingsOn(LocalDate.of(2026, 11, 22)));
    }

    @Test
    public void ignoresPathsThatAreNotCatalogEntries() {
        DashcamCatalog catalog = DashcamCatalog.fromPaths(Arrays.asList(
                "sd//photo/2026_11_21_081200_00.JPG", "../x.MP4", null));
        assertTrue(catalog.getRecordings().isEmpty());
        assertTrue(catalog.getUndated().isEmpty());
        assertNull(catalog.getLatestDay());
    }
}
