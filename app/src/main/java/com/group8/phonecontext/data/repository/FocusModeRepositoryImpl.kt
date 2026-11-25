package com.group8.phonecontext.data.repository

import com.group8.phonecontext.data.datasource.SystemFocusDataSource
import com.group8.phonecontext.domain.repository.FocusModeRepository

class FocusModeRepositoryImpl(
    private val systemFocusDataSource: SystemFocusDataSource
) : FocusModeRepository {

    override fun hasNotificationPolicyAccess(): Boolean =
        systemFocusDataSource.hasNotificationPolicyAccess()

    override fun isAppFocusActive(): Boolean =
        systemFocusDataSource.isAppFocusActive()

    override fun isExternalDndActive(): Boolean =
        systemFocusDataSource.isExternalDndActive()

    override fun activateFocusMode(): Boolean =
        systemFocusDataSource.activateFocusMode()

    override fun releaseFocusMode(): Boolean =
        systemFocusDataSource.releaseFocusMode()

    override fun sendTestNotification(): Boolean =
        systemFocusDataSource.sendTestNotification()
}
