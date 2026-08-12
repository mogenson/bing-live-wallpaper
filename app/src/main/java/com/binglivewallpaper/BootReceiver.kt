package com.binglivewallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-schedules the daily refresh work after a device reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "BOOT_COMPLETED received; scheduling refresh work")
            BingRefreshWorker.schedule(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
