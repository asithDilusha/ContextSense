package com.group8.phonecontext.domain.repository

interface FocusModeRepository {
    fun hasNotificationPolicyAccess(): Boolean
    fun isAppFocusActive(): Boolean
    fun isExternalDndActive(): Boolean
    fun activateFocusMode(): Boolean
    fun releaseFocusMode(): Boolean
    fun sendTestNotification(): Boolean
}
