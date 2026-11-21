package com.hfuu.schedule.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.hfuu.schedule.MainActivity
import com.hfuu.schedule.data.model.AgendaEventEntity

object AgendaReminderScheduler {

    fun scheduleOrCancel(context: Context, event: AgendaEventEntity) {
        AgendaReminderReceiver.ensureChannel(context)
        val triggerAt = event.reminderTriggerMillis()
        if (triggerAt == null) {
            cancel(context, event.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val subtitleParts = buildList {
            add(event.formatTimeRange())
            if (event.location.isNotBlank()) add("@${event.location}")
            if (event.notes.isNotBlank()) add(event.notes)
        }
        val subtitle = subtitleParts.joinToString(" · ")

        val pendingIntent = buildPendingIntent(
            context = context,
            eventId = event.id,
            title = "【${event.tagEnum().label}】${event.title}",
            subtitle = subtitle
        )

        runCatching {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (canExact) {
                val showIntent = PendingIntent.getActivity(
                    context,
                    (event.id.toInt() and 0x7FFFFFFF).coerceAtLeast(1000) + 50000,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, showIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancel(context: Context, eventId: Long) {
        if (eventId <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = buildPendingIntent(context, eventId, "", "")
        runCatching {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildPendingIntent(
        context: Context,
        eventId: Long,
        title: String,
        subtitle: String
    ): PendingIntent {
        val intent = Intent(context, AgendaReminderReceiver::class.java).apply {
            putExtra(AgendaReminderReceiver.EXTRA_EVENT_ID, eventId)
            putExtra(AgendaReminderReceiver.EXTRA_TITLE, title)
            putExtra(AgendaReminderReceiver.EXTRA_SUBTITLE, subtitle)
        }
        return PendingIntent.getBroadcast(
            context,
            (eventId.toInt() and 0x7FFFFFFF).coerceAtLeast(1000),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
