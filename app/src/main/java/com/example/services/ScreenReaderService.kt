package com.example.services

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenReaderService : AccessibilityService() {

  companion object {
    private const val TAG = "ULTRON_SCREEN_READER"

    @Volatile
    var lastCapturedText: String = ""
      private set

    @Volatile
    var lastCapturedTimestamp: Long = 0L
      private set

    @Volatile
    var isServiceRunning: Boolean = false
      private set

    fun clearCapturedText() {
      lastCapturedText = ""
      lastCapturedTimestamp = 0L
    }
  }

  override fun onServiceConnected() {
    super.onServiceConnected()
    isServiceRunning = true
    Log.d(TAG, "ScreenReaderService connected and active.")
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    if (event == null) return

    val eventType = event.eventType
    if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
        eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    ) {
      val rootNode = rootInActiveWindow ?: return
      val textBuilder = StringBuilder()
      extractTextFromNode(rootNode, textBuilder)
      val captured = textBuilder.toString().trim()

      if (captured.isNotEmpty() && captured != lastCapturedText) {
        lastCapturedText = captured
        lastCapturedTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Captured screen text (${captured.length} chars):\n$captured")
      }
    }
  }

  private fun extractTextFromNode(node: AccessibilityNodeInfo?, builder: StringBuilder) {
    if (node == null) return

    val text = node.text?.toString()?.trim()
    val contentDescription = node.contentDescription?.toString()?.trim()

    if (!text.isNullOrEmpty()) {
      builder.append(text).append("\n")
    } else if (!contentDescription.isNullOrEmpty()) {
      builder.append(contentDescription).append("\n")
    }

    val childCount = node.childCount
    for (i in 0 until childCount) {
      val child = node.getChild(i)
      if (child != null) {
        extractTextFromNode(child, builder)
      }
    }
  }

  override fun onInterrupt() {
    Log.d(TAG, "ScreenReaderService interrupted.")
  }

  override fun onDestroy() {
    super.onDestroy()
    isServiceRunning = false
    Log.d(TAG, "ScreenReaderService destroyed.")
  }
}
