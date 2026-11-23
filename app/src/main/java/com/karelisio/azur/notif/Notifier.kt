package com.karelisio.azur.notif

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.karelisio.azur.MainActivity
import com.karelisio.azur.R

object Notifier {
    const val CHANNEL_RAIN = "rain"
    const val CHANNEL_MORNING = "morning"
    const val CHANNEL_VIGILANCE = "vigilance"
    private const val ID_RAIN = 1
    private const val ID_MORNING = 2
    private const val ID_VIGILANCE = 3

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_RAIN, "Pluie dans l'heure", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Alerte quand la pluie arrive sur votre position" },
                NotificationChannel(CHANNEL_MORNING, "Résumé du matin", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "La météo de la journée à l'heure choisie" },
                NotificationChannel(CHANNEL_VIGILANCE, "Vigilances Météo-France", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Vigilance jaune, orange ou rouge de votre département" },
            ),
        )
    }

    private fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (android.os.Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun post(context: Context, id: Int, channel: String, title: String, text: String, largeIcon: Int?, color: Int? = null) {
        if (!canNotify(context)) return
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_azur)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
        largeIcon?.let { res ->
            runCatching { vectorBitmap(context, res) }.getOrNull()?.let { builder.setLargeIcon(it) }
        }
        color?.let { builder.setColor(it).setColorized(false) }
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // autorisation retirée entre-temps
        }
    }

    fun rain(context: Context, title: String, text: String, icon: Int) = post(context, ID_RAIN, CHANNEL_RAIN, title, text, icon)
    fun morning(context: Context, title: String, text: String, icon: Int) = post(context, ID_MORNING, CHANNEL_MORNING, title, text, icon)
    fun vigilance(context: Context, title: String, text: String, color: Int) =
        post(context, ID_VIGILANCE, CHANNEL_VIGILANCE, title, text, null, color)

    private fun vectorBitmap(context: Context, res: Int): android.graphics.Bitmap? {
        val d = ContextCompat.getDrawable(context, res) ?: return BitmapFactory.decodeResource(context.resources, res)
        val size = (64 * context.resources.displayMetrics.density).toInt()
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        d.setBounds(0, 0, size, size)
        d.draw(canvas)
        return bmp
    }
}
