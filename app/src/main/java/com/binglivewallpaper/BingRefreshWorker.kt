package com.binglivewallpaper

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Daily worker that downloads the Bing image of the day and stores it on
 * disk for the wallpaper service to render.
 *
 * Constraints: unmetered network + battery not low.
 * Schedule: every day at 11:00 UTC.
 */
class BingRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "doWork start (attempt $runAttemptCount)")
        return@withContext try {
            val result = BingImageFetcher.fetch()
            ImageStore.save(applicationContext, result.bitmap, result.url, result.date)
            Log.d(TAG, "doWork success: ${result.url}")
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "doWork failed: ${t.message}", t)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "BingRefreshWorker"
        private const val WORK_NAME = "bing_daily_refresh"
        private const val TARGET_HOUR_UTC = 11
        private const val INITIAL_BACKOFF_MINUTES = 30L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val initialDelay = initialDelayToNextUtc(TARGET_HOUR_UTC)
            Log.d(TAG, "Scheduling periodic work; initial delay = ${initialDelay / 1000}s")

            val request = PeriodicWorkRequestBuilder<BingRefreshWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_MINUTES,
                    TimeUnit.MINUTES,
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request,
            )
        }

        /**
         * Kicks off an immediate one-time fetch, bypassing the daily schedule.
         * Uses the same constraints so it still respects unmetered-network and
         * battery-not-low requirements.
         */
        fun runOnceNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<BingRefreshWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    INITIAL_BACKOFF_MINUTES,
                    TimeUnit.MINUTES,
                )
                .build()

            Log.d(TAG, "Enqueuing one-time fetch now")
            WorkManager.getInstance(context).enqueue(request)
        }

        /**
         * Milliseconds from now until the next occurrence of [hourUtc]:00 UTC.
         */
        internal fun initialDelayToNextUtc(hourUtc: Int): Long {
            val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            val next = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hourUtc)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (next.timeInMillis <= now.timeInMillis) {
                next.add(Calendar.DAY_OF_YEAR, 1)
            }
            return next.timeInMillis - now.timeInMillis
        }
    }
}
