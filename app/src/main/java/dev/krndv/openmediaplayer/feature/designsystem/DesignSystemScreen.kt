package dev.krndv.openmediaplayer.feature.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.krndv.openmediaplayer.R
import dev.krndv.openmediaplayer.core.design.components.FamilySectionHeader
import dev.krndv.openmediaplayer.core.design.components.SettingSwitchRow
import dev.krndv.openmediaplayer.core.design.tokens.FamilySpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemScreen(
    dynamicColorEnabled: Boolean,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    var selectedFilter by rememberSaveable { mutableStateOf("all") }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.design_system_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        containerColor = colorScheme.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = FamilySpacing.pageHorizontal,
                    vertical = FamilySpacing.large,
                ),
            verticalArrangement = Arrangement.spacedBy(FamilySpacing.section),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
                Text(
                    text = stringResource(R.string.design_system_eyebrow),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.secondary,
                )
                Text(
                    text = stringResource(R.string.design_system_headline),
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.design_system_intro),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(FamilySpacing.extraLarge),
                    verticalArrangement = Arrangement.spacedBy(FamilySpacing.medium),
                ) {
                    Text(
                        text = stringResource(R.string.shared_foundation_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.shared_foundation_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onPrimaryContainer,
                    )
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.preview_dialog_button))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.large)) {
                FamilySectionHeader(title = stringResource(R.string.color_roles_title))
                Row(horizontalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
                    ColorRole(
                        name = stringResource(R.string.color_primary),
                        color = colorScheme.primary,
                        onColor = colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    ColorRole(
                        name = stringResource(R.string.color_secondary),
                        color = colorScheme.secondary,
                        onColor = colorScheme.onSecondary,
                        modifier = Modifier.weight(1f),
                    )
                    ColorRole(
                        name = stringResource(R.string.color_tertiary),
                        color = colorScheme.tertiary,
                        onColor = colorScheme.onTertiary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.large)) {
                FamilySectionHeader(title = stringResource(R.string.component_examples_title))
                Row(horizontalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
                    Button(
                        onClick = { showDialog = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.button_filled), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = { selectedFilter = "audio" },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.button_tonal), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { selectedFilter = "video" },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.button_outlined), maxLines = 1)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.large)) {
                FamilySectionHeader(
                    title = stringResource(R.string.interaction_examples_title),
                    description = stringResource(R.string.filter_description),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(FamilySpacing.small)) {
                    FilterChip(
                        selected = selectedFilter == "all",
                        onClick = { selectedFilter = "all" },
                        label = { Text(stringResource(R.string.filter_all)) },
                    )
                    FilterChip(
                        selected = selectedFilter == "audio",
                        onClick = { selectedFilter = "audio" },
                        label = { Text(stringResource(R.string.filter_audio)) },
                    )
                    FilterChip(
                        selected = selectedFilter == "video",
                        onClick = { selectedFilter = "video" },
                        label = { Text(stringResource(R.string.filter_video)) },
                    )
                }
                Text(
                    text = stringResource(R.string.selected_filter) + ": " + when (selectedFilter) {
                        "audio" -> stringResource(R.string.filter_audio)
                        "video" -> stringResource(R.string.filter_video)
                        else -> stringResource(R.string.filter_all)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(FamilySpacing.large)) {
                FamilySectionHeader(title = stringResource(R.string.settings_pattern_title))
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
                ) {
                    SettingSwitchRow(
                        title = stringResource(R.string.dynamic_color_title),
                        description = stringResource(R.string.dynamic_color_description),
                        checked = dynamicColorEnabled,
                        onCheckedChange = onDynamicColorChange,
                    )
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.dialog_title)) },
            text = { Text(stringResource(R.string.dialog_description)) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
        )
    }
}

@Composable
private fun ColorRole(
    name: String,
    color: Color,
    onColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FamilySpacing.extraSmall),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            color = color,
            contentColor = onColor,
            shape = MaterialTheme.shapes.large,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "Aa",
                    style = MaterialTheme.typography.titleLarge,
                    color = onColor,
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
