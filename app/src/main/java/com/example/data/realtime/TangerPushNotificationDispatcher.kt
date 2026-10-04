package com.example.data.realtime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

object TangerPushNotificationDispatcher {
    const val CHANNEL_ID_LUCKY_GIFTS = "tangeram_lucky_gifts_channel"
    const val CHANNEL_ID_ROOM_EVENTS = "tangeram_room_events_channel"
    private val notificationIdSeq = AtomicInteger(2000)

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val luckyChannel = NotificationChannel(
                CHANNEL_ID_LUCKY_GIFTS,
                "TANGERAM Global Lucky Gifts & Wallet Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications for global gifts, Lucky Gift rewards, cashback, and jackpots"
            }
            val roomChannel = NotificationChannel(
                CHANNEL_ID_ROOM_EVENTS,
                "TANGERAM Live Rooms & Official Broadcasts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for live voice room top banners, PK battles, and official events"
            }
            manager.createNotificationChannel(luckyChannel)
            manager.createNotificationChannel(roomChannel)
        }
    }

    fun dispatchGiftOrLuckyNotification(context: Context, event: RealtimeEvent.GiftBroadcast) {
        try {
            ensureNotificationChannels(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val title = if (event.luckyRewardGranted > 0) {
                "🍀 LUCKY GIFT HIT! ${event.giftEmoji} ${event.giftName} x${event.quantity}"
            } else {
                "🎁 Global Gift: ${event.giftEmoji} ${event.giftName} x${event.quantity}"
            }
            val body = buildString {
                append("${event.senderName} (#${event.senderId}) ➔ ${event.receiverName} (#${event.receiverId}) • 🪙 ${event.totalCoins} Coins")
                if (event.luckyRewardGranted > 0) {
                    append(" • Lucky Reward: +${event.luckyRewardGranted}🪙")
                }
                if (event.cashbackGranted > 0) {
                    append(" • Cashback: +${event.cashbackGranted}🪙")
                }
            }

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_LUCKY_GIFTS)
                .setSmallIcon(android.R.drawable.btn_star_big_on)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdSeq.incrementAndGet(), notification)
        } catch (_: Throwable) {
        }
    }
}
