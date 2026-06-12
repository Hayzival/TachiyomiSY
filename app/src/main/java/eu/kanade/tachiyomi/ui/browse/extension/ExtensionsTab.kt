package eu.kanade.tachiyomi.ui.browse.extension

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlipToBack
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.browse.ExtensionScreen
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.more.settings.screen.browse.ExtensionReposScreen
import eu.kanade.tachiyomi.extension.model.Extension
import eu.kanade.tachiyomi.ui.browse.extension.details.ExtensionDetailsScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun extensionsTab(
    extensionsScreenModel: ExtensionsScreenModel,
): TabContent {
    val navigator = LocalNavigator.currentOrThrow
    val state by extensionsScreenModel.state.collectAsState()
    var privateExtensionToUninstall by remember { mutableStateOf<Extension?>(null) }

    return TabContent(
        titleRes = MR.strings.label_extensions,
        badgeNumber = state.updates.takeIf { it > 0 },
        searchEnabled = !state.selectionMode,
        actions = if (state.selectionMode) {
            persistentListOf()
        } else {
            persistentListOf(
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.action_filter),
                    onClick = { navigator.push(ExtensionFilterScreen()) },
                ),
                AppBar.OverflowAction(
                    title = stringResource(MR.strings.label_extension_repos),
                    onClick = { navigator.push(ExtensionReposScreen()) },
                ),
            )
        },
        numberSelected = state.selection.size,
        selectionToolbar = {
            AppBar(
                titleContent = { Text(text = "${state.selection.size}") },
                actions = {
                    AppBarActions(
                        persistentListOf(
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_all),
                                icon = Icons.Outlined.SelectAll,
                                onClick = extensionsScreenModel::toggleAllSelection,
                            ),
                            AppBar.Action(
                                title = stringResource(MR.strings.action_select_inverse),
                                icon = Icons.Outlined.FlipToBack,
                                onClick = extensionsScreenModel::invertSelection,
                            ),
                        ),
                    )
                },
                isActionMode = true,
                onCancelActionMode = extensionsScreenModel::clearSelection,
            )
        },
        content = { contentPadding, _ ->
            BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
                if (state.selectionMode) {
                    extensionsScreenModel.clearSelection()
                } else {
                    extensionsScreenModel.search(null)
                }
            }

            ExtensionScreen(
                state = state,
                contentPadding = contentPadding,
                searchQuery = state.searchQuery,
                onLongClickItem = extensionsScreenModel::toggleSelection,
                onClickItemCancel = extensionsScreenModel::cancelInstallUpdateExtension,
                onClickUpdateAll = extensionsScreenModel::updateAllExtensions,
                onOpenWebView = { extension ->
                    extension.sources.getOrNull(0)?.let {
                        navigator.push(
                            WebViewScreen(
                                url = it.baseUrl,
                                initialTitle = it.name,
                                sourceId = it.id,
                            ),
                        )
                    }
                },
                onInstallExtension = extensionsScreenModel::installExtension,
                onOpenExtension = { navigator.push(ExtensionDetailsScreen(it.pkgName)) },
                onTrustExtension = { extensionsScreenModel.trustExtension(it) },
                onUninstallExtension = { extensionsScreenModel.uninstallExtension(it) },
                onUpdateExtension = extensionsScreenModel::updateExtension,
                onRefresh = extensionsScreenModel::findAvailableExtensions,
                // SY -->
                onClickTrustSelected = extensionsScreenModel::trustSelected,
                onClickUninstallSelected = extensionsScreenModel::uninstallSelected,
                onClickUpdateSelected = extensionsScreenModel::updateSelected,
                // SY <--
            )

            privateExtensionToUninstall?.let { extension ->
                ExtensionUninstallConfirmation(
                    extensionName = extension.name,
                    onClickConfirm = {
                        extensionsScreenModel.uninstallExtension(extension)
                    },
                    onDismissRequest = {
                        privateExtensionToUninstall = null
                    },
                )
            }

            LaunchedEffect(state.selectionMode) {
                HomeScreen.showBottomNav(!state.selectionMode)
            }
        },
    )
}

@Composable
private fun ExtensionUninstallConfirmation(
    extensionName: String,
    onClickConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = stringResource(MR.strings.ext_confirm_remove))
        },
        text = {
            Text(text = stringResource(MR.strings.remove_private_extension_message, extensionName))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onClickConfirm()
                    onDismissRequest()
                },
            ) {
                Text(text = stringResource(MR.strings.ext_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    )
}
