package com.financeobserver.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that restarts the notification listener on device boot.
 * Ensures the app continues monitoring after a reboot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed - services will restart automatically")
            // Notification Listener Service is automatically restarted by Android
            // when the device boots, as long as it's declared in the manifest.
            // No additional action needed here.
        }
    }
}
