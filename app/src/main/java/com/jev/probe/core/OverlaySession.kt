package com.jev.probe.core

/** Invalidates delayed work when the foreground app or permission scope changes. */
class OverlaySession {
    @Volatile var generation = 0L
        private set
    @Volatile var packageName: String? = null
        private set
    @Volatile var allowed = false
        private set

    fun update(pkg: String?, permitted: Boolean): Boolean {
        if (packageName == pkg && allowed == permitted) return false
        packageName = pkg
        allowed = permitted
        invalidate()
        return true
    }

    fun invalidate() { generation++ }

    fun accepts(token: Long, pkg: String?): Boolean =
        allowed && pkg != null && packageName == pkg && generation == token
}
