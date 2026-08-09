package com.ateam.schedulizer

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ateam.schedulizer.data.CalendarSyncManager
import kotlin.time.Duration.Companion.hours

class ICSSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            CalendarSyncManager.syncAll()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

actual object SubscriptionSyncScheduler {
    actual fun schedulePeriodicSync(context: Any?) {
        val ctx = context as Context
        val request = PeriodicWorkRequestBuilder<ICSSyncWorker>(6.hours.inWholeMilliseconds, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "ics_sync", ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }
}