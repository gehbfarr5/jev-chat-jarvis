package com.jev.probe.core

import org.junit.Assert.*
import org.junit.Test

class OverlaySessionTest {
    @Test fun leavingWechatRejectsDelayedAnalysisAndOcr() {
        val s = OverlaySession()
        s.update("com.tencent.mm", true)
        val token = s.generation
        assertTrue(s.accepts(token, "com.tencent.mm"))
        s.update("com.android.settings", false)
        assertFalse(s.accepts(token, "com.tencent.mm"))
        assertFalse(s.accepts(s.generation, "com.android.settings"))
    }

    @Test fun returningToSameAppDoesNotReviveOldRequests() {
        val s = OverlaySession()
        s.update("com.tencent.mm", true)
        val token = s.generation
        s.update("com.android.launcher", false)
        s.update("com.tencent.mm", true)
        assertFalse(s.accepts(token, "com.tencent.mm"))
        assertTrue(s.accepts(s.generation, "com.tencent.mm"))
    }

    @Test fun switchingBetweenSelectedAppsRejectsPreviousResults() {
        val s = OverlaySession()
        s.update("com.tencent.mm", true)
        val token = s.generation
        s.update("com.tencent.mobileqq", true)
        assertFalse(s.accepts(token, "com.tencent.mm"))
        assertFalse(s.accepts(token, "com.tencent.mobileqq"))
    }

    @Test fun clearingSelectionOrDisablingMasterSwitchInvalidatesWork() {
        val s = OverlaySession()
        s.update("com.tencent.mm", true)
        val token = s.generation
        s.update("com.tencent.mm", false)
        assertFalse(s.accepts(token, "com.tencent.mm"))
        assertFalse(s.accepts(s.generation, "com.tencent.mm"))
    }

    @Test fun ordinaryEventsKeepCurrentSession() {
        val s = OverlaySession()
        s.update("com.tencent.mm", true)
        val token = s.generation
        assertFalse(s.update("com.tencent.mm", true))
        assertTrue(s.accepts(token, "com.tencent.mm"))
    }

    @Test fun unknownForegroundAndNewConversationRejectOldWork() {
        val s = OverlaySession()
        s.update("com.tencent.mm", true)
        var token = s.generation
        s.invalidate()
        assertFalse(s.accepts(token, "com.tencent.mm"))
        token = s.generation
        s.update(null, false)
        assertFalse(s.accepts(token, "com.tencent.mm"))
        assertFalse(s.accepts(s.generation, null))
    }
}
