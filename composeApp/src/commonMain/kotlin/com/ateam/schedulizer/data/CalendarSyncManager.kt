package com.ateam.schedulizer.data

import com.ateam.schedulizer.ReminderSchedule
import com.ateam.schedulizer.databaseInstance
import com.ateam.schedulizer.entities.CalendarSubscription
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object CalendarSyncManager {

    suspend fun addSubscription(url: String, name: String): Result<CalendarSubscription> = runCatching {
        val subscription = CalendarSubscription(url = url, name = name)
        val id = databaseInstance.taskDao().let {
            it.upsert(subscription)
            it.getAllSubscriptions().last { s -> s.url == url }.id
        }
        val saved = subscription.copy(id = id)
        sync(saved).getOrThrow()
        saved
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
            .filter { it.autoSyncEnabled }
            .forEach { sync(it) }
    }

    suspend fun sync(subscription: CalendarSubscription): Result<Unit> = runCatching {
        val icsText = ICSURLFetcher.fetch(subscription.url).getOrThrow()
        val fetched = ICSParser.parseVEvents(icsText, ReminderSchedule.None, subscription.id)
        val existing = databaseInstance.taskDao().getSetTasksForSubscription(subscription.id)

        val existingByUid = existing.filter { it.icsUid != null }.associateBy { it.icsUid }
        val fetchedUids = fetched.mapNotNull { it.icsUid }.toSet()

        // Upsert new/changed, preserving completion state on events
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

        // Remove tasks whose source event no longer exists in the feed.
        existing.filter { it.icsUid != null && it.icsUid !in fetchedUids }
            .forEach { databaseInstance.deleteTask(it) }

        databaseInstance.taskDao().upsert(
            subscription.copy(lastSyncedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
        )
        databaseInstance.refresh()
    }
}