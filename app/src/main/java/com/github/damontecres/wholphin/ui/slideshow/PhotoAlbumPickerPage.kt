package com.github.damontecres.wholphin.ui.slideshow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.wholphin.R
import com.github.damontecres.wholphin.data.ServerRepository
import com.github.damontecres.wholphin.data.model.BaseItem
import com.github.damontecres.wholphin.data.model.CollectionFolderFilter
import com.github.damontecres.wholphin.preferences.UserPreferences
import com.github.damontecres.wholphin.services.NavigationManager
import com.github.damontecres.wholphin.ui.AspectRatios
import com.github.damontecres.wholphin.ui.cards.GridCard
import com.github.damontecres.wholphin.ui.components.ErrorMessage
import com.github.damontecres.wholphin.ui.components.LoadingPage
import com.github.damontecres.wholphin.ui.data.SortAndDirection
import com.github.damontecres.wholphin.ui.launchIO
import com.github.damontecres.wholphin.ui.nav.Destination
import com.github.damontecres.wholphin.ui.tryRequestFocus
import com.github.damontecres.wholphin.util.LoadingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.CollectionType
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import org.jellyfin.sdk.model.api.request.GetItemsRequest
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PhotoAlbumPickerViewModel
    @Inject
    constructor(
        private val api: ApiClient,
        private val serverRepository: ServerRepository,
        val navigationManager: NavigationManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow<LoadingState>(LoadingState.Pending)
        val state: StateFlow<LoadingState> = _state

        private val _albums = MutableStateFlow<List<BaseItem>>(emptyList())
        val albums: StateFlow<List<BaseItem>> = _albums

        init {
            loadPhotoAlbums()
        }

        fun loadPhotoAlbums() {
            viewModelScope.launchIO {
                _state.update { LoadingState.Loading }
                try {
                    val userId = serverRepository.currentUser?.id

                    val userViews =
                        try {
                            if (userId != null) {
                                api.userViewsApi
                                    .getUserViews(userId = userId)
                                    .content.items
                                    .filter {
                                        it.collectionType == CollectionType.PHOTOS ||
                                            it.collectionType == CollectionType.HOMEVIDEOS ||
                                            it.type == BaseItemKind.PHOTO_ALBUM
                                    }
                            } else {
                                emptyList()
                            }
                        } catch (e: Exception) {
                            Timber.w(e, "Error fetching user views for photo album picker")
                            emptyList()
                        }

                    val photoAlbums =
                        try {
                            api.itemsApi
                                .getItems(
                                    GetItemsRequest(
                                        userId = userId,
                                        includeItemTypes = listOf(BaseItemKind.PHOTO_ALBUM),
                                        recursive = true,
                                    ),
                                ).content.items
                        } catch (e: Exception) {
                            Timber.w(e, "Error fetching photo albums")
                            emptyList()
                        }

                    val combinedDtos = (userViews + photoAlbums).distinctBy { it.id }
                    val items = combinedDtos.map { BaseItem.from(it, api, false) }

                    _albums.update { items }
                    _state.update { LoadingState.Success }
                } catch (ex: Exception) {
                    Timber.e(ex, "Error loading photo albums for picker")
                    _state.update { LoadingState.Error(ex) }
                }
            }
        }

        fun onAlbumSelected(albumId: UUID) {
            val destination =
                Destination.Slideshow(
                    parentId = albumId,
                    index = 0,
                    filter = CollectionFolderFilter(),
                    sortAndDirection = SortAndDirection(ItemSortBy.RANDOM, SortOrder.ASCENDING),
                    recursive = true,
                    startSlideshow = true,
                )
            navigationManager.navigateTo(destination)
        }
    }

@Composable
fun PhotoAlbumPickerPage(
    preferences: UserPreferences,
    modifier: Modifier = Modifier,
    viewModel: PhotoAlbumPickerViewModel = hiltViewModel(),
) {
    val loadingState by viewModel.state.collectAsState()
    val albums by viewModel.albums.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.photo_album_picker),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        when (val st = loadingState) {
            is LoadingState.Error -> {
                ErrorMessage(st, Modifier.fillMaxSize())
            }

            LoadingState.Loading,
            LoadingState.Pending,
            -> {
                LoadingPage(Modifier.fillMaxSize())
            }

            LoadingState.Success -> {
                if (albums.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Text(
                            text = stringResource(R.string.no_photo_libraries),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    val focusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) {
                        focusRequester.tryRequestFocus()
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 180.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(albums, key = { _, item -> item.id }) { index, item ->
                            GridCard(
                                item = item,
                                onClick = { viewModel.onAlbumSelected(item.id) },
                                onLongClick = {},
                                showTitle = true,
                                imageAspectRatio = AspectRatios.SQUARE,
                                modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}
