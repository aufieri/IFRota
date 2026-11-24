package br.edu.ifsp.ifrota.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.edu.ifsp.ifrota.MainActivity
import br.edu.ifsp.ifrota.R
import br.edu.ifsp.ifrota.data.geofencing.GeofenceEvent
import br.edu.ifsp.ifrota.data.geofencing.GeofenceTransition

/** Publica cada evento de perímetro como notificação do sistema. */
class GeofenceNotifier(private val context: Context) {

    init {
        // Canais só existem a partir do Android 8; criar de novo um canal existente não faz nada.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Perímetros (geofencing)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Veículos entrando e saindo de perímetros monitorados" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun notify(event: GeofenceEvent) {
        if (!canNotify()) return

        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val title = when (event.transition) {
            GeofenceTransition.ENTER -> "Entrada em perímetro"
            GeofenceTransition.EXIT -> "Saída de perímetro"
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_geofence)
            .setContentTitle(title)
            .setContentText(event.message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        try {
            // Um id por veículo + perímetro: a notificação mais recente substitui a anterior.
            NotificationManagerCompat.from(context)
                .notify("${event.vehicleId}:${event.geofence.id}".hashCode(), notification)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio.
        }
    }

    private companion object {
        const val CHANNEL_ID = "geofence"
    }
}
