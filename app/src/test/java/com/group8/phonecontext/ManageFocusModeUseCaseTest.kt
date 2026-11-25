package com.group8.phonecontext

import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.repository.FocusModeRepository
import com.group8.phonecontext.domain.usecase.FocusTransitionEvent
import com.group8.phonecontext.domain.usecase.ManageFocusModeUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageFocusModeUseCaseTest {

    private class FakeFocusModeRepository(
        var policyAccessGranted: Boolean = true,
        var externalDndActive: Boolean = false
    ) : FocusModeRepository {
        var appFocusActive: Boolean = false
        var activateCalls: Int = 0
        var releaseCalls: Int = 0

        override fun hasNotificationPolicyAccess(): Boolean = policyAccessGranted
        override fun isAppFocusActive(): Boolean = appFocusActive
        override fun isExternalDndActive(): Boolean = externalDndActive && !appFocusActive

        override fun activateFocusMode(): Boolean {
            activateCalls++
            appFocusActive = true
            return true
        }

        override fun releaseFocusMode(): Boolean {
            releaseCalls++
            appFocusActive = false
            return true
        }

        override fun sendTestNotification(): Boolean = true
    }

    @Test
    fun `activates on desk face-down and releases when transitioning to pocket`() {
        val repo = FakeFocusModeRepository()
        val useCase = ManageFocusModeUseCase(repo)

        val d1 = useCase.evaluateWindow(ContextType.DESK_FACE_DOWN, true, true, 1_000L)
        assertEquals(FocusTransitionEvent.ACTIVATED, d1.event)
        assertTrue(d1.isAppFocusActive)
        assertEquals(1, repo.activateCalls)

        // Second consecutive desk window does not re-trigger
        val d2 = useCase.evaluateWindow(ContextType.DESK_FACE_DOWN, true, true, 4_000L)
        assertEquals(FocusTransitionEvent.NO_CHANGE, d2.event)
        assertTrue(d2.isAppFocusActive)
        assertEquals(1, repo.activateCalls)

        // Transitioning to pocket cleanly releases our focus action
        val d3 = useCase.evaluateWindow(ContextType.POCKET_SEATED, true, true, 7_000L)
        assertEquals(FocusTransitionEvent.RELEASED, d3.event)
        assertFalse(d3.isAppFocusActive)
        assertEquals(1, repo.releaseCalls)
    }

    @Test
    fun `respects pre-existing user DND and does not disable it on desk exit`() {
        val repo = FakeFocusModeRepository(externalDndActive = true)
        val useCase = ManageFocusModeUseCase(repo)

        val d1 = useCase.evaluateWindow(ContextType.DESK_FACE_DOWN, true, true, 1_000L)
        assertEquals(FocusTransitionEvent.SKIPPED_EXISTING_DND, d1.event)
        assertFalse(d1.isAppFocusActive)
        assertEquals(0, repo.activateCalls)

        val d2 = useCase.evaluateWindow(ContextType.WALKING_IN_HAND, true, true, 4_000L)
        assertEquals(FocusTransitionEvent.NO_CHANGE, d2.event)
        assertEquals(0, repo.releaseCalls)
    }

    @Test
    fun `returns PERMISSION_REQUIRED when notification policy access is missing`() {
        val repo = FakeFocusModeRepository(policyAccessGranted = false)
        val useCase = ManageFocusModeUseCase(repo)

        val d1 = useCase.evaluateWindow(ContextType.DESK_FACE_DOWN, true, true, 1_000L)
        assertEquals(FocusTransitionEvent.PERMISSION_REQUIRED, d1.event)
        assertEquals(0, repo.activateCalls)
    }
}
