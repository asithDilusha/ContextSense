package com.group8.phonecontext.domain.usecase

import com.group8.phonecontext.domain.model.ContextType
import com.group8.phonecontext.domain.repository.FocusModeRepository

enum class FocusTransitionEvent {
    ACTIVATED,
    RELEASED,
    NO_CHANGE,
    SKIPPED_EXISTING_DND,
    PERMISSION_REQUIRED
}

data class FocusDecision(
    val event: FocusTransitionEvent,
    val isAppFocusActive: Boolean,
    val timestampMs: Long
)

/**
 * Coordinates Automatic Focus Mode (DND) transitions cleanly after each complete 3-second window.
 *
 * - Activates DND when a full window is classified as [ContextType.DESK_FACE_DOWN].
 * - Releases only this app's focus action when the desk context ends.
 * - Preserves any pre-existing user-enabled DND when [respectExistingUserDnd] is enabled.
 */
class ManageFocusModeUseCase(
    private val focusModeRepository: FocusModeRepository
) {
    private var skippedDueToExistingUserDnd: Boolean = false

    fun evaluateWindow(
        predictedContext: ContextType,
        autoFocusEnabled: Boolean,
        respectExistingUserDnd: Boolean,
        nowMs: Long
    ): FocusDecision {
        val currentlyActiveByApp = focusModeRepository.isAppFocusActive()

        if (!autoFocusEnabled) {
            return if (currentlyActiveByApp) {
                focusModeRepository.releaseFocusMode()
                skippedDueToExistingUserDnd = false
                FocusDecision(FocusTransitionEvent.RELEASED, isAppFocusActive = false, timestampMs = nowMs)
            } else {
                FocusDecision(FocusTransitionEvent.NO_CHANGE, isAppFocusActive = false, timestampMs = nowMs)
            }
        }

        if (!focusModeRepository.hasNotificationPolicyAccess()) {
            return FocusDecision(
                event = FocusTransitionEvent.PERMISSION_REQUIRED,
                isAppFocusActive = false,
                timestampMs = nowMs
            )
        }

        return if (predictedContext == ContextType.DESK_FACE_DOWN) {
            if (currentlyActiveByApp) {
                FocusDecision(FocusTransitionEvent.NO_CHANGE, isAppFocusActive = true, timestampMs = nowMs)
            } else if (respectExistingUserDnd && focusModeRepository.isExternalDndActive()) {
                skippedDueToExistingUserDnd = true
                FocusDecision(
                    event = FocusTransitionEvent.SKIPPED_EXISTING_DND,
                    isAppFocusActive = false,
                    timestampMs = nowMs
                )
            } else {
                val activated = focusModeRepository.activateFocusMode()
                skippedDueToExistingUserDnd = false
                FocusDecision(
                    event = if (activated) FocusTransitionEvent.ACTIVATED else FocusTransitionEvent.NO_CHANGE,
                    isAppFocusActive = focusModeRepository.isAppFocusActive(),
                    timestampMs = nowMs
                )
            }
        } else {
            if (currentlyActiveByApp) {
                focusModeRepository.releaseFocusMode()
                skippedDueToExistingUserDnd = false
                FocusDecision(FocusTransitionEvent.RELEASED, isAppFocusActive = false, timestampMs = nowMs)
            } else {
                skippedDueToExistingUserDnd = false
                FocusDecision(FocusTransitionEvent.NO_CHANGE, isAppFocusActive = false, timestampMs = nowMs)
            }
        }
    }

    fun releaseIfOwned(nowMs: Long): FocusDecision {
        return if (focusModeRepository.isAppFocusActive()) {
            focusModeRepository.releaseFocusMode()
            skippedDueToExistingUserDnd = false
            FocusDecision(FocusTransitionEvent.RELEASED, isAppFocusActive = false, timestampMs = nowMs)
        } else {
            FocusDecision(FocusTransitionEvent.NO_CHANGE, isAppFocusActive = false, timestampMs = nowMs)
        }
    }
}
