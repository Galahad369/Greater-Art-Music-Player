package com.local.listentomusic.playback

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.local.listentomusic.MainActivity
import com.local.listentomusic.R

class PlaybackWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { connect(context, null) }
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action in listOf("widget.toggle", "widget.next", "widget.previous")) connect(context, intent.action)
    }
    private fun connect(context: Context, action: String?) {
        val pending = goAsync()
        val future = MediaController.Builder(context, SessionToken(context, ComponentName(context, PlaybackService::class.java)))
            .setConnectionHints(orderedTransportHints())
            .buildAsync()
        future.addListener({
            try {
                val player = future.get()
                when (action) {
                    "widget.toggle" -> togglePlayback(player)
                    "widget.next" -> player.seekToNextMediaItem()
                    "widget.previous" -> player.seekToPreviousMediaItem()
                }
                update(context, player.mediaMetadata.title?.toString() ?: "Greater Art", playbackRequested(player), null)
                player.release()
            } catch (_: Exception) { update(context, "Open Greater Art", false, null) }
            finally { pending.finish() }
        }, ContextCompat.getMainExecutor(context))
    }

    companion object {
        fun update(context: Context, title: String, playing: Boolean, art: Bitmap?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PlaybackWidget::class.java))
            if (ids.isEmpty()) return
            val views = RemoteViews(context.packageName, R.layout.playback_widget)
            views.setTextViewText(R.id.widget_title, title)
            views.setImageViewResource(R.id.widget_toggle, if (playing) R.drawable.ic_pause else R.drawable.ic_play)
            if (art != null) views.setImageViewBitmap(R.id.widget_art, art)
            else views.setImageViewResource(R.id.widget_art, R.drawable.ic_launcher_foreground)
            views.setOnClickPendingIntent(R.id.widget_title, PendingIntent.getActivity(context, 10,
                Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            listOf(R.id.widget_previous to "widget.previous", R.id.widget_toggle to "widget.toggle", R.id.widget_next to "widget.next").forEachIndexed { index, (id, action) ->
                views.setOnClickPendingIntent(id, PendingIntent.getBroadcast(context, index,
                    Intent(context, PlaybackWidget::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            }
            manager.updateAppWidget(ids, views)
        }
    }
}
