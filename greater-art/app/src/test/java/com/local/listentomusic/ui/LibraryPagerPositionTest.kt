package com.local.listentomusic.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryPagerPositionTest {
    @Test fun leftCenterRightAndMidDragFollowActualPagerOffset() {
        assertEquals(0f, libraryPagerBackgroundPosition(0, 0f), 0f)
        assertEquals(.25f, libraryPagerBackgroundPosition(1, -.5f), 0f)
        assertEquals(.5f, libraryPagerBackgroundPosition(1, 0f), 0f)
        assertEquals(.75f, libraryPagerBackgroundPosition(1, .5f), 0f)
        assertEquals(1f, libraryPagerBackgroundPosition(2, 0f), 0f)
    }

    @Test fun navigationIndicatorUsesTheSameContinuousPagerSource() {
        assertEquals(0f, libraryPagerNavigationPosition(0, 0f), 0f)
        assertEquals(.5f, libraryPagerNavigationPosition(1, -.5f), 0f)
        assertEquals(1f, libraryPagerNavigationPosition(1, 0f), 0f)
        assertEquals(1.5f, libraryPagerNavigationPosition(1, .5f), 0f)
        assertEquals(2f, libraryPagerNavigationPosition(2, 0f), 0f)
    }
}
