package com.idlix.mobile.ui.screens.detail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idlix.data.local.PlayerAppearanceStore
import com.idlix.data.model.PlayerAppearancePresets
import com.idlix.data.model.SubdlItem
import com.idlix.data.model.SubdlQuery
import com.idlix.data.model.SubdlSearchResult
import com.idlix.data.model.SubdlSubtitle
import com.idlix.data.model.SubsourceMovie
import com.idlix.data.model.SubsourceSearchResult
import com.idlix.data.model.SubsourceSubtitle
import com.idlix.data.model.SubtitleSize
import com.idlix.data.model.SubtitleTrack
import com.idlix.mobile.R
import com.idlix.mobile.ui.screens.settings.ColorPickerSheet
import com.idlix.mobile.ui.screens.settings.MobileColorSwatch
import com.idlix.mobile.ui.theme.BackgroundDark
import com.idlix.mobile.ui.theme.BackgroundElevated
import com.idlix.mobile.ui.theme.IdlixRed
import com.idlix.mobile.ui.theme.SurfaceDark
import com.idlix.mobile.ui.theme.SurfaceVariantDark
import com.idlix.mobile.ui.theme.TextDim
import com.idlix.mobile.ui.theme.TextMuted
import com.idlix.mobile.ui.theme.TextSecondary
import com.idlix.mobile.ui.theme.TextWhite

private enum class ModalActivePicker {
    NONE, TEXT_COLOR, BOX_COLOR
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubtitleModalBottomSheet(
    isVisible: Boolean,
    availableSubtitles: List<SubtitleTrack>,
    selectedSubtitle: SubtitleTrack?,
    isSubtitleEnabled: Boolean,
    subdlSubtitles: List<SubdlSubtitle>,
    isSearchingSubdl: Boolean,
    subdlCandidates: List<SubdlItem> = emptyList(),
    subdlError: SubdlSearchResult.Error? = null,
    subdlActiveQuery: SubdlQuery? = null,
    subsourceSubtitles: List<SubsourceSubtitle> = emptyList(),
    isSearchingSubsource: Boolean = false,
    subsourceCandidates: List<SubsourceMovie> = emptyList(),
    subsourceError: SubsourceSearchResult.Error? = null,
    isManualSearchOpen: Boolean = false,
    manualTitle: String = "",
    manualYear: String = "",
    manualSeason: String = "",
    manualEpisode: String = "",
    isUsingSessionOverride: Boolean = false,
    isSeries: Boolean = false,
    onToggleSubtitle: () -> Unit,
    onSelectTrack: (SubtitleTrack?) -> Unit,
    onSelectSubdl: (SubdlSubtitle) -> Unit,
    onSearchSubdl: () -> Unit,
    onSelectSubsource: (SubsourceSubtitle) -> Unit = {},
    onSearchSubsource: () -> Unit = {},
    onSelectSubsourceCandidate: (SubsourceMovie) -> Unit = {},
    onOpenManualSearch: () -> Unit,
    onCloseManualSearch: () -> Unit,
    onManualTitleChange: (String) -> Unit,
    onManualYearChange: (String) -> Unit,
    onManualSeasonChange: (String) -> Unit,
    onManualEpisodeChange: (String) -> Unit,
    onSubmitManualSearch: () -> Unit,
    onSelectCandidate: (SubdlItem) -> Unit,
    onRestoreOriginalTitle: () -> Unit,
    onOpenPositionEditor: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var activePicker by remember { mutableStateOf(ModalActivePicker.NONE) }

    val appearance by PlayerAppearanceStore.appearance.collectAsState()
    val recentColors by PlayerAppearanceStore.recentCustomColors.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundElevated,
        contentColor = TextWhite,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(vertical = 10.dp),
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(2.dp)
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Tab Header: Tracks vs Appearance
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = BackgroundElevated,
                contentColor = IdlixRed,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = IdlixRed
                    )
                },
                divider = {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ClosedCaption, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.audio_and_subtitles), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = IdlixRed,
                    unselectedContentColor = TextMuted
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.player_appearance_header), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    selectedContentColor = IdlixRed,
                    unselectedContentColor = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (selectedTab == 0) {
                if (isManualSearchOpen) {
                    // Manual Title Search Form
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onCloseManualSearch) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextWhite)
                            }
                            Text(
                                text = stringResource(R.string.subdl_manual_search),
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Title field
                        OutlinedTextField(
                            value = manualTitle,
                            onValueChange = onManualTitleChange,
                            label = { Text(stringResource(R.string.subdl_title_label), color = TextDim) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceVariantDark,
                                unfocusedContainerColor = SurfaceVariantDark,
                                focusedBorderColor = IdlixRed,
                                unfocusedBorderColor = SurfaceVariantDark,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                onSubmitManualSearch()
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Year + Season + Episode
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = manualYear,
                                onValueChange = onManualYearChange,
                                label = { Text(stringResource(R.string.subdl_year_label), color = TextDim) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceVariantDark,
                                    unfocusedContainerColor = SurfaceVariantDark,
                                    focusedBorderColor = IdlixRed,
                                    unfocusedBorderColor = SurfaceVariantDark,
                                    focusedTextColor = TextWhite,
                                    unfocusedTextColor = TextWhite
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (isSeries) {
                                OutlinedTextField(
                                    value = manualSeason,
                                    onValueChange = onManualSeasonChange,
                                    label = { Text(stringResource(R.string.subdl_season_label), color = TextDim) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceVariantDark,
                                        unfocusedContainerColor = SurfaceVariantDark,
                                        focusedBorderColor = IdlixRed,
                                        unfocusedBorderColor = SurfaceVariantDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = manualEpisode,
                                    onValueChange = onManualEpisodeChange,
                                    label = { Text(stringResource(R.string.subdl_episode_label), color = TextDim) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = SurfaceVariantDark,
                                        unfocusedContainerColor = SurfaceVariantDark,
                                        focusedBorderColor = IdlixRed,
                                        unfocusedBorderColor = SurfaceVariantDark,
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                onSubmitManualSearch()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IdlixRed),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.subdl_search_action))
                        }

                        if (isUsingSessionOverride) {
                            TextButton(
                                onClick = onRestoreOriginalTitle,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextDim)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.subdl_restore_original), color = TextDim, fontSize = 12.sp)
                            }
                        }
                    }
                } else if (subdlCandidates.isNotEmpty()) {
                    // Candidate Picker View
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.subdl_pick_title),
                            color = TextWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subdlCandidates) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceVariantDark)
                                        .clickable { onSelectCandidate(item) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name.orEmpty(),
                                            color = TextWhite,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "${item.type ?: "Film"} • ${item.year ?: ""}",
                                            color = TextDim,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Icon(Icons.Default.Check, contentDescription = null, tint = TextMuted)
                                }
                            }
                        }
                    }
                } else {
                    // Standard Tracks View
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isSubtitleEnabled) stringResource(R.string.subtitle_on) else stringResource(R.string.subtitle_off),
                            fontSize = 13.sp,
                            color = if (isSubtitleEnabled) IdlixRed else TextMuted
                        )
                        Switch(
                            checked = isSubtitleEnabled,
                            onCheckedChange = { onToggleSubtitle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IdlixRed,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Off Option
                        item {
                            val isOffSelected = !isSubtitleEnabled || selectedSubtitle == null
                            SubtitleOptionItem(
                                title = stringResource(R.string.subtitles_off),
                                subtitle = null,
                                isSelected = isOffSelected,
                                onClick = { onSelectTrack(null) }
                            )
                        }

                        // Embedded Subtitles
                        if (availableSubtitles.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.default_subtitle),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }

                            items(availableSubtitles) { track ->
                                val isSelected = isSubtitleEnabled && selectedSubtitle?.path == track.path
                                SubtitleOptionItem(
                                    title = track.label.ifBlank {
                                        if (track.lang.equals("id", ignoreCase = true)) stringResource(R.string.indonesian) else track.lang.uppercase()
                                    },
                                    subtitle = if (track.lang.equals("id", ignoreCase = true)) stringResource(R.string.indonesian) else track.lang,
                                    isSelected = isSelected,
                                    onClick = { onSelectTrack(track) }
                                )
                            }
                        }

                        // SubDL Subtitles Section
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = stringResource(R.string.subdl_subtitles_header),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary
                                    )
                                    if (subdlActiveQuery != null) {
                                        Text(
                                            text = "Query: ${subdlActiveQuery.filmName}${if (subdlActiveQuery.year != null) " (${subdlActiveQuery.year})" else ""}",
                                            fontSize = 11.sp,
                                            color = TextDim,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Manual Search Icon Button
                                    IconButton(
                                        onClick = onOpenManualSearch,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.subdl_manual_search),
                                            tint = IdlixRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Refresh / Search SubDL button
                                    if (!isSearchingSubdl) {
                                        IconButton(
                                            onClick = onSearchSubdl,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                tint = IdlixRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isSearchingSubdl) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = IdlixRed,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.searching_subdl),
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        } else if (subdlError != null) {
                            item {
                                val errorMsg = when (subdlError.reason) {
                                    SubdlSearchResult.Error.Reason.INVALID_KEY -> stringResource(R.string.subdl_error_invalid_key)
                                    SubdlSearchResult.Error.Reason.QUOTA -> stringResource(R.string.subdl_error_quota)
                                    SubdlSearchResult.Error.Reason.NETWORK -> stringResource(R.string.subdl_error_network)
                                    else -> subdlError.message.orEmpty().ifBlank { "Error" }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x22E50914), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = IdlixRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = errorMsg, color = IdlixRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        } else if (subdlSubtitles.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.no_subdl_found),
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                    TextButton(onClick = onOpenManualSearch) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp), tint = IdlixRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.subdl_type_title_btn), color = IdlixRed, fontSize = 12.sp)
                                    }
                                }
                            }
                        } else {
                            items(subdlSubtitles) { subdl ->
                                val subRelease = subdl.releaseName?.ifBlank { subdl.name.orEmpty() } ?: subdl.name.orEmpty().ifBlank { "Subtitle" }
                                val isSelected = isSubtitleEnabled && selectedSubtitle?.label == subRelease
                                val subLang = subdl.language ?: subdl.lang ?: "ID"
                                val subAuthor = subdl.author?.ifBlank { "SubDL" } ?: "SubDL"

                                SubtitleOptionItem(
                                    title = subRelease,
                                    subtitle = "$subLang • $subAuthor",
                                    isSelected = isSelected,
                                    showDownloadIcon = !isSelected,
                                    onClick = { onSelectSubdl(subdl) }
                                )
                            }
                        }

                        // SubSource Subtitles Section (Indonesian)
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.subsource_subtitles_header),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                if (!isSearchingSubsource) {
                                    IconButton(
                                        onClick = onSearchSubsource,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = IdlixRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (isSearchingSubsource) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = IdlixRed,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.searching_subsource),
                                        fontSize = 13.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        } else if (subsourceError != null) {
                            item {
                                val errorMsg = when (subsourceError.reason) {
                                    SubsourceSearchResult.Error.Reason.NO_API_KEY -> stringResource(R.string.subsource_error_no_key)
                                    SubsourceSearchResult.Error.Reason.INVALID_KEY -> stringResource(R.string.subsource_error_invalid_key)
                                    SubsourceSearchResult.Error.Reason.QUOTA -> stringResource(R.string.subsource_error_quota)
                                    SubsourceSearchResult.Error.Reason.NETWORK -> stringResource(R.string.subsource_error_network)
                                    else -> subsourceError.message.orEmpty().ifBlank { "Error" }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x22E50914), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = IdlixRed, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = errorMsg, color = IdlixRed, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        } else if (subsourceSubtitles.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.no_subsource_found),
                                    fontSize = 13.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            if (subsourceCandidates.size > 1) {
                                item {
                                    Text(
                                        text = stringResource(R.string.subsource_pick_title),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                                    )
                                }
                                items(subsourceCandidates) { candidate ->
                                    SubtitleOptionItem(
                                        title = candidate.title ?: "Title",
                                        subtitle = "${candidate.type ?: "movie"} • ${candidate.releaseYear ?: ""}",
                                        isSelected = false,
                                        onClick = { onSelectSubsourceCandidate(candidate) }
                                    )
                                }
                            }
                        } else {
                            items(subsourceSubtitles) { sub ->
                                val isSelected = isSubtitleEnabled &&
                                    selectedSubtitle?.label == "Indonesian [SubSource]" &&
                                    sub.subtitleId != null &&
                                    selectedSubtitle?.path?.contains("subsource_id_${sub.subtitleId}_") == true
                                val meta = buildString {
                                    append("Indonesian")
                                    if (sub.hearingImpaired) append(" • HI")
                                    sub.downloads?.let { append(" • $it downloads") }
                                }
                                SubtitleOptionItem(
                                    title = sub.displayRelease,
                                    subtitle = meta,
                                    isSelected = isSelected,
                                    showDownloadIcon = !isSelected,
                                    onClick = { onSelectSubsource(sub) }
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.subtitle_text_size),
                            color = TextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.size_small),
                                    isSelected = appearance.subtitleSize == SubtitleSize.SMALL,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleSize = SubtitleSize.SMALL) } },
                                    modifier = Modifier.weight(1f)
                                )
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.size_medium),
                                    isSelected = appearance.subtitleSize == SubtitleSize.MEDIUM,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleSize = SubtitleSize.MEDIUM) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.size_large),
                                    isSelected = appearance.subtitleSize == SubtitleSize.LARGE,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleSize = SubtitleSize.LARGE) } },
                                    modifier = Modifier.weight(1f)
                                )
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.size_extra_large),
                                    isSelected = appearance.subtitleSize == SubtitleSize.EXTRA_LARGE,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleSize = SubtitleSize.EXTRA_LARGE) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.subtitle_text_color),
                            color = TextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val isCustomText = appearance.subtitleTextColor !in PlayerAppearancePresets.subtitleTextColors
                            PlayerAppearancePresets.subtitleTextColors.forEach { col ->
                                val isSelected = appearance.subtitleTextColor == col
                                MobileColorSwatch(
                                    color = col,
                                    isSelected = isSelected,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleTextColor = col) } }
                                )
                            }
                            com.idlix.mobile.ui.screens.settings.CustomColorSwatch(
                                currentColor = appearance.subtitleTextColor,
                                isCustomSelected = isCustomText,
                                onClick = { activePicker = ModalActivePicker.TEXT_COLOR }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.subtitle_box_color),
                            color = TextDim,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isTransSelected = appearance.subtitleBoxColor == 0x00000000
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.box_transparent),
                                    isSelected = isTransSelected,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleBoxColor = 0x00000000) } },
                                    modifier = Modifier.weight(1f)
                                )
                                val is50Selected = appearance.subtitleBoxColor == 0x80000000.toInt()
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.box_black_50),
                                    isSelected = is50Selected,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleBoxColor = 0x80000000.toInt()) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val is75Selected = appearance.subtitleBoxColor == 0xC0000000.toInt()
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.box_black_75),
                                    isSelected = is75Selected,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleBoxColor = 0xC0000000.toInt()) } },
                                    modifier = Modifier.weight(1f)
                                )
                                val isSolidSelected = appearance.subtitleBoxColor == 0xFF000000.toInt()
                                com.idlix.mobile.ui.screens.settings.OptionPillButton(
                                    text = stringResource(R.string.box_black_solid),
                                    isSelected = isSolidSelected,
                                    onClick = { PlayerAppearanceStore.update { it.copy(subtitleBoxColor = 0xFF000000.toInt()) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            val isCustomBox = appearance.subtitleBoxColor !in PlayerAppearancePresets.subtitleBoxColors
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCustomBox) IdlixRed else SurfaceVariantDark)
                                    .border(
                                        width = if (isCustomBox) 1.5.dp else 1.dp,
                                        color = if (isCustomBox) IdlixRed else Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { activePicker = ModalActivePicker.BOX_COLOR },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Default.Palette,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isCustomBox) Color.White else TextWhite
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCustomBox) {
                                            "${stringResource(R.string.color_custom)} (${com.idlix.data.util.ColorMath.toHex(appearance.subtitleBoxColor, includeAlpha = true)})"
                                        } else {
                                            stringResource(R.string.color_custom)
                                        },
                                        color = if (isCustomBox) Color.White else TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomBox) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onOpenPositionEditor()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IdlixRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(text = stringResource(R.string.adjust_position_on_video), color = Color.White, fontSize = 13.sp)
                    }

                    TextButton(
                        onClick = { PlayerAppearanceStore.resetAll() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextDim)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.reset_all_appearance), color = TextDim, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Custom Color Picker Sheet inside modal
    ColorPickerSheet(
        isOpen = activePicker != ModalActivePicker.NONE,
        initialColor = when (activePicker) {
            ModalActivePicker.TEXT_COLOR -> appearance.subtitleTextColor
            ModalActivePicker.BOX_COLOR -> appearance.subtitleBoxColor
            ModalActivePicker.NONE -> 0
        },
        supportsAlpha = activePicker == ModalActivePicker.BOX_COLOR,
        title = when (activePicker) {
            ModalActivePicker.TEXT_COLOR -> stringResource(R.string.subtitle_text_color)
            ModalActivePicker.BOX_COLOR -> stringResource(R.string.subtitle_box_color)
            ModalActivePicker.NONE -> ""
        },
        recentColors = recentColors,
        onColorSelected = { selected ->
            when (activePicker) {
                ModalActivePicker.TEXT_COLOR -> PlayerAppearanceStore.update { it.copy(subtitleTextColor = selected or 0xFF000000.toInt()) }
                ModalActivePicker.BOX_COLOR -> PlayerAppearanceStore.update { it.copy(subtitleBoxColor = selected) }
                ModalActivePicker.NONE -> {}
            }
            PlayerAppearanceStore.addRecentCustomColor(selected)
            activePicker = ModalActivePicker.NONE
        },
        onDismiss = { activePicker = ModalActivePicker.NONE }
    )
}

@Composable
private fun SubtitleOptionItem(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    showDownloadIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) IdlixRed.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) TextWhite else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(IdlixRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else if (showDownloadIcon) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
