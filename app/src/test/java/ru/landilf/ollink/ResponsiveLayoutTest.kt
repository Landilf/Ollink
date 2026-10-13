package ru.landilf.ollink

import org.junit.Assert.*
import org.junit.Test
import ru.landilf.ollink.presentation.ResponsiveLayout

class ResponsiveLayoutTest {
    @Test fun usesActualWindowSizeInsteadOfDeviceOrientation() {
        assertFalse(ResponsiveLayout.calculate(320f, 640f, 1f).persistentSidebar)
        assertFalse(ResponsiveLayout.calculate(560f, 900f, 1f).persistentSidebar)
        assertFalse(ResponsiveLayout.calculate(411f, 300f, 1f).persistentSidebar)
        assertTrue(ResponsiveLayout.calculate(700f, 360f, 1f).persistentSidebar)
        assertTrue(ResponsiveLayout.calculate(1000f, 900f, 1f).persistentSidebar)
    }
    @Test fun keyboardAndLargeFontLeaveSpaceForTheConversation() {
        val large = ResponsiveLayout.calculate(320f, 280f, 2f)
        assertTrue(large.compactHeight)
        assertEquals(1, large.composerMaxLines)
        assertTrue(large.composerMaxHeight <= 84f)
        assertTrue(large.horizontalPadding <= 12f)
        val regular = ResponsiveLayout.calculate(411f, 800f, 1f)
        assertFalse(regular.compactHeight)
        assertTrue(regular.composerMaxLines > large.composerMaxLines)
    }
}
