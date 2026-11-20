package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.AppDivider
import com.v2ray.ang.ui.compose.LocalStealthMode
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.verticalScrollbar
import com.v2ray.ang.util.Utils

enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    CustomTheme(R.drawable.ic_image_24dp, R.string.title_pref_custom_theme),
    Promotion(R.drawable.ic_idea_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    CheckUpdate(R.drawable.ic_check_update_24dp, R.string.update_check_for_update),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

private val mainDrawerItems = listOf(
    MainDestination.CustomTheme,
    MainDestination.PerAppProxy,
    MainDestination.Settings,
    MainDestination.Promotion,
    MainDestination.CheckUpdate,
    MainDestination.About
)

private val advancedDrawerItems = listOf(
    MainDestination.Subscriptions,
    MainDestination.Routing,
    MainDestination.UserAssets,
    MainDestination.BackupRestore,
    MainDestination.Logcat
)

@Composable
fun MainDrawerContent(drawerState: DrawerState, onNavigate: (MainDestination) -> Unit) {
    val drawerScrollState = rememberScrollState()
    val context = LocalContext.current
    var showAdvanced by remember { mutableStateOf(false) }
    val isStealth = LocalStealthMode.current || ThemeManager.isStealthMode.collectAsState().value

    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.fillMaxWidth(0.75f),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(drawerScrollState)
                .verticalScrollbar(drawerScrollState)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (isStealth) 20.dp else 28.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isStealth) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E2638))
                                .clickable { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock_24dp),
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.bunker_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isStealth) "Stealth Edition" else "@bunker_kota",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { Utils.openUri(context, AppConfig.TG_CHANNEL_URL) }
                    )
                }
            }

            mainDrawerItems.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(stringResource(item.labelRes)) },
                    selected = false,
                    onClick = { onNavigate(item) },
                    icon = { Icon(painterResource(item.iconRes), contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }

            AppDivider(modifier = Modifier.padding(vertical = 6.dp))

            NavigationDrawerItem(
                label = { Text(stringResource(R.string.title_advanced_settings)) },
                selected = showAdvanced,
                onClick = { showAdvanced = !showAdvanced },
                icon = { Icon(painterResource(R.drawable.ic_source_code_24dp), contentDescription = null) },
                badge = {
                    Icon(
                        painter = painterResource(R.drawable.ic_expand_more_24dp),
                        contentDescription = null,
                        modifier = Modifier.graphicsLayer { rotationZ = if (showAdvanced) 180f else 0f }
                    )
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            if (showAdvanced) {
                advancedDrawerItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(item.labelRes), style = MaterialTheme.typography.bodyMedium) },
                        selected = false,
                        onClick = { onNavigate(item) },
                        icon = { Icon(painterResource(item.iconRes), contentDescription = null, modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.padding(start = 16.dp, end = 12.dp, top = 2.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}
