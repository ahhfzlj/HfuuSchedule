package com.hfuu.schedule.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hfuu.schedule.MainActivity
import com.hfuu.schedule.R

class AgendaReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(EXTRA_EVENT_ID, 0L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "日程提醒" }
        val subtitle = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty().ifBlank { "你有一项日程即将开始" }
        deliverNotification(context, eventId, title, subtitle)
    }

    companion object {
        private const val LEGACY_CHANNEL_ID = "hfuu_agenda_reminders"
        const val CHANNEL_ID = "hfuu_agenda_reminders_v2"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_SUBTITLE = "extra_subtitle"

        private val VIBRATION_PATTERN = longArrayOf(0L, 350L, 180L, 350L)

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
                // 移除旧版未显式绑定提示音的渠道，确保新版带响铃/横幅/震动的渠道生效
                if (nm.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
                    runCatching { nm.deleteNotificationChannel(LEGACY_CHANNEL_ID) }
                }
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "日程响铃提醒",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "日程开始前与倒计时提醒（含系统提示音、横幅弹窗与震动）"
                        setSound(soundUri, audioAttrs)
                        enableVibration(true)
                        vibrationPattern = VIBRATION_PATTERN
                        enableLights(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setBypassDnd(true)
                    }
                    nm.createNotificationChannel(channel)
                }
            }
        }

        fun deliverNotification(
            context: Context,
            eventId: Long,
            title: String,
            subtitle: String
        ) {
            ensureChannel(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                eventId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setSound(soundUri)
                .setVibrate(VIBRATION_PATTERN)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .build()

            val notifyId = if (eventId == -999L) {
                999999
            } else {
                (eventId.toInt() and 0x7FFFFFFF).coerceAtLeast(1000)
            }

            runCatching {
                NotificationManagerCompat.from(context).notify(notifyId, notification)
            }

            // 兼容部分国产定制系统默认将侧载应用通知渠道降级为静默/无声的情况：
            // 仅当手机未静音且系统渠道被降级无声时，主动补播一次系统默认提示音（避免重复叠音）
            playFallbackToneIfChannelMuted(context, soundUri)
        }

        private fun playFallbackToneIfChannelMuted(context: Context, soundUri: android.net.Uri?) {
            if (soundUri == null) return
            runCatching {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) return

                val channelMutedByRom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    val ch = nm?.getNotificationChannel(CHANNEL_ID)
                    ch == null || ch.sound == null || ch.importance < NotificationManager.IMPORTANCE_DEFAULT
                } else {
                    false
                }

                if (channelMutedByRom) {
                    val ringtone = RingtoneManager.getRingtone(context, soundUri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone?.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    ringtone?.play()
                }
            }
        }

        fun sendTestNotification(context: Context) {
            deliverNotification(
                context = context,
                eventId = -999L,
                title = "【日程提醒测试】合大课表",
                subtitle = "如果你听到了系统提示音，说明日程响铃提醒已正常工作"
            )
        }

        fun openChannelSettings(context: Context) {
            ensureChannel(context)
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            runCatching { context.startActivity(intent) }
        }
    }
}
