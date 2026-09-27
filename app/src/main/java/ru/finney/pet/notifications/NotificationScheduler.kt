package ru.finney.pet.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.finney.pet.domain.game.Game
import ru.finney.pet.domain.game.Session
import ru.finney.pet.domain.model.ReminderRule
import java.time.Duration

/**
 * Расписание уведомлений. Следит за сохранённой игрой, а не за кнопками: уснул — «проснулся»
 * ставится на [Game.sleepEndsAt], разбудили раньше — снимается, включили демо — сон короче,
 * и задача переставляется. Так срабатывает при любом пути — с главного, из отладки, после сброса.
 */
class NotificationScheduler(
    context: Context,
    private val session: Session,
    private val game: Game,
    private val rule: ReminderRule,
    private val prefs: NotificationPrefs,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val work = WorkManager.getInstance(context.applicationContext)

    /** Сон открытого профиля и демо-режим: всё, от чего зависит расписание. */
    private data class Watch(val sleepingSince: Long?, val endsAt: Long?, val isDemo: Boolean)

    fun start() {
        scope.launch {
            var previous: Watch? = null
            session.activeGame
                .map { saved -> saved?.state?.let { Watch(it.sleepingSince, game.sleepEndsAt(it), it.isDemo) } }
                .distinctUntilChanged()
                .collect { watch ->
                    scheduleWake(previous, watch)
                    // Включили или выключили демо — напоминание по новому интервалу.
                    if (watch != null && previous != null && watch.isDemo != previous?.isDemo) {
                        scheduleReminder(ReminderPolicy.interval(rule, watch.isDemo))
                    }
                    previous = watch
                }
        }
    }

    /** Приложение открыли: запомнить время и начать отсчёт до следующего напоминания заново. */
    fun appOpened() {
        scope.launch {
            prefs.setLastOpened(clock())
            val saved = session.activeGame.first() ?: return@launch
            scheduleReminder(ReminderPolicy.interval(rule, saved.state.isDemo))
        }
    }

    /**
     * Следующее напоминание через [delay]. [fromWorker] — ставит сам [ReminderWorker]:
     * тогда новое встаёт в очередь за ним, а не отменяет его на полуслове.
     */
    fun scheduleReminder(delay: Duration, fromWorker: Boolean = false) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay.coerceAtLeast(Duration.ZERO))
            .build()
        val policy = if (fromWorker) ExistingWorkPolicy.APPEND_OR_REPLACE else ExistingWorkPolicy.REPLACE
        work.enqueueUniqueWork(REMINDER, policy, request)
    }

    private fun scheduleWake(previous: Watch?, watch: Watch?) {
        val since = watch?.sleepingSince
        val endsAt = watch?.endsAt
        if (since == null || endsAt == null) {
            // Проснулся. Разбудили раньше срока — «проснулся» не нужен. Проснулся сам —
            // задачу не трогаем: главный экран будит питомца в срок и в фоне, а сказать
            // об этом ребёнку всё равно надо.
            val wasEnding = previous?.endsAt
            if (wasEnding != null && clock() < wasEnding - WAKE_SLACK_MS) work.cancelUniqueWork(WAKE)
            return
        }
        val request = OneTimeWorkRequestBuilder<WakeWorker>()
            .setInitialDelay(Duration.ofMillis((endsAt - clock()).coerceAtLeast(0)))
            .setInputData(workDataOf(WakeWorker.KEY_SLEEPING_SINCE to since))
            .build()
        work.enqueueUniqueWork(WAKE, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val REMINDER = "reminder"
        const val WAKE = "wake"

        /** Проснулся за столько до срока или позже — это «сам», а не «разбудили». */
        const val WAKE_SLACK_MS = 2_000L
    }
}
