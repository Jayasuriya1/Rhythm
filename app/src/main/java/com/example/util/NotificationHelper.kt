package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.entity.ReminderEntity
import com.example.receiver.ReminderActionReceiver

object NotificationHelper {

    const val CHANNEL_ID_MEDICATIONS = "channel_medications"
    const val CHANNEL_ID_ROUTINE = "channel_routine"
    const val CHANNEL_ID_HYDRATION = "channel_hydration"
    const val CHANNEL_ID_STUDY = "channel_study"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val medChannel = NotificationChannel(
                CHANNEL_ID_MEDICATIONS,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority reminders for scheduled medications and dosages"
                enableVibration(true)
                setShowBadge(true)
            }

            val routineChannel = NotificationChannel(
                CHANNEL_ID_ROUTINE,
                "Routine & Sleep Anchors",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gentle reminders for wake-up, meals, dinner, and bedtime rhythm anchors"
                enableVibration(true)
                setShowBadge(true)
            }

            val hydrationChannel = NotificationChannel(
                CHANNEL_ID_HYDRATION,
                "Hydration Checks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mindful hydration check-ins throughout the day"
                enableVibration(false)
                setShowBadge(false)
            }

            val studyChannel = NotificationChannel(
                CHANNEL_ID_STUDY,
                "Study & Focus Pauses",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for focus sessions and study breaks"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(medChannel, routineChannel, hydrationChannel, studyChannel)
            )
        }
    }

    fun getChannelForCategory(category: String): String {
        return when (category.uppercase()) {
            "MEDICATION" -> CHANNEL_ID_MEDICATIONS
            "HYDRATION" -> CHANNEL_ID_HYDRATION
            "STUDY_BREAK" -> CHANNEL_ID_STUDY
            else -> CHANNEL_ID_ROUTINE
        }
    }

    fun showReminderNotification(
        context: Context,
        reminder: ReminderEntity,
        isQuietHoursActive: Boolean = false
    ) {
        createNotificationChannels(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = getChannelForCategory(reminder.category)

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationId = reminder.id.toInt().takeIf { it != 0 } ?: 1001

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_rhythm)
            .setContentTitle(reminder.title)
            .setContentText(reminder.customMessage ?: "Scheduled for ${reminder.formattedTime()}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.customMessage ?: "Scheduled for ${reminder.formattedTime()}"))
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)

        // Priority and sound/vibration considerations
        if (isQuietHoursActive && !reminder.isUrgent) {
            // Mute sound and vibration during quiet hours
            builder.setSilent(true)
        } else {
            builder.setDefaults(
                (if (reminder.soundEnabled) NotificationCompat.DEFAULT_SOUND else 0) or
                (if (reminder.vibrateEnabled) NotificationCompat.DEFAULT_VIBRATE else 0)
            )
        }

        // Add Quick Action for Medication
        if (reminder.category.equals("MEDICATION", ignoreCase = true)) {
            val medActionIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_TAKE_MEDICATION
                putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminder.id)
                putExtra(ReminderActionReceiver.EXTRA_REFERENCE_ID, reminder.referenceId)
                putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val medPendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt() + 100000,
                medActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_stat_rhythm,
                "Mark as Taken",
                medPendingIntent
            )
        }

        // Add Quick Action for Hydration
        if (reminder.category.equals("HYDRATION", ignoreCase = true)) {
            val waterActionIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_LOG_WATER
                putExtra(ReminderActionReceiver.EXTRA_VOLUME_ML, 250)
                putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
            }
            val waterPendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.toInt() + 200000,
                waterActionIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_stat_rhythm,
                "+250ml Water",
                waterPendingIntent
            )
        }

        notificationManager.notify(notificationId, builder.build())
    }

    fun showTestNotification(context: Context) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_ROUTINE)
            .setSmallIcon(R.drawable.ic_stat_rhythm)
            .setContentTitle("Rhythm & Focus Test Reminder")
            .setContentText("Notifications and alarm channels are operating properly!")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(9999, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
    }
}
