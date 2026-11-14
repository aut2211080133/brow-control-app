
package com.example.browcontrol

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    private fun performScroll(action: Int) {
        rootInActiveWindow?.performAction(action)
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        private var instance: MyAccessibilityService? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun scrollDown() {
            mainHandler.post {
                instance?.performScroll(
                    AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                )
            }
        }

        fun scrollUp() {
            mainHandler.post {
                instance?.performScroll(
                    AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                )
            }
        }
    }
}
