package com.ateam.schedulizer.data

import com.ateam.schedulizer.ReminderSchedule
import com.ateam.schedulizer.databaseInstance
import com.ateam.schedulizer.entities.CalendarSubscription
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object CalendarSyncManager {

    suspend fun addSubscription(url: String, name: String): Result<CalendarSubscription> = runCatching {
        // Validate first so a bad link never gets to the database
        ICSURLFetcher.fetch(url).getOrThrow()

        val subscription = CalendarSubscription(url = url, name = name)
        val id = databaseInstance.taskDao().let {
            it.upsert(subscription)
            it.getAllSubscriptions().last { s -> s.url == url }.id
        }
        val saved = subscription.copy(id = id)
        sync(saved).getOrThrow()
        saved
    }

    suspend fun saveSubscription(
        existing: CalendarSubscription?,
        name: String,
        url: String
    ): Result<Unit> = runCatching {
        if (existing == null) {
            addSubscription(url, name).getOrThrow()
            return@runCatching
        }

        ICSURLFetcher.fetch(url).getOrThrow()

        // Read the row so a stale copy from the dialog can't overwrite properties
        val latest = databaseInstance.taskDao().getSubscription(existing.id)
            ?: error("Subscription no longer exists")

        val urlChanged = latest.url != url
        if (urlChanged) databaseInstance.removeSubscriptionTasks(latest.id)

        val updated = latest.copy(
            name = name,
            url = url,
            lastSyncedAt = if (urlChanged) null else latest.lastSyncedAt
        )
        databaseInstance.taskDao().upsert(updated)
        databaseInstance.refresh()

        if (urlChanged && updated.enabled) sync(updated).getOrThrow()
    }

    suspend fun setEnabled(subscription: CalendarSubscription, enabled: Boolean): Result<Unit> = runCatching {
        val updated = databaseInstance.setSubscriptionEnabled(subscription, enabled)
        if (enabled) sync(updated).getOrThrow()
    }

    suspend fun removeSubscription(subscription: CalendarSubscription, deleteImportedTasks: Boolean = true) {
        if (deleteImportedTasks) {
            databaseInstance.taskDao().getSetTasksForSubscription(subscription.id)
                .forEach { databaseInstance.deleteTask(it) }
        }
        databaseInstance.taskDao().deleteSubscription(subscription.id)
        databaseInstance.refresh()
    }

    suspend fun syncAll() {
        databaseInstance.taskDao().getAllSubscriptions()
            .filter { it.autoSyncEnabled && it.enabled }
            .forEach { sync(it) }
    }

    suspend fun sync(subscription: CalendarSubscription): Result<Unit> = runCatching {
        // Never sync a disabled (or deleted) subscription
        val latest = databaseInstance.taskDao().getSubscription(subscription.id)
            ?: error("Subscription no longer exists")
        if (!latest.enabled) error("Subscription is disabled")

        val icsText = ICSURLFetcher.fetch(latest.url).getOrThrow()
        val fetched = ICSParser.parseVEvents(icsText, ReminderSchedule.None, latest.id)
        val existing = databaseInstance.taskDao().getSetTasksForSubscription(latest.id)

        val existingByUid = existing.filter { it.icsUid != null }.associateBy { it.icsUid }
        val fetchedUids = fetched.mapNotNull { it.icsUid }.toSet()

        // Upsert new or changed, preserving the completion on events
        fetched.forEach { incoming ->
            val match = incoming.icsUid?.let { existingByUid[it] }
            val toSave = if (match != null) {
                incoming.copy(
                    id = match.id,
                    completed = match.completed,
                    completionDate = match.completionDate
                )
            } else incoming
            databaseInstance.upsertTask(toSave)
        }

        // Remove tasks where the source event no longer exists in the feed.
        existing.filter { it.icsUid != null && it.icsUid !in fetchedUids }
            .forEach { databaseInstance.deleteTask(it) }

        // Read again so we do not break edits made while the sync was running
        databaseInstance.taskDao().getSubscription(latest.id)?.let { current ->
            databaseInstance.taskDao().upsert(
                current.copy(lastSyncedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
            )
        }
        databaseInstance.refresh()
    }
}