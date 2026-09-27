package ru.finney.pet.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import ru.finney.pet.FinneyApplication
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Напоминание о питомце. Каждый запуск решает, писать ли сейчас ([ReminderPolicy]),
 * и ставит следующий: цепочка разовых задач, а не периодическая — в демо-режиме
 * напоминание раз в пару минут, а периодическая задача WorkManager не бывает чаще 15 минут.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FinneyApplication).container
        val scheduler = container.notifications
        val saved = container.session.activeGame.first() ?: return Result.success()
        val state = saved.state
        val rule = container.content.economy.reminders
        val interval = ReminderPolicy.interval(rule, state.isDemo)
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val lastOpened = container.notificationPrefs.lastOpened()?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }

        // Спящего не зовём: о нём напомнит «проснулся».
        val decision = if (state.sleepingSince != null) ReminderDecision.Skip else ReminderPolicy.decide(now, lastOpened, state.isDemo, rule)
        when (decision) {
            ReminderDecision.Send -> {
                // Приложение на экране — ребёнок и так с питомцем.
                if (!container.appVisible) {
                    val wish = ReminderTexts.wish(state.pet.copy(energy = container.game.energyAt(state)), container.content.economy.pet)
                    Notifier.post(applicationContext, Notifier.ID_REMINDER, ReminderTexts.reminder(saved.profile.petName, wish))
                }
                scheduler.scheduleReminder(interval, fromWorker = true)
            }
            is ReminderDecision.Later -> scheduler.scheduleReminder(Duration.between(now, decision.at), fromWorker = true)
            ReminderDecision.Skip -> scheduler.scheduleReminder(interval, fromWorker = true)
        }
        return Result.success()
    }
}

/** «Питомец проснулся» — в момент, когда сон кончается сам. */
class WakeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as FinneyApplication).container
        val saved = container.session.activeGame.first() ?: return Result.success()
        // Ещё спит тем же сном или уже проснулся сам (разбудили раньше — задачу сняли).
        // Уснул снова — у нового сна своя задача.
        val since = inputData.getLong(KEY_SLEEPING_SINCE, -1)
        val sleeping = saved.state.sleepingSince
        if ((sleeping == null || sleeping == since) && !container.appVisible) {
            Notifier.post(applicationContext, Notifier.ID_WOKE, ReminderTexts.woke(saved.profile.petName))
        }
        return Result.success()
    }

    companion object {
        const val KEY_SLEEPING_SINCE = "sleepingSince"
    }
}
