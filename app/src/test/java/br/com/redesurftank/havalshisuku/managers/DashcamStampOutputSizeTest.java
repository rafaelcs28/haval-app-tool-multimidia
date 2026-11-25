package br.com.redesurftank.havalshisuku.managers;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class DashcamStampOutputSizeTest {
    @Test
    public void fourKIsScaledTo1080pAndSmallerSourcesStay() {
        assertArrayEquals(new int[]{1920, 1080}, DashcamStampEncoder.outputSize(3840, 2160));
        assertArrayEquals(new int[]{1920, 1080}, DashcamStampEncoder.outputSize(1920, 1080));
        assertArrayEquals(new int[]{1280, 720}, DashcamStampEncoder.outputSize(1280, 720));
        assertArrayEquals(new int[]{1440, 1080}, DashcamStampEncoder.outputSize(2880, 2160));
    }
}
