package app.unpeep.privacy.optics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Which preview and analysis sizes the finders ask for, and the 2× crop on older phones. */
class CameraSizesTest {
    private fun s(w: Int, h: Int) = Dim(w, h)

    @Test fun thePreviewIsTheLargest43UpTo1920() {
        assertEquals(s(1440, 1080), CameraSizes.preview(listOf(s(4032, 3024), s(1920, 1080), s(1440, 1080), s(640, 480))))
    }

    @Test fun withNo43PreviewTheLargestThatFitsIsUsed() {
        assertEquals(s(1920, 1080), CameraSizes.preview(listOf(s(3840, 2160), s(1920, 1080), s(1280, 720))))
    }

    @Test fun aPhoneWithOnlyHugeSizesStillGetsTheSmallest() {
        assertEquals(s(2560, 1920), CameraSizes.preview(listOf(s(4032, 3024), s(2560, 1920))))
        assertNull(CameraSizes.preview(emptyList()))
    }

    @Test fun analysisIsTheSmallestOfTheSameShapeAtLeast320Wide() {
        assertEquals(s(320, 240), CameraSizes.analysis(listOf(s(1440, 1080), s(640, 480), s(320, 240), s(176, 144), s(320, 180)), s(1440, 1080)))
    }

    @Test fun analysisFallsBackWhenNoSizeIsBigEnoughOrOfTheShape() {
        assertEquals(s(176, 132), CameraSizes.analysis(listOf(s(176, 132), s(160, 120)), s(1440, 1080)))
        assertEquals(s(352, 288), CameraSizes.analysis(listOf(s(352, 288), s(1280, 720)), s(1440, 1080)))
        assertNull(CameraSizes.analysis(emptyList(), s(1440, 1080)))
    }

    @Test fun twoTimesZoomIsTheMiddleHalfOfTheSensor() {
        assertEquals(Box(1000, 750, 3000, 2250), CameraSizes.crop(4000, 3000, 2f))
        assertEquals(Box(0, 0, 4000, 3000), CameraSizes.crop(4000, 3000, 1f))
        assertEquals(Box(0, 0, 4000, 3000), CameraSizes.crop(4000, 3000, 0.5f))
    }
}
