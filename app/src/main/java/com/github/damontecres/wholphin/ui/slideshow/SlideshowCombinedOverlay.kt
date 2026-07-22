package com.github.damontecres.wholphin.ui.slideshow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.data.model.AudioItem
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.ui.AppColors
import com.github.damontecres.wholphin.ui.detail.music.NowPlayingOverlay
import com.github.damontecres.wholphin.ui.detail.music.NowPlayingState
import com.github.damontecres.wholphin.ui.main.settings.MoveDirection
import com.github.damontecres.wholphin.ui.playback.ControllerViewState

@Composable
fun SlideshowCombinedOverlay(
    modifier: Modifier = Modifier,
    // Image Overlay params
    onDismiss: () -> Unit,
    player: Player,
    slideshowControls: SlideshowControls,
    slideshowEnabled: Boolean,
    position: Int,
    count: Int,
    image: ImageState,
    onClickItem: (BaseItem) -> Unit,
    onLongClickItem: (BaseItem) -> Unit,
    onZoom: (Float) -> Unit,
    onRotate: (Int) -> Unit,
    onReset: () -> Unit,
    onShowFilterDialogClick: () -> Unit,
    
    // Music Overlay params
    nowPlayingState: NowPlayingState,
    musicPlayer: Player,
    currentAudio: AudioItem?,
    queue: List<AudioItem>,
    controllerViewState: ControllerViewState,
    onClickSong: (Int, AudioItem) -> Unit,
    onLongClickSong: (Int, AudioItem) -> Unit,
    onClickMore: () -> Unit,
    onMoveQueue: (Int, MoveDirection) -> Unit,
    onClickMoreItem: (Int, AudioItem) -> Unit,
    onClickStop: () -> Unit,
    onClickSlideshow: () -> Unit,
    lyricsFocusRequester: FocusRequester,
    onClickPlayRecommended: () -> Unit,

    // Settings
    slideShowDelayMs: Long,
    onSlideShowDelayMsChange: (Long) -> Unit,
    crossFadeDuration: Long,
    onCrossFadeDurationChange: (Long) -> Unit,
    zoomPanEnabled: Boolean,
    onZoomPanEnabledChange: (Boolean) -> Unit,
    isShuffleEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onChangeAlbumClick: () -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var localShuffleEnabled by remember(isShuffleEnabled) { mutableStateOf(isShuffleEnabled) }
    
    Column(
        modifier = modifier.background(AppColors.TransparentBlack50)
    ) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.padding(16.dp)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onFocus = { selectedTabIndex = 0 },
                onClick = { selectedTabIndex = 0 },
            ) {
                Text(
                    text = "Slideshow",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Tab(
                selected = selectedTabIndex == 1,
                onFocus = { selectedTabIndex = 1 },
                onClick = { selectedTabIndex = 1 },
            ) {
                Text(
                    text = "Music",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
        
        if (selectedTabIndex == 0) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ImageOverlay(
                    onDismiss = onDismiss,
                    player = player,
                    slideshowControls = slideshowControls,
                    slideshowEnabled = slideshowEnabled,
                    position = position,
                    count = count,
                    image = image,
                    onClickItem = onClickItem,
                    onLongClickItem = onLongClickItem,
                    onZoom = onZoom,
                    onRotate = onRotate,
                    onReset = onReset,
                    onShowFilterDialogClick = onShowFilterDialogClick,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Speed: ${if (slideShowDelayMs < 7000L) "Fast" else if (slideShowDelayMs > 12000L) "Slow" else "Normal"}", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = {
                        val next = if (slideShowDelayMs <= 5000L) 10000L else if (slideShowDelayMs <= 10000L) 15000L else 5000L
                        onSlideShowDelayMsChange(next)
                    }) { Text("Change") }
                    
                    Spacer(Modifier.width(32.dp))
                    Text("Transition: ${if (crossFadeDuration > 0) "Crossfade" else if (zoomPanEnabled) "Zoom" else "None"}", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = {
                        if (crossFadeDuration > 0) {
                            onCrossFadeDurationChange(0L)
                            onZoomPanEnabledChange(true)
                        } else if (zoomPanEnabled) {
                            onZoomPanEnabledChange(false)
                        } else {
                            onCrossFadeDurationChange(750L)
                            onZoomPanEnabledChange(false)
                        }
                    }) { Text("Change") }
                    
                    Spacer(Modifier.width(32.dp))
                    Text("Shuffle: ${if (localShuffleEnabled) "On" else "Off"}", color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = {
                        onShuffleToggle()
                        localShuffleEnabled = !localShuffleEnabled
                    }) { Text("Toggle") }
                    
                    Spacer(Modifier.width(32.dp))
                    Button(onClick = onChangeAlbumClick) { Text("Change Album") }
                }
            }
        } else {
            NowPlayingOverlay(
                state = nowPlayingState,
                player = musicPlayer,
                current = currentAudio,
                queue = queue,
                controllerViewState = controllerViewState,
                onClickSong = onClickSong,
                onLongClickSong = onLongClickSong,
                onClickMore = onClickMore,
                onMoveQueue = onMoveQueue,
                onClickMoreItem = onClickMoreItem,
                onClickStop = onClickStop,
                onClickSlideshow = onClickSlideshow,
                lyricsFocusRequester = lyricsFocusRequester,
                onClickPlayRecommended = onClickPlayRecommended,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
