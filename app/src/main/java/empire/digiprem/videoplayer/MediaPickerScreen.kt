package empire.digiprem.videoplayer

import android.media.metrics.PlaybackStateEvent.STATE_ENDED
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.RetainedEffect
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import kotlinx.coroutines.delay


@Composable
fun MediaPickerScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = retain {
        ExoPlayer
            .Builder(context.applicationContext)
            .build()

    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->

        uri?.let {
            player.setMediaItem(
                MediaItem.fromUri(uri)
            )
            player.prepare()
            player.play()
        }


    }

    var isPlaying by retain {
        mutableStateOf(false)
    }

    var currentPosition by retain {
        mutableLongStateOf(0L)
    }
    var duration by retain {
        mutableLongStateOf(0L)
    }

    var isSeeking by retain {
        mutableStateOf(false)
    }

    var isBuffering by retain {
        mutableStateOf(false)
    }
    var isPlayerUiVisible by retain {
        mutableStateOf(true)
    }

    RetainedEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                super.onIsPlayingChanged(playing)
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    duration = player.duration.coerceAtLeast(0)
                }
            }
        }

        player.addListener(listener)
        onRetire {
            player.removeListener(listener)
            player.release()
        }
    }


    LaunchedEffect(isPlayerUiVisible, isSeeking, isPlaying) {
        delay(5000L)
        if (!isSeeking) {
            isPlayerUiVisible = false

        }
    }

    LaunchedEffect(player,isPlaying,isSeeking) {
        while (isPlaying) {
            if (!isSeeking) {
                currentPosition = player.currentPosition.coerceAtLeast(0)
                delay(16L)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, alignment = Alignment.CenterVertically)
    ) {
        Button(
            onClick = {
                videoPickerLauncher.launch(
                    input = PickVisualMediaRequest(
                        mediaType = ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                )
            }
        ) {
            Text("Pick video")
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp)

        ) {
            ContentFrame(
                player = player,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) {
                        isPlayerUiVisible = !isPlayerUiVisible
                    }
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedVisibility(
                    visible = isPlayerUiVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    PlayerUI(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        isSeeking = isSeeking,
                        currentPosition = currentPosition,
                        duration = duration,
                        onSeekBarPositionChangeFinished = {
                            player.seekTo(it)
                            isSeeking = false
                        },
                        onPlayPauseClick = {
                            when {
                                !isPlaying && player.playbackState == STATE_ENDED -> {
                                    player.seekTo(0)
                                    player.play()
                                }
                                !isPlaying -> player.play()
                                isPlaying -> player.pause()
                            }
                        },
                        onSeekBarPositonChange = {
                            isSeeking = true
                            currentPosition = it

                        }
                    )
                }
            }
        }

    }

}