package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MusicViewModel
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.HotMagenta
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark

data class NavigationTabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                SonicFlowMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SonicFlowMainApp(
    viewModel: MusicViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var isNowPlayingExpanded by remember { mutableStateOf(false) }

    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val playbackQueue by viewModel.playbackQueue.collectAsState()
    val isShuffled by viewModel.isShuffled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val equalizerPreset by viewModel.equalizerPreset.collectAsState()
    val sleepTimerSeconds by viewModel.sleepTimerSecondsLeft.collectAsState()
    val visualizerAmplitudes by viewModel.visualizerAmplitudes.collectAsState()

    val allTracks by viewModel.allTracks.collectAsState()
    val favoriteTracks by viewModel.favoriteTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val scanStatusMessage by viewModel.scanStatusMessage.collectAsState()

    val navTabs = listOf(
        NavigationTabItem("Tracks", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "nav_tab_tracks"),
        NavigationTabItem("Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic, "nav_tab_playlists"),
        NavigationTabItem("Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "nav_tab_favorites"),
        NavigationTabItem("Tools", Icons.Filled.Tune, Icons.Outlined.Tune, "nav_tab_tools")
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(NeonCyan, ElectricPurple, HotMagenta)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "SonicFlow",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = SurfaceDark
                    )
                )
            },
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Persistent Mini Player above Navigation Bar
                    MiniPlayer(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        onPlayPause = { viewModel.playOrPause() },
                        onSkipNext = { viewModel.skipToNext() },
                        onClick = { isNowPlayingExpanded = true }
                    )

                    // Material 3 Navigation Bar
                    NavigationBar(
                        containerColor = SurfaceDark,
                        tonalElevation = 8.dp
                    ) {
                        navTabs.forEachIndexed { index, item ->
                            val isSelected = selectedTab == index
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NeonCyan,
                                    selectedTextColor = NeonCyan,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> LibraryScreen(
                        tracks = allTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        searchQuery = searchQuery,
                        sortOption = sortOption,
                        availablePlaylists = playlists,
                        statusMessage = scanStatusMessage,
                        onSearchQueryChange = { viewModel.setSearchQuery(it) },
                        onSortOptionChange = { viewModel.setSortOption(it) },
                        onTrackClick = { viewModel.playTrack(it, allTracks) },
                        onPlayAll = {
                            if (allTracks.isNotEmpty()) viewModel.playTrack(allTracks.first(), allTracks)
                        },
                        onShuffleAll = {
                            if (allTracks.isNotEmpty()) {
                                val shuffled = allTracks.shuffled()
                                viewModel.playTrack(shuffled.first(), shuffled)
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { playlistId, trackId ->
                            viewModel.addTrackToPlaylist(playlistId, trackId)
                        },
                        onDeleteTrack = { viewModel.deleteTrack(it) },
                        onScanPhone = { viewModel.scanPhoneAudio() },
                        onImportAudioUris = { viewModel.importAudioUris(it) },
                        onDismissStatusMessage = { viewModel.dismissStatusMessage() }
                    )

                    1 -> PlaylistsScreen(
                        viewModel = viewModel,
                        playlists = playlists,
                        selectedPlaylist = selectedPlaylist,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onCreatePlaylist = { viewModel.createPlaylist(it) },
                        onDeletePlaylist = { viewModel.deletePlaylist(it) },
                        onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )

                    2 -> FavoritesScreen(
                        favorites = favoriteTracks,
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        availablePlaylists = playlists,
                        onTrackClick = { viewModel.playTrack(it, favoriteTracks) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onAddToPlaylist = { playlistId, trackId ->
                            viewModel.addTrackToPlaylist(playlistId, trackId)
                        },
                        onDeleteTrack = { viewModel.deleteTrack(it) },
                        onPlayAll = {
                            if (favoriteTracks.isNotEmpty()) viewModel.playTrack(favoriteTracks.first(), favoriteTracks)
                        },
                        onShuffleAll = {
                            if (favoriteTracks.isNotEmpty()) {
                                val shuffled = favoriteTracks.shuffled()
                                viewModel.playTrack(shuffled.first(), shuffled)
                            }
                        }
                    )

                    3 -> ToolsScreen(
                        equalizerPreset = equalizerPreset,
                        sleepTimerSeconds = sleepTimerSeconds,
                        tracks = allTracks,
                        onSetEqualizerPreset = { viewModel.setEqualizerPreset(it) },
                        onSetSleepTimer = { viewModel.setSleepTimer(it) },
                        onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                        onScanPhone = { viewModel.scanPhoneAudio() }
                    )
                }
            }
        }

        // Fullscreen Now Playing Overlay
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentTrack != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            NowPlayingScreen(
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                playbackQueue = playbackQueue,
                isShuffled = isShuffled,
                repeatMode = repeatMode,
                playbackSpeed = playbackSpeed,
                equalizerPreset = equalizerPreset,
                sleepTimerSeconds = sleepTimerSeconds,
                visualizerAmplitudes = visualizerAmplitudes,
                onPlayPause = { viewModel.playOrPause() },
                onSkipNext = { viewModel.skipToNext() },
                onSkipPrevious = { viewModel.skipToPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekRelative = { viewModel.seekRelative(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeatMode = { viewModel.cycleRepeatMode() },
                onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                onSetEqualizerPreset = { viewModel.setEqualizerPreset(it) },
                onSetSleepTimer = { viewModel.setSleepTimer(it) },
                onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTrackSelectFromQueue = { viewModel.playTrack(it) },
                onDismiss = { isNowPlayingExpanded = false }
            )
        }
    }
}
