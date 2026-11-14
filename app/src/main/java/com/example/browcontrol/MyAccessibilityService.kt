
package com.example.browcontrol

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Accessibility events are handled here.
    }

    override fun onInterrupt() {
        // Called when the service is interrupted.
    }

    fun scrollDown() {
        rootInActiveWindow?.performAction(
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        )
    }

    fun scrollUp() {
        rootInActiveWindow?.performAction(
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        )
    }
}
