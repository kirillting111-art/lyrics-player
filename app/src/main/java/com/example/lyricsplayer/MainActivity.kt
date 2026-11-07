package com.example.lyricsplayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class TrackInfo(val title: String, val artist: String, val cover: Bitmap?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

fun hasListenerAccess(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver, "enabled_notification_listeners"
    ) ?: return false
    return enabled.contains(context.packageName)
}

fun readTrack(context: Context): TrackInfo? {
    return try {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val comp = ComponentName(context, MediaListener::class.java)
        val controller = msm.getActiveSessions(comp).firstOrNull() ?: return null
        val m = controller.metadata ?: return null
        TrackInfo(
            title = m.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "",
            artist = m.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "",
            cover = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: m.getBitmap(MediaMetadata.METADATA_KEY_ART)
        )
    } catch (e: SecurityException) {
        null
    }
}

@Composable
fun App() {
    val context = LocalContext.current
    var access by remember { mutableStateOf(hasListenerAccess(context)) }
    var track by remember { mutableStateOf<TrackInfo?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            access = hasListenerAccess(context)
            track = if (access) readTrack(context) else null
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0B))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!access) {
            Text("Нужен доступ к уведомлениям, чтобы видеть, что играет", color = Color.White)
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) { Text("Выдать доступ") }
        } else if (track == null) {
            Text("Включи песню в любом плеере", color = Color.Gray)
        } else {
            val t = track!!
            if (t.cover != null) {
                Image(
                    bitmap = t.cover.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(24.dp))
            }
            Text(t.title, color = Color.White, fontSize = 26.sp)
            Spacer(Modifier.height(8.dp))
            Text(t.artist, color = Color.Gray, fontSize = 18.sp)
        }
    }
}
