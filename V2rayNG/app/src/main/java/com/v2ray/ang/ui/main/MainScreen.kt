package com.v2ray.ang.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.core.LauncherManager
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.extension.toast
import com.v2ray.ang.handler.InAppUpdateDialog
import com.v2ray.ang.handler.SettingsManager
import com.v2ray.ang.handler.UpdateCheckerManager
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.QRCodeDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val displayText = mainViewModel.formatStatus(uiState.status)
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val hideOfflineServers = uiState.hideOfflineServers
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    val isDarkTheme = LocalDarkTheme.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isAntiRknEnabled by remember {
        mutableStateOf(SettingsManager.isAntiRknRoutingEnabled())
    }
    var availableUpdateVersion by remember { mutableStateOf<String?>(null) }
    var updateDownloadUrl by remember { mutableStateOf<String?>(null) }
    var updateReleaseNotes by remember { mutableStateOf<String?>(null) }
    var showInAppUpdateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val result = UpdateCheckerManager.checkForUpdate(includePreRelease = true)
                if (result.hasUpdate) {
                    withContext(Dispatchers.Main) {
                        availableUpdateVersion = result.latestVersion
                        updateDownloadUrl = result.downloadUrl
                        updateReleaseNotes = result.releaseNotes
                    }
                }
            }
        }
    }

    val handleToggleAntiRkn: () -> Unit = {
        val nextState = !isAntiRknEnabled
        SettingsManager.toggleAntiRknRouting(context, nextState)
        isAntiRknEnabled = nextState
        if (nextState) {
            context.toast("⚡ Включен режим Анти-РКН: банки и РФ-сайты идут напрямую!")
        } else {
            context.toast("🌐 Включен Полный туннель: весь интернет идет через VPN")
        }
        if (isRunning) {
            LauncherManager.restartService(context)
        }
    }

    val handleUpdateClick: () -> Unit = {
        showInAppUpdateDialog = true
    }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) {
        mutableStateOf<ServerDeleteTarget?>(null)
    }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    val removeServer: (String, String) -> Unit = { guid, profileName ->
        if (confirmRemove) {
            showRemoveConfirm = ServerDeleteTarget(guid, profileName)
        } else {
            onAction(MainAction.RemoveServer(guid))
        }
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { groups.size.coerceAtLeast(1) }
    )

    val lazyListStates = remember { mutableStateMapOf<String, LazyListState>() }
    val lazyGridStates = remember { mutableStateMapOf<String, LazyGridState>() }

    LaunchedEffect(groups) {
        val validGroupIds = groups.map { it.id }.toSet()
        lazyListStates.keys.retainAll(validGroupIds)
        lazyGridStates.keys.retainAll(validGroupIds)
    }

    LaunchedEffect(groups, uiState.selectedGroupId) {
        if (groups.isEmpty()) return@LaunchedEffect
        val selectedIndex = groups.indexOfFirst { it.id == uiState.selectedGroupId }
            .takeIf { it >= 0 } ?: 0
        if (!pagerState.isScrollInProgress && pagerState.settledPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    val latestGroups by rememberUpdatedState(groups)
    // Height of the floating connection bar, so the server lists can scroll clear of it.
    var bottomBarHeightPx by remember { mutableIntStateOf(0) }
    var showSpeedtestDialog by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val currentGroups = latestGroups
                if (page in currentGroups.indices) {
                    onAction(MainAction.SelectGroup(currentGroups[page].id))
                }
            }
    }

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    if (showSpeedtestDialog) {
        BunkerSpeedtestDialog(
            show = showSpeedtestDialog,
            onDismiss = { showSpeedtestDialog = false },
            serverName = mainViewModel.getSelectedServerRemarks(),
            proxyPort = SettingsManager.getHttpPort()
        )
    }

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }

    if (showInAppUpdateDialog && !availableUpdateVersion.isNullOrBlank() && !updateDownloadUrl.isNullOrBlank()) {
        InAppUpdateDialog(
            latestVersion = availableUpdateVersion!!,
            releaseNotes = updateReleaseNotes,
            downloadUrl = updateDownloadUrl!!,
            onDismissRequest = { showInAppUpdateDialog = false }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            topBar = {
                MainTopBar(
                    isLoading = isLoading,
                    showSearch = showSearch,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query: String ->
                        searchQuery = query
                        onAction(MainAction.Search(query))
                    },
                    onSearchClose = {
                        searchQuery = ""
                        onAction(MainAction.Search(""))
                        showSearch = false
                    },
                    onSearchToggle = { show: Boolean -> showSearch = show },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAction = onAction,
                    onMoreMenuAction = { action ->
                        when (action) {
                            MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                            MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                            MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                            MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                            MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                            MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                            MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                            MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                            MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                            MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                        }
                    }
                )
            },
            floatingActionButton = {},
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val bottomBarHeight = with(LocalDensity.current) { bottomBarHeightPx.toDp() }

            Box(Modifier.fillMaxSize()) {
            if (groups.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // No bottom padding: the lists run behind the floating connection bar.
                        .padding(
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            top = innerPadding.calculateTopPadding(),
                            end = innerPadding.calculateEndPadding(layoutDirection)
                        )
                ) {
                    if (groups.size > 1) {
                        GroupTabBar(
                            groups = groups,
                            selectedTabIndex = pagerState.currentPage.coerceIn(0, groups.lastIndex),
                            mainViewModel = mainViewModel,
                            onTabClick = { targetIndex ->
                                scope.launch {
                                    pagerState.navigateToPageOptimized(
                                        targetPage = targetIndex,
                                        animateAdjacentPage = true
                                    )
                                }
                            }
                        )
                    }

                    val activeGroup = groups.getOrNull(pagerState.currentPage.coerceIn(0, groups.lastIndex))
                    val activeGroupId = activeGroup?.id.orEmpty()
                    val activeGroupStateFlow = remember(activeGroupId) {
                        mainViewModel.serverGroupState(activeGroupId)
                    }
                    val activeGroupState by activeGroupStateFlow.collectAsStateWithLifecycle()

                    val realServers = remember(activeGroupState.servers) {
                        activeGroupState.servers.filter { it.profile.configType != EConfigType.POLICYGROUP }
                    }
                    val totalServersCount = realServers.size
                    val testedServersCount = realServers.count { it.testDelayMillis != 0L }
                    val workingServersCount = realServers.count { it.testDelayMillis > 0L }

                    BunkerHeroHeader(
                        isRunning = isRunning,
                        statusText = displayText,
                        currentServerRemark = mainViewModel.getSelectedServerRemarks(),
                        isTesting = uiState.isTesting,
                        workingServersCount = workingServersCount,
                        totalServersCount = totalServersCount,
                        testedServersCount = testedServersCount,
                        serversInGroup = realServers,
                        isAntiRknEnabled = isAntiRknEnabled,
                        onToggleAntiRkn = { handleToggleAntiRkn() },
                        availableUpdateVersion = availableUpdateVersion,
                        onUpdateClick = { handleUpdateClick() },
                        onToggle = { onAction(MainAction.ToggleService) },
                        onTestPing = { onAction(MainAction.TestRealAllServers) },
                        onAutoSelect = { onAction(MainAction.AutoSelectBestServer) },
                        onSortByPing = { onAction(MainAction.SortByTestResults) },
                        onSpeedtestClick = { showSpeedtestDialog = true },
                        onUpdateSub = { onAction(MainAction.UpdateSubscriptions) },
                        hideOfflineServers = hideOfflineServers,
                        onToggleHideOffline = { onAction(MainAction.ToggleHideOfflineServers) },
                        doubleColumnDisplay = doubleColumnDisplay,
                        onToggleDoubleColumn = { onAction(MainAction.ToggleDoubleColumn) },
                        onRemoveInvalid = { onAction(MainAction.RemoveInvalidServers) },
                        onTelegramSos = { onAction(MainAction.TelegramSos) },
                        onGenerateWarp = { onAction(MainAction.GenerateWarp) },
                        onSmartClean = { onAction(MainAction.SmartCleanServers) }
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        userScrollEnabled = true,
                        beyondViewportPageCount = 1,
                        key = { page -> groups.getOrNull(page)?.id ?: "group-page-$page" }
                    ) { page ->
                        val group = groups.getOrNull(page) ?: return@HorizontalPager

                        GroupPagerPage(
                            groupId = group.id,
                            mainViewModel = mainViewModel,
                            selectedGuid = selectedGuid,
                            locateTarget = uiState.locateTarget,
                            doubleColumnDisplay = doubleColumnDisplay,
                            hideOfflineServers = hideOfflineServers,
                            searchQuery = searchQuery,
                            lazyListStates = lazyListStates,
                            lazyGridStates = lazyGridStates,
                            onSelectServer = { guid -> onAction(MainAction.SelectServer(guid)) },
                            onEditServer = { guid, profile -> onAction(MainAction.EditServer(guid, profile)) },
                            onShareServer = { guid, profile ->
                                shareTarget = Triple(guid, profile, false)
                            },
                            onMoreServer = { guid, profile ->
                                shareTarget = Triple(guid, profile, true)
                            },
                            onRemoveServer = removeServer,
                            usageInfo = group.usageInfo,
                            groupTitle = group.remarks,
                            onOpenLink = { url -> onAction(MainAction.OpenSubscriptionLink(url)) },
                            contentPadding = PaddingValues(
                                start = 0.dp,
                                top = 0.dp,
                                end = 0.dp,
                                bottom = bottomBarHeight + 36.dp
                            )
                        )
                    }
                }
            }
            MainBottomBar(
                displayText = displayText,
                isRunning = isRunning,
                isDarkTheme = isDarkTheme,
                onAction = onAction,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { bottomBarHeightPx = it.height }
            )
            }
        }
    }
}
