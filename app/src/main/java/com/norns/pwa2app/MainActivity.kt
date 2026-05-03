package com.norns.pwa2app

import android.annotation.SuppressLint
import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.webkit.URLUtil
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.norns.pwa2app.ui.theme.PWA2APPTheme
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private var shortcutLaunchUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        val launchStartedAt = SystemClock.uptimeMillis()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = transparentSystemBarStyle(),
            navigationBarStyle = transparentSystemBarStyle()
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        splashScreen.setKeepOnScreenCondition {
            SystemClock.uptimeMillis() - launchStartedAt < MIN_SPLASH_DURATION_MS
        }
        splashScreen.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            val iconFade = ObjectAnimator.ofFloat(icon, "alpha", 1f, 0f)
            val iconScaleX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 0.86f)
            val iconScaleY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 0.86f)
            val lift = ObjectAnimator.ofFloat(provider.view, "translationY", 0f, -provider.view.height * 0.06f)
            val fade = ObjectAnimator.ofFloat(provider.view, "alpha", 1f, 0f)
            AnimatorSet().apply {
                duration = SPLASH_EXIT_DURATION_MS
                playTogether(iconFade, iconScaleX, iconScaleY, lift, fade)
                doOnEnd { provider.remove() }
                start()
            }
        }
        shortcutLaunchUrl = intent.getStringExtra(EXTRA_SHORTCUT_URL)
        setContent {
            PWA2APPTheme {
                PwaContainerApp(
                    shortcutLaunchUrl = shortcutLaunchUrl,
                    onShortcutLaunchConsumed = { shortcutLaunchUrl = null },
                    onFinishShortcutSession = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutLaunchUrl = intent.getStringExtra(EXTRA_SHORTCUT_URL)
    }
}

class WebNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notification = intent.parcelableExtra<WebNotification>(EXTRA_WEB_NOTIFICATION)
        val notificationId = intent.getIntExtra(
            EXTRA_WEB_NOTIFICATION_ID,
            notification?.tag?.hashCode() ?: 0
        )
        val launchUrl = resolveNotificationLaunchUrl(
            intent.getStringExtra(EXTRA_SHORTCUT_URL)
        ) ?: resolveNotificationLaunchUrl(notification?.source)

        when (intent.action) {
            ACTION_WEB_NOTIFICATION_CLICK -> {
                val actionName = intent.getStringExtra(EXTRA_WEB_NOTIFICATION_ACTION)
                runCatching {
                    if (actionName.isNullOrBlank()) {
                        notification?.click()
                    } else {
                        notification?.click(actionName)
                    }
                }
                launchMainActivity(context, launchUrl)
            }

            ACTION_WEB_NOTIFICATION_DISMISS -> {
                runCatching { notification?.dismiss() }
            }
        }

        if (notificationId != 0) {
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }
}

@Composable
fun PwaContainerApp(
    shortcutLaunchUrl: String?,
    onShortcutLaunchConsumed: () -> Unit,
    onFinishShortcutSession: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sites by remember { mutableStateOf(loadSites(context)) }
    var editingName by rememberSaveable { mutableStateOf("") }
    var editingUrl by rememberSaveable { mutableStateOf("") }
    var currentUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isShortcutSession by rememberSaveable { mutableStateOf(false) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSiteActionDialog by remember { mutableStateOf<PendingSiteActionDialog?>(null) }

    LaunchedEffect(shortcutLaunchUrl, sites) {
        if (shortcutLaunchUrl.isNullOrBlank()) return@LaunchedEffect
        val normalized = normalizeUrl(shortcutLaunchUrl)
        if (normalized != null) {
            currentUrl = normalized
            isShortcutSession = true
        }
        onShortcutLaunchConsumed()
    }

    if (currentUrl == null) {
        HomeScreen(
            sites = sites,
            inputName = editingName,
            inputUrl = editingUrl,
            errorText = errorText,
            onNameChanged = {
                editingName = it
                errorText = null
            },
            onInputChanged = {
                editingUrl = it
                errorText = null
            },
            onAddClicked = {
                val normalized = normalizeUrl(editingUrl)
                if (normalized == null) {
                    errorText = "Please enter a valid URL, e.g. https://example.com"
                    return@HomeScreen
                }
                if (sites.any { it.url.equals(normalized, ignoreCase = true) }) {
                    errorText = "This URL is already saved"
                    return@HomeScreen
                }
                val name = editingName.trim().ifBlank { defaultNameForUrl(normalized) }
                val newSite = SavedSite(
                    id = siteIdFromUrl(normalized),
                    name = name,
                    url = normalized,
                    iconUrl = null
                )
                sites = listOf(newSite) + sites
                saveSites(context, sites)
                editingName = ""
                editingUrl = ""
                errorText = null
            },
            onOpenClicked = { site ->
                isShortcutSession = false
                currentUrl = site.url
                ShortcutManagerCompat.reportShortcutUsed(context, site.id)
            },
            onCreateShortcutClicked = { site ->
                scope.launch {
                    val resolvedIconUrl = site.iconUrl ?: resolveSiteIconUrl(site.url)
                    val updatedSite = if (!resolvedIconUrl.isNullOrBlank() && site.iconUrl != resolvedIconUrl) {
                        val refreshed = site.copy(iconUrl = resolvedIconUrl)
                        sites = sites.map { current ->
                            if (current.id == site.id) refreshed else current
                        }
                        saveSites(context, sites)
                        refreshed
                    } else {
                        site
                    }
                    val shortcutBitmap = loadShortcutIconBitmap(context, updatedSite)
                    val created = createPinnedShortcut(context, updatedSite, shortcutBitmap)
                    val message = if (created) {
                        "Shortcut request sent for ${updatedSite.name}"
                    } else {
                        "Launcher does not support pinned shortcuts"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            },
            onDeleteClicked = { site ->
                pendingSiteActionDialog = PendingSiteActionDialog(
                    site = site,
                    action = SiteAction.DeleteSite
                )
            },
            onClearSessionClicked = { site ->
                pendingSiteActionDialog = PendingSiteActionDialog(
                    site = site,
                    action = SiteAction.ClearSession
                )
            },
            onSiteIconResolved = { site, iconUrl ->
                if (site.iconUrl == iconUrl) return@HomeScreen
                val updatedSites = sites.map { current ->
                    if (current.id == site.id) current.copy(iconUrl = iconUrl) else current
                }
                sites = updatedSites
                saveSites(context, updatedSites)
            }
        )
        pendingSiteActionDialog?.let { dialog ->
            SiteActionConfirmDialog(
                dialog = dialog,
                onDismiss = { pendingSiteActionDialog = null },
                onConfirm = {
                    pendingSiteActionDialog = null
                    when (dialog.action) {
                        SiteAction.DeleteSite -> {
                            sites = sites.filterNot { it.id == dialog.site.id }
                            saveSites(context, sites)
                        }

                        SiteAction.ClearSession -> {
                            scope.launch {
                                val cleared = clearSiteSession(context, dialog.site)
                                val message = if (cleared) {
                                    "Session cleared for ${dialog.site.name}"
                                } else {
                                    "Failed to clear session for ${dialog.site.name}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            )
        }
    } else {
        val displayName = sites.firstOrNull { it.url.equals(currentUrl, ignoreCase = true) }?.name
            ?: defaultNameForUrl(currentUrl!!)
        PwaBrowserScreen(
            initialTitle = displayName,
            initialUrl = currentUrl!!,
            showTopBar = !isShortcutSession,
            onClose = {
                if (isShortcutSession) {
                    onFinishShortcutSession()
                } else {
                    currentUrl = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    sites: List<SavedSite>,
    inputName: String,
    inputUrl: String,
    errorText: String?,
    onNameChanged: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onAddClicked: () -> Unit,
    onOpenClicked: (SavedSite) -> Unit,
    onCreateShortcutClicked: (SavedSite) -> Unit,
    onDeleteClicked: (SavedSite) -> Unit,
    onClearSessionClicked: (SavedSite) -> Unit,
    onSiteIconResolved: (SavedSite, String) -> Unit
) {
    var revealContent by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(HOME_REVEAL_DELAY_MS)
        revealContent = true
    }

    SystemBarsEffect(
        statusBarColor = MaterialTheme.colorScheme.background,
        navigationBarColor = MaterialTheme.colorScheme.background
    )

    val statusBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        FluentBackdrop()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = innerPadding.calculateTopPadding() + statusBarTopPadding + 10.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FluentReveal(visible = revealContent, delayMillis = 0) {
                        HeroPanel(siteCount = sites.size)
                    }
                }

                item {
                    FluentReveal(visible = revealContent, delayMillis = 80) {
                        GlassPanel {
                            Text(
                                text = "Create a web app",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add a PWA URL, give it a name, then pin it to the home screen in a cleaner app shell.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FluentTextField(
                                value = inputName,
                                onValueChange = onNameChanged,
                                label = "App Name (optional)",
                                placeholder = "My PWA"
                            )
                            FluentTextField(
                                value = inputUrl,
                                onValueChange = onInputChanged,
                                label = "PWA URL",
                                placeholder = "https://example.com",
                                isError = errorText != null
                            )
                            AnimatedVisibility(
                                visible = errorText != null,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.error,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = errorText.orEmpty(),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Button(
                                onClick = onAddClicked,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text("Save app")
                            }
                        }
                    }
                }

                item {
                    FluentReveal(visible = revealContent, delayMillis = 140) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Saved apps",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Open directly or create shortcut icons",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CountBadge(siteCount = sites.size)
                        }
                    }
                }

                if (sites.isEmpty()) {
                    item {
                        FluentReveal(visible = revealContent, delayMillis = 180) {
                            EmptyStatePanel()
                        }
                    }
                } else {
                    itemsIndexed(sites, key = { _, site -> site.id }) { index, site ->
                        FluentReveal(visible = revealContent, delayMillis = 180 + (index * 45)) {
                            SiteCard(
                                site = site,
                                onOpenClicked = { onOpenClicked(site) },
                                onCreateShortcutClicked = { onCreateShortcutClicked(site) },
                                onClearSessionClicked = { onClearSessionClicked(site) },
                                onDeleteClicked = { onDeleteClicked(site) },
                                onSiteIconResolved = { iconUrl -> onSiteIconResolved(site, iconUrl) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PwaBrowserScreen(
    initialTitle: String,
    initialUrl: String,
    showTopBar: Boolean,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val appContext = context.applicationContext
    val runtime = remember(appContext) { GeckoRuntimeHolder.get(appContext) }
    val scope = rememberCoroutineScope()
    var geckoViewRef by remember(initialUrl) { mutableStateOf<GeckoView?>(null) }
    var geckoSessionRef by remember(initialUrl) { mutableStateOf<GeckoSession?>(null) }
    var canGoBack by remember(initialUrl) { mutableStateOf(false) }
    var pageTitle by remember(initialUrl) { mutableStateOf(initialTitle) }
    var pageUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
    var isLoading by remember(initialUrl) { mutableStateOf(true) }
    var isFullscreen by remember(initialUrl) { mutableStateOf(false) }
    var pendingPermissionRequest by remember(initialUrl) { mutableStateOf<PendingAndroidPermissionRequest?>(null) }
    var pendingFilePrompt by remember(initialUrl) { mutableStateOf<PendingFilePromptRequest?>(null) }
    var pendingIntentSenderRequest by remember(initialUrl) { mutableStateOf<PendingIntentSenderRequest?>(null) }
    var pendingAlertPromptDialog by remember(initialUrl) { mutableStateOf<PendingAlertPromptDialog?>(null) }
    var pendingButtonPromptDialog by remember(initialUrl) { mutableStateOf<PendingButtonPromptDialog?>(null) }
    var pendingTextPromptDialog by remember(initialUrl) { mutableStateOf<PendingTextPromptDialog?>(null) }
    var pendingAuthPromptDialog by remember(initialUrl) { mutableStateOf<PendingAuthPromptDialog?>(null) }
    val defaultChromeColor = MaterialTheme.colorScheme.surface
    var chromeColor by remember(initialUrl) { mutableStateOf(defaultChromeColor) }
    val onChromeColor = if (chromeColor.luminance() > 0.5f) Color(0xFF111827) else Color.White
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val effectiveShowTopBar = showTopBar && !isFullscreen

    val appPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        pendingPermissionRequest?.onResult(grantResults)
        pendingPermissionRequest = null
    }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val request = pendingFilePrompt
        pendingFilePrompt = null
        if (request != null) {
            completePendingFilePrompt(
                context = appContext,
                request = request,
                resultCode = activityResult.resultCode,
                data = activityResult.data
            )
        }
    }

    val webAuthnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val request = pendingIntentSenderRequest
        pendingIntentSenderRequest = null
        if (request != null) {
            if (activityResult.resultCode == Activity.RESULT_OK) {
                request.result.complete(activityResult.data ?: Intent())
            } else {
                request.result.completeExceptionally(
                    CancellationException("WebAuthn activity was cancelled")
                )
            }
        }
    }

    fun requestAndroidPermissions(
        permissions: Array<String>,
        onResult: (Boolean) -> Unit
    ) {
        val requiredPermissions = permissions
            .filter { it.isNotBlank() }
            .distinct()
            .filterNot { permission -> hasAppPermission(context, permission) }

        if (requiredPermissions.isEmpty()) {
            onResult(true)
            return
        }

        if (activity == null) {
            onResult(false)
            return
        }

        pendingPermissionRequest = PendingAndroidPermissionRequest(
            permissions = requiredPermissions,
            onCompleted = { grantResults ->
                onResult(requiredPermissions.all { permission -> grantResults[permission] == true })
            }
        )
        appPermissionLauncher.launch(requiredPermissions.toTypedArray())
    }

    DisposableEffect(runtime, activity) {
        val activityDelegate = object : GeckoRuntime.ActivityDelegate {
            override fun onStartActivityForResult(intent: PendingIntent): GeckoResult<Intent>? {
                val result = GeckoResult<Intent>()
                if (activity == null) {
                    result.completeExceptionally(
                        IllegalStateException("No Activity available for WebAuthn flow")
                    )
                    return result
                }
                pendingIntentSenderRequest = PendingIntentSenderRequest(result)
                webAuthnLauncher.launch(
                    IntentSenderRequest.Builder(intent.intentSender).build()
                )
                return result
            }
        }

        val notificationDelegate = object : WebNotificationDelegate {
            override fun onShowNotification(notification: WebNotification) {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !hasAppPermission(appContext, Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    notification.dismiss()
                    return
                }
                showWebNotification(appContext, notification)
            }

            override fun onCloseNotification(notification: WebNotification) {
                NotificationManagerCompat.from(appContext).cancel(notification.tag.hashCode())
                notification.dismiss()
            }
        }

        val serviceWorkerDelegate = object : GeckoRuntime.ServiceWorkerDelegate {
            override fun onOpenWindow(url: String): GeckoResult<GeckoSession> {
                val session = geckoSessionRef
                val result = GeckoResult<GeckoSession>()
                if (session == null || !session.isOpen) {
                    result.completeExceptionally(
                        IllegalStateException("No active session available for service worker window")
                    )
                    return result
                }
                session.loadUri(url)
                result.complete(session)
                return result
            }
        }

        runtime.setActivityDelegate(activityDelegate)
        runtime.setWebNotificationDelegate(notificationDelegate)
        runtime.setServiceWorkerDelegate(serviceWorkerDelegate)

        onDispose {
            if (runtime.getActivityDelegate() === activityDelegate) {
                runtime.setActivityDelegate(null)
            }
            if (runtime.getWebNotificationDelegate() === notificationDelegate) {
                runtime.setWebNotificationDelegate(null)
            }
            if (runtime.getServiceWorkerDelegate() === serviceWorkerDelegate) {
                runtime.setServiceWorkerDelegate(null)
            }
        }
    }

    SystemBarsEffect(
        statusBarColor = if (isFullscreen) Color.Black else chromeColor,
        navigationBarColor = if (isFullscreen) Color.Black else MaterialTheme.colorScheme.background,
        immersive = isFullscreen
    )

    LaunchedEffect(pageUrl) {
        resolveThemeColorForUrl(pageUrl)?.let { resolvedColor ->
            chromeColor = resolvedColor
        }
    }

    LaunchedEffect(geckoViewRef, pageUrl, isFullscreen) {
        val geckoView = geckoViewRef ?: return@LaunchedEffect
        while (isActive && geckoViewRef === geckoView) {
            geckoView.capturePixels().accept(
                { bitmap ->
                    bitmap?.let { nonNullBitmap ->
                        sampleBrowserChromeColor(nonNullBitmap)?.let { sampledColor ->
                            chromeColor = sampledColor
                        }
                    }
                },
                { null }
            )
            delay(CHROME_COLOR_SAMPLE_INTERVAL_MS)
        }
    }

    BackHandler {
        val session = geckoSessionRef
        if (session != null && canGoBack) {
            session.goBack()
        } else {
            onClose()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (effectiveShowTopBar) {
                val secondaryButtonColors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = onChromeColor.copy(alpha = 0.14f),
                    contentColor = onChromeColor
                )
                val primaryButtonColors = ButtonDefaults.buttonColors(
                    containerColor = onChromeColor,
                    contentColor = if (onChromeColor.luminance() > 0.5f) {
                        Color(0xFF0F172A)
                    } else {
                        Color.White
                    }
                )
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = {
                        Text(
                            text = pageTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = chromeColor,
                        titleContentColor = onChromeColor,
                        navigationIconContentColor = onChromeColor,
                        actionIconContentColor = onChromeColor
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        FilledTonalButton(
                            onClick = {
                                val session = geckoSessionRef
                                if (session != null && canGoBack) {
                                    session.goBack()
                                } else {
                                    onClose()
                                }
                            },
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .height(36.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = secondaryButtonColors,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text("Back")
                        }
                    },
                    actions = {
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .height(36.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = primaryButtonColors,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                        ) {
                            Text("Exit")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!effectiveShowTopBar && !isFullscreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                        .background(chromeColor)
                )
            }
            AndroidView(
                modifier = Modifier
                    .padding(
                        top = when {
                            isFullscreen -> 0.dp
                            effectiveShowTopBar -> 0.dp
                            else -> statusBarHeight
                        }
                    )
                    .fillMaxSize(),
                factory = { context ->
                    val session = createAppGeckoSession(
                        context = context,
                        runtime = runtime,
                        initialUrl = initialUrl,
                        onCanGoBackChanged = { canGoBack = it },
                        onPageTitleChanged = { resolvedTitle ->
                            pageTitle = resolvedTitle?.trim().takeUnless { it.isNullOrBlank() } ?: initialTitle
                        },
                        onPageUrlChanged = { resolvedUrl ->
                            if (resolvedUrl.isNotBlank()) {
                                pageUrl = resolvedUrl
                            }
                        },
                        onLoadingChanged = { loading ->
                            isLoading = loading
                        },
                        onManifestThemeColorResolved = { resolvedColor ->
                            if (resolvedColor != null) {
                                chromeColor = resolvedColor
                            }
                        },
                        onCloseRequested = onClose,
                        onFullScreenChanged = { fullScreen ->
                            isFullscreen = fullScreen
                        },
                        onExternalResponse = { response ->
                            scope.launch {
                                val savedDownload = saveExternalResponse(context, response)
                                if (savedDownload == null) {
                                    Toast.makeText(
                                        context,
                                        "Unable to download this file",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                Toast.makeText(
                                    context,
                                    "Downloaded ${savedDownload.fileName}",
                                    Toast.LENGTH_SHORT
                                ).show()

                                if (savedDownload.openAfterSave) {
                                    openDownloadedFile(
                                        context = context,
                                        fileUri = savedDownload.uri,
                                        mimeType = savedDownload.mimeType
                                    )
                                }
                            }
                        },
                        onAndroidPermissionsRequested = { permissions, callback ->
                            val requestedPermissions = permissions?.filterNotNull()?.toTypedArray()
                                ?: emptyArray()
                            requestAndroidPermissions(requestedPermissions) { granted ->
                                if (granted) callback.grant() else callback.reject()
                            }
                        },
                        onContentPermissionRequested = { permission ->
                            val result = GeckoResult<Int>()
                            if (
                                permission.permission == GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ) {
                                requestAndroidPermissions(
                                    arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                                ) { granted ->
                                    completeContentPermissionRequest(
                                        runtime = runtime,
                                        permission = permission,
                                        result = result,
                                        value = if (granted) {
                                            GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                                        } else {
                                            GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                                        }
                                    )
                                }
                            } else {
                                completeContentPermissionRequest(
                                    runtime = runtime,
                                    permission = permission,
                                    result = result,
                                    value = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                                )
                            }
                            result
                        },
                        onMediaPermissionRequested = { _, video, audio, callback ->
                            val mediaPermissions = buildList {
                                if (!video.isNullOrEmpty()) add(Manifest.permission.CAMERA)
                                if (!audio.isNullOrEmpty()) add(Manifest.permission.RECORD_AUDIO)
                            }.toTypedArray()
                            requestAndroidPermissions(mediaPermissions) { granted ->
                                if (!granted) {
                                    callback.reject()
                                } else {
                                    callback.grant(video?.firstOrNull(), audio?.firstOrNull())
                                }
                            }
                        },
                        onFilePromptRequested = { prompt ->
                            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                            val request = buildFilePromptRequest(context, prompt)
                            if (request == null) {
                                result.complete(prompt.dismiss())
                            } else {
                                pendingFilePrompt = PendingFilePromptRequest(
                                    prompt = prompt,
                                    result = result,
                                    captureOutputUri = request.captureOutputUri,
                                    persistable = request.persistable
                                )
                                fileChooserLauncher.launch(request.intent)
                            }
                            result
                        },
                        onFolderUploadPromptRequested = { prompt ->
                            GeckoResult<GeckoSession.PromptDelegate.PromptResponse>().apply {
                                complete(prompt.confirm(AllowOrDeny.ALLOW))
                            }
                        },
                        onPopupPromptRequested = { prompt ->
                            GeckoResult<GeckoSession.PromptDelegate.PromptResponse>().apply {
                                complete(prompt.confirm(AllowOrDeny.ALLOW))
                            }
                        },
                        onSharePromptRequested = { prompt ->
                            GeckoResult<GeckoSession.PromptDelegate.PromptResponse>().apply {
                                val launched = launchSharePrompt(context, prompt)
                                complete(
                                    if (launched) {
                                        prompt.confirm(GeckoSession.PromptDelegate.SharePrompt.Result.SUCCESS)
                                    } else {
                                        prompt.confirm(GeckoSession.PromptDelegate.SharePrompt.Result.FAILURE)
                                    }
                                )
                            }
                        },
                        onAlertPromptRequested = { prompt ->
                            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                            pendingAlertPromptDialog = PendingAlertPromptDialog(
                                title = prompt.title,
                                message = prompt.message,
                                onDismiss = { result.complete(prompt.dismiss()) }
                            )
                            result
                        },
                        onButtonPromptRequested = { prompt ->
                            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                            pendingButtonPromptDialog = PendingButtonPromptDialog(
                                title = prompt.title,
                                message = prompt.message,
                                onConfirm = {
                                    result.complete(
                                        prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.POSITIVE)
                                    )
                                },
                                onDismiss = {
                                    result.complete(
                                        prompt.confirm(GeckoSession.PromptDelegate.ButtonPrompt.Type.NEGATIVE)
                                    )
                                }
                            )
                            result
                        },
                        onTextPromptRequested = { prompt ->
                            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                            pendingTextPromptDialog = PendingTextPromptDialog(
                                title = prompt.title,
                                message = prompt.message,
                                defaultValue = prompt.defaultValue,
                                onConfirm = { value -> result.complete(prompt.confirm(value)) },
                                onDismiss = { result.complete(prompt.dismiss()) }
                            )
                            result
                        },
                        onAuthPromptRequested = { prompt ->
                            val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                            pendingAuthPromptDialog = PendingAuthPromptDialog(
                                title = prompt.title,
                                message = prompt.message,
                                defaultUsername = prompt.authOptions.username,
                                defaultPassword = prompt.authOptions.password,
                                passwordOnly = prompt.authOptions.flags and
                                    GeckoSession.PromptDelegate.AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD != 0,
                                onConfirm = { username, password ->
                                    result.complete(
                                        if (
                                            prompt.authOptions.flags and
                                            GeckoSession.PromptDelegate.AuthPrompt.AuthOptions.Flags.ONLY_PASSWORD != 0
                                        ) {
                                            prompt.confirm(password)
                                        } else {
                                            prompt.confirm(username, password)
                                        }
                                    )
                                },
                                onDismiss = { result.complete(prompt.dismiss()) }
                            )
                            result
                        }
                    )
                    geckoSessionRef = session
                    GeckoView(context).apply {
                        setSession(session)
                        geckoViewRef = this
                    }
                },
                update = { _ -> },
                onRelease = { geckoView ->
                    geckoView.releaseSession()
                    geckoViewRef = null
                    geckoSessionRef?.apply {
                        setFocused(false)
                        setActive(false)
                        close()
                    }
                    geckoSessionRef = null
                }
            )
            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(animationSpec = tween(durationMillis = 150)),
                exit = fadeOut(animationSpec = tween(durationMillis = 220))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = if (effectiveShowTopBar) 18.dp else statusBarHeight + 18.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 10.dp
                    ) {
                        Text(
                            text = "Loading",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    pendingAlertPromptDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                pendingAlertPromptDialog = null
                dialog.onDismiss()
            },
            title = {
                if (!dialog.title.isNullOrBlank()) {
                    Text(dialog.title)
                }
            },
            text = {
                if (!dialog.message.isNullOrBlank()) {
                    Text(dialog.message)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAlertPromptDialog = null
                    dialog.onDismiss()
                }) {
                    Text("OK")
                }
            }
        )
    }

    pendingButtonPromptDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = {
                pendingButtonPromptDialog = null
                dialog.onDismiss()
            },
            title = {
                if (!dialog.title.isNullOrBlank()) {
                    Text(dialog.title)
                }
            },
            text = {
                if (!dialog.message.isNullOrBlank()) {
                    Text(dialog.message)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingButtonPromptDialog = null
                    dialog.onConfirm()
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingButtonPromptDialog = null
                    dialog.onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingTextPromptDialog?.let { dialog ->
        var textValue by remember(dialog) { mutableStateOf(dialog.defaultValue.orEmpty()) }
        AlertDialog(
            onDismissRequest = {
                pendingTextPromptDialog = null
                dialog.onDismiss()
            },
            title = {
                if (!dialog.title.isNullOrBlank()) {
                    Text(dialog.title)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!dialog.message.isNullOrBlank()) {
                        Text(dialog.message)
                    }
                    FluentTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = "Value",
                        placeholder = dialog.defaultValue.orEmpty()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingTextPromptDialog = null
                    dialog.onConfirm(textValue)
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingTextPromptDialog = null
                    dialog.onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingAuthPromptDialog?.let { dialog ->
        var username by remember(dialog) { mutableStateOf(dialog.defaultUsername.orEmpty()) }
        var password by remember(dialog) { mutableStateOf(dialog.defaultPassword.orEmpty()) }
        AlertDialog(
            onDismissRequest = {
                pendingAuthPromptDialog = null
                dialog.onDismiss()
            },
            title = {
                if (!dialog.title.isNullOrBlank()) {
                    Text(dialog.title)
                } else {
                    Text("Authentication Required")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!dialog.message.isNullOrBlank()) {
                        Text(dialog.message)
                    }
                    if (!dialog.passwordOnly) {
                        FluentTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = "Username",
                            placeholder = "Enter username"
                        )
                    }
                    FluentTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Enter password"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingAuthPromptDialog = null
                    dialog.onConfirm(username, password)
                }) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingAuthPromptDialog = null
                    dialog.onDismiss()
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HeroPanel(siteCount: Int) {
    val appName = stringResource(R.string.app_name)
    GlassPanel {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CountPill(text = "Fluent web app workspace")
            }
        }
        Text(
            text = "Turn PWAs into focused apps",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Save multiple websites, launch them in a cleaner shell, and pin desktop icons for direct access.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountPill(text = "$siteCount saved")
            CountPill(text = "Shortcut-ready")
        }
    }
}

@Composable
private fun SiteCard(
    site: SavedSite,
    onOpenClicked: () -> Unit,
    onCreateShortcutClicked: () -> Unit,
    onClearSessionClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onSiteIconResolved: (String) -> Unit
) {
    val resolvedIconUrl by produceState<String?>(initialValue = site.iconUrl, site.id, site.url, site.iconUrl) {
        value = site.iconUrl ?: resolveSiteIconUrl(site.url)
    }

    LaunchedEffect(resolvedIconUrl) {
        val iconUrl = resolvedIconUrl ?: return@LaunchedEffect
        if (site.iconUrl != iconUrl) onSiteIconResolved(iconUrl)
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SiteIcon(
                site = site,
                iconUrl = resolvedIconUrl
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CountPill(text = defaultNameForUrl(site.url))
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = site.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onOpenClicked,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Open")
            }
        }
        FilledTonalButton(
            onClick = onCreateShortcutClicked,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Shortcut")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = onClearSessionClicked,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Clear session")
            }
            TextButton(onClick = onDeleteClicked) {
                Text("Delete")
            }
        }
    }
}

@Composable
private fun SiteIcon(
    site: SavedSite,
    iconUrl: String?
) {
    Surface(
        modifier = Modifier.size(58.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f))
    ) {
        if (!iconUrl.isNullOrBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(iconUrl)
                    .build(),
                contentDescription = "${site.name} icon",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(9.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = site.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyStatePanel() {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "No apps yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Add your first PWA above to build a cleaner launcher icon and a reusable web app entry.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountBadge(siteCount: Int) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = "$siteCount",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun CountPill(text: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun FluentReveal(
    visible: Boolean,
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 520,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 620,
                delayMillis = delayMillis,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { it / 7 }
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 160))
    ) {
        content()
    }
}

@Composable
private fun FluentTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        singleLine = true,
        isError = isError,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            errorContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(28.dp)
    Card(
        modifier = modifier
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            )
            .animateContentSize(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.9f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun FluentBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-64).dp, y = (-18).dp)
                .blur(42.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = 210.dp, y = 110.dp)
                .blur(48.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                        )
                    )
                )
        )
    }
}

@Composable
@Suppress("DEPRECATION")
private fun SystemBarsEffect(
    statusBarColor: Color,
    navigationBarColor: Color,
    immersive: Boolean = false
) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return

    SideEffect {
        val window = activity.window
        window.statusBarColor = statusBarColor.toArgb()
        window.navigationBarColor = navigationBarColor.toArgb()
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = statusBarColor.luminance() > 0.5f
        controller.isAppearanceLightNavigationBars = navigationBarColor.luminance() > 0.5f
        if (immersive) {
            controller.systemBarsBehavior =
                androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is android.content.ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private object GeckoRuntimeHolder {
    @Volatile
    private var runtime: GeckoRuntime? = null

    fun get(context: Context): GeckoRuntime {
        return runtime ?: synchronized(this) {
            runtime ?: GeckoRuntime.create(context.applicationContext).also { runtime = it }
        }
    }
}

@Composable
private fun SiteActionConfirmDialog(
    dialog: PendingSiteActionDialog,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = when (dialog.action) {
        SiteAction.DeleteSite -> "Delete app?"
        SiteAction.ClearSession -> "Clear session?"
    }
    val message = when (dialog.action) {
        SiteAction.DeleteSite ->
            "Remove ${dialog.site.name} from your saved apps? This will not clear its session data."

        SiteAction.ClearSession ->
            "Clear cookies and local session data for ${dialog.site.name}? You may need to sign in again."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun buildBrowserSessionSettings(initialUrl: String): GeckoSessionSettings {
    return GeckoSessionSettings.Builder()
        .usePrivateMode(false)
        .contextId(buildSiteSessionContextId(initialUrl))
        .build()
}

private fun buildSiteSessionContextId(initialUrl: String): String {
    val normalizedUrl = normalizeUrl(initialUrl) ?: return DEFAULT_GECKO_SESSION_CONTEXT_ID
    val origin = originForUrl(normalizedUrl)?.lowercase(Locale.ROOT) ?: return DEFAULT_GECKO_SESSION_CONTEXT_ID
    val sanitizedOrigin = buildString(origin.length) {
        origin.forEach { ch ->
            append(if (ch.isLetterOrDigit()) ch else '_')
        }
    }.trim('_')
    return if (sanitizedOrigin.isBlank()) {
        DEFAULT_GECKO_SESSION_CONTEXT_ID
    } else {
        "${GECKO_SESSION_CONTEXT_PREFIX}_$sanitizedOrigin"
    }
}

private inline fun <reified T : android.os.Parcelable> Intent.parcelableExtra(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
}

private fun hasAppPermission(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(context, permission) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun completeContentPermissionRequest(
    runtime: GeckoRuntime,
    permission: GeckoSession.PermissionDelegate.ContentPermission,
    result: GeckoResult<Int>,
    value: Int
) {
    runCatching {
        runtime.storageController.setPermission(permission, value)
    }
    result.complete(value)
}

private fun resolveNotificationLaunchUrl(sourceUrl: String?): String? {
    return sourceUrl
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::normalizeUrl)
}

private fun launchMainActivity(context: Context, launchUrl: String?) {
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            launchUrl?.let { putExtra(EXTRA_SHORTCUT_URL, it) }
        }
    )
}

private suspend fun clearSiteSession(context: Context, site: SavedSite): Boolean {
    val contextId = buildSiteSessionContextId(site.url)
    return withContext(Dispatchers.IO) {
        runCatching {
            GeckoRuntimeHolder.get(context).storageController.clearDataForSessionContext(contextId)
        }.isSuccess
    }
}

@SuppressLint("MissingPermission")
private fun showWebNotification(context: Context, notification: WebNotification) {
    if (
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        notification.dismiss()
        return
    }
    ensureWebNotificationChannel(context)
    val notificationId = notification.tag.hashCode()
    val title = notification.title?.takeIf { it.isNotBlank() }
        ?: notification.source?.let(::defaultNameForUrl)
        ?: "Website notification"
    val text = notification.text?.takeIf { it.isNotBlank() }
        ?: notification.source.orEmpty()
    val launchUrl = resolveNotificationLaunchUrl(notification.source)

    val contentIntent = PendingIntent.getBroadcast(
        context,
        notificationId,
        Intent(context, WebNotificationReceiver::class.java).apply {
            action = ACTION_WEB_NOTIFICATION_CLICK
            putExtra(EXTRA_WEB_NOTIFICATION, notification)
            putExtra(EXTRA_WEB_NOTIFICATION_ID, notificationId)
            launchUrl?.let { putExtra(EXTRA_SHORTCUT_URL, it) }
        },
        PENDING_INTENT_FLAGS
    )
    val deleteIntent = PendingIntent.getBroadcast(
        context,
        notificationId + 1,
        Intent(context, WebNotificationReceiver::class.java).apply {
            action = ACTION_WEB_NOTIFICATION_DISMISS
            putExtra(EXTRA_WEB_NOTIFICATION, notification)
            putExtra(EXTRA_WEB_NOTIFICATION_ID, notificationId)
            launchUrl?.let { putExtra(EXTRA_SHORTCUT_URL, it) }
        },
        PENDING_INTENT_FLAGS
    )

    val builder = NotificationCompat.Builder(context, WEB_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(text)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .setDeleteIntent(deleteIntent)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)

    if (notification.silent) {
        builder.setSilent(true)
    } else if (notification.vibrate.isNotEmpty()) {
        builder.setVibrate(notification.vibrate.map { it.toLong() }.toLongArray())
    } else {
        builder.setDefaults(NotificationCompat.DEFAULT_ALL)
    }

    notification.actions.take(MAX_NOTIFICATION_ACTIONS).forEachIndexed { index, action ->
        val actionIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2 + index,
            Intent(context, WebNotificationReceiver::class.java).apply {
                this.action = ACTION_WEB_NOTIFICATION_CLICK
                putExtra(EXTRA_WEB_NOTIFICATION, notification)
                putExtra(EXTRA_WEB_NOTIFICATION_ID, notificationId)
                putExtra(EXTRA_WEB_NOTIFICATION_ACTION, action.name)
                launchUrl?.let { putExtra(EXTRA_SHORTCUT_URL, it) }
            },
            PENDING_INTENT_FLAGS
        )
        builder.addAction(0, action.title, actionIntent)
    }

    runCatching {
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }.onFailure { error ->
        if (error is SecurityException) {
            notification.dismiss()
            return
        }
        throw error
    }
    notification.show()
}

private fun ensureWebNotificationChannel(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return
    val existingChannel = manager.getNotificationChannel(WEB_NOTIFICATION_CHANNEL_ID)
    if (existingChannel != null) return
    manager.createNotificationChannel(
        NotificationChannel(
            WEB_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.web_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.web_notification_channel_description)
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
    )
}

private fun launchSharePrompt(
    context: Context,
    prompt: GeckoSession.PromptDelegate.SharePrompt
): Boolean {
    val sharePayload = buildString {
        prompt.text?.trim()?.takeIf { it.isNotEmpty() }?.let { append(it) }
        prompt.uri?.trim()?.takeIf { it.isNotEmpty() }?.let { uri ->
            if (isNotEmpty()) append('\n')
            append(uri)
        }
    }.trim()
    if (sharePayload.isBlank()) return false

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, sharePayload)
    }
    return launchIntent(
        context,
        Intent.createChooser(shareIntent, prompt.title?.takeIf { it.isNotBlank() } ?: "Share")
    )
}

private fun sampleBrowserChromeColor(bitmap: Bitmap): Color? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null

    val sampleHeight = max(8, min(bitmap.height / 6, 96))
    val stepX = max(1, bitmap.width / 36)
    val stepY = max(1, sampleHeight / 10)
    var redTotal = 0L
    var greenTotal = 0L
    var blueTotal = 0L
    var sampleCount = 0L

    for (y in 0 until sampleHeight step stepY) {
        for (x in 0 until bitmap.width step stepX) {
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 200) continue
            redTotal += android.graphics.Color.red(pixel)
            greenTotal += android.graphics.Color.green(pixel)
            blueTotal += android.graphics.Color.blue(pixel)
            sampleCount += 1
        }
    }

    if (sampleCount == 0L) return null
    return Color(
        red = (redTotal / sampleCount).toInt() / 255f,
        green = (greenTotal / sampleCount).toInt() / 255f,
        blue = (blueTotal / sampleCount).toInt() / 255f
    )
}

private fun buildFilePromptRequest(
    context: Context,
    prompt: GeckoSession.PromptDelegate.FilePrompt
): BuiltFilePromptRequest? {
    if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.SINGLE &&
        prompt.capture != GeckoSession.PromptDelegate.FilePrompt.Capture.NONE
    ) {
        buildCaptureIntent(context, prompt)?.let { return it }
    }

    if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
        return BuiltFilePromptRequest(
            intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            },
            captureOutputUri = null,
            persistable = true
        )
    }

    val mimeTypes = sanitizeMimeTypes(prompt.mimeTypes)
    val baseIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = mimeTypes.firstOrNull() ?: "*/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE)
        if (mimeTypes.size > 1 || type == "*/*") {
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }
    return BuiltFilePromptRequest(
        intent = baseIntent,
        captureOutputUri = null,
        persistable = true
    )
}

private fun buildCaptureIntent(
    context: Context,
    prompt: GeckoSession.PromptDelegate.FilePrompt
): BuiltFilePromptRequest? {
    val mimeTypes = sanitizeMimeTypes(prompt.mimeTypes)
    val acceptsImage = mimeTypes.any { it.startsWith("image/") || it == "*/*" }
    val acceptsVideo = mimeTypes.any { it.startsWith("video/") }
    val acceptsAudio = mimeTypes.any { it.startsWith("audio/") }

    return when {
        acceptsImage -> {
            val outputUri = createCaptureOutputUri(context, "jpg") ?: return null
            BuiltFilePromptRequest(
                intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                captureOutputUri = outputUri,
                persistable = false
            )
        }

        acceptsVideo -> {
            val outputUri = createCaptureOutputUri(context, "mp4") ?: return null
            BuiltFilePromptRequest(
                intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
                    addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                captureOutputUri = outputUri,
                persistable = false
            )
        }

        acceptsAudio -> BuiltFilePromptRequest(
            intent = Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION),
            captureOutputUri = null,
            persistable = false
        )

        else -> null
    }
}

private fun createCaptureOutputUri(context: Context, extension: String): Uri? {
    return runCatching {
        val cacheDir = File(context.cacheDir, "browser")
        cacheDir.mkdirs()
        val file = File.createTempFile("capture_", ".$extension", cacheDir)
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

private fun sanitizeMimeTypes(mimeTypes: Array<String>?): Array<String> {
    val filtered = mimeTypes
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        .orEmpty()
    return if (filtered.isEmpty()) arrayOf("*/*") else filtered.toTypedArray()
}

private fun completePendingFilePrompt(
    context: Context,
    request: PendingFilePromptRequest,
    resultCode: Int,
    data: Intent?
) {
    if (resultCode != Activity.RESULT_OK) {
        request.result.complete(request.prompt.dismiss())
        return
    }

    val uris = extractFilePromptUris(data, request.captureOutputUri)
    if (uris.isEmpty()) {
        request.result.complete(request.prompt.dismiss())
        return
    }

    if (request.persistable) {
        var takeFlags = 0
        if (data?.flags?.and(Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
            takeFlags = takeFlags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (data?.flags?.and(Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
            takeFlags = takeFlags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        if (takeFlags != 0) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                }
            }
        }
    }

    val response = if (
        request.prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE &&
        uris.size > 1
    ) {
        request.prompt.confirm(context, uris.toTypedArray())
    } else {
        request.prompt.confirm(context, uris.first())
    }
    request.result.complete(response)
}

private fun extractFilePromptUris(data: Intent?, captureOutputUri: Uri?): List<Uri> {
    val clipData = data?.clipData
    if (clipData != null && clipData.itemCount > 0) {
        return buildList {
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index).uri?.let(::add)
            }
        }
    }
    data?.data?.let { return listOf(it) }
    captureOutputUri?.let { return listOf(it) }
    return emptyList()
}

private suspend fun saveExternalResponse(
    context: Context,
    response: WebResponse
): SavedDownload? = withContext(Dispatchers.IO) {
    val appName = context.getString(R.string.app_name)
    val inputStream = response.body ?: return@withContext null
    val contentDisposition = headerValue(response.headers, "content-disposition")
    val mimeType = headerValue(response.headers, "content-type")
        ?.substringBefore(';')
        ?.trim()
        .orEmpty()
    val fileName = URLUtil.guessFileName(response.uri, contentDisposition, mimeType)
        .ifBlank { "download" }

    inputStream.use { stream ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/$appName"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(collection, values) ?: return@withContext null
            context.contentResolver.openOutputStream(uri)?.use { output ->
                stream.copyTo(output)
            } ?: return@withContext null
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            return@withContext SavedDownload(
                uri = uri,
                fileName = fileName,
                mimeType = mimeType.ifBlank { null },
                openAfterSave = response.requestExternalApp
            )
        }

        val downloadDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            appName
        ).apply { mkdirs() }
        val outputFile = uniqueFile(downloadDir, fileName)
        outputFile.outputStream().use { output ->
            stream.copyTo(output)
        }
        val fileUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )
        SavedDownload(
            uri = fileUri,
            fileName = outputFile.name,
            mimeType = mimeType.ifBlank { null },
            openAfterSave = response.requestExternalApp
        )
    }
}

private fun openDownloadedFile(
    context: Context,
    fileUri: Uri,
    mimeType: String?
): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(fileUri, mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return launchIntent(context, intent)
}

private fun uniqueFile(directory: File, fileName: String): File {
    val cleanName = fileName.ifBlank { "download" }
    val baseName = cleanName.substringBeforeLast('.', cleanName)
    val extension = cleanName.substringAfterLast('.', "")
    var candidate = File(directory, cleanName)
    var index = 1
    while (candidate.exists()) {
        val suffix = " ($index)"
        val resolvedName = if (extension.isBlank()) {
            baseName + suffix
        } else {
            "$baseName$suffix.$extension"
        }
        candidate = File(directory, resolvedName)
        index += 1
    }
    return candidate
}

private fun headerValue(headers: Map<String, String>, name: String): String? {
    return headers.entries.firstOrNull { (key, _) ->
        key.equals(name, ignoreCase = true)
    }?.value
}

private fun createAppGeckoSession(
    context: Context,
    runtime: GeckoRuntime,
    initialUrl: String,
    onCanGoBackChanged: (Boolean) -> Unit,
    onPageTitleChanged: (String?) -> Unit,
    onPageUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onManifestThemeColorResolved: (Color?) -> Unit,
    onCloseRequested: () -> Unit,
    onFullScreenChanged: (Boolean) -> Unit,
    onExternalResponse: (WebResponse) -> Unit,
    onAndroidPermissionsRequested: (
        Array<String>?,
        GeckoSession.PermissionDelegate.Callback
    ) -> Unit,
    onContentPermissionRequested: (
        GeckoSession.PermissionDelegate.ContentPermission
    ) -> GeckoResult<Int>,
    onMediaPermissionRequested: (
        String,
        Array<GeckoSession.PermissionDelegate.MediaSource>?,
        Array<GeckoSession.PermissionDelegate.MediaSource>?,
        GeckoSession.PermissionDelegate.MediaCallback
    ) -> Unit,
    onFilePromptRequested: (
        GeckoSession.PromptDelegate.FilePrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onFolderUploadPromptRequested: (
        GeckoSession.PromptDelegate.FolderUploadPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onPopupPromptRequested: (
        GeckoSession.PromptDelegate.PopupPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onSharePromptRequested: (
        GeckoSession.PromptDelegate.SharePrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onAlertPromptRequested: (
        GeckoSession.PromptDelegate.AlertPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onButtonPromptRequested: (
        GeckoSession.PromptDelegate.ButtonPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onTextPromptRequested: (
        GeckoSession.PromptDelegate.TextPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onAuthPromptRequested: (
        GeckoSession.PromptDelegate.AuthPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>
): GeckoSession {
    return GeckoSession(buildBrowserSessionSettings(initialUrl)).apply {
        contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                onPageTitleChanged(title)
            }

            override fun onWebAppManifest(session: GeckoSession, manifest: JSONObject) {
                val parsedColor = parseCssColor(
                    manifest.optString("theme_color").trim().ifBlank {
                        manifest.optString("background_color").trim()
                    }
                )
                onManifestThemeColorResolved(parsedColor)
            }

            override fun onCloseRequest(session: GeckoSession) {
                onCloseRequested()
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                onFullScreenChanged(fullScreen)
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                onExternalResponse(response)
            }
        }
        progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                onLoadingChanged(true)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                onLoadingChanged(false)
            }
        }
        navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                onCanGoBackChanged(canGoBack)
            }

            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                if (!url.isNullOrBlank()) {
                    onPageUrlChanged(url)
                }
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                val targetUri = runCatching { Uri.parse(request.uri) }.getOrNull() ?: return null
                if (
                    request.target == GeckoSession.NavigationDelegate.TARGET_WINDOW_NEW &&
                    targetUri.scheme?.lowercase(Locale.ROOT) in INTERNAL_BROWSER_SCHEMES
                ) {
                    session.loadUri(request.uri)
                    return GeckoResult.deny()
                }
                return if (
                    handleAppNavigationRequest(context, targetUri) { fallbackUrl ->
                        session.loadUri(fallbackUrl)
                    }
                ) {
                    GeckoResult.deny()
                } else {
                    null
                }
            }

            override fun onSubframeLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                val targetUri = runCatching { Uri.parse(request.uri) }.getOrNull() ?: return null
                return if (
                    handleAppNavigationRequest(context, targetUri) { fallbackUrl ->
                        session.loadUri(fallbackUrl)
                    }
                ) {
                    GeckoResult.deny()
                } else {
                    null
                }
            }
        }
        permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onAndroidPermissionsRequest(
                session: GeckoSession,
                permissions: Array<String>?,
                callback: GeckoSession.PermissionDelegate.Callback
            ) {
                onAndroidPermissionsRequested(permissions, callback)
            }

            override fun onContentPermissionRequest(
                session: GeckoSession,
                perm: GeckoSession.PermissionDelegate.ContentPermission
            ): GeckoResult<Int> {
                return onContentPermissionRequested(perm)
            }

            override fun onMediaPermissionRequest(
                session: GeckoSession,
                uri: String,
                video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
                callback: GeckoSession.PermissionDelegate.MediaCallback
            ) {
                onMediaPermissionRequested(uri, video, audio, callback)
            }
        }
        promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onFilePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FilePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onFilePromptRequested(prompt)
            }

            override fun onFolderUploadPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FolderUploadPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onFolderUploadPromptRequested(prompt)
            }

            override fun onPopupPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.PopupPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onPopupPromptRequested(prompt)
            }

            override fun onSharePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.SharePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onSharePromptRequested(prompt)
            }

            override fun onAlertPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onAlertPromptRequested(prompt)
            }

            override fun onButtonPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ButtonPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onButtonPromptRequested(prompt)
            }

            override fun onTextPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.TextPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onTextPromptRequested(prompt)
            }

            override fun onAuthPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AuthPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onAuthPromptRequested(prompt)
            }
        }
        open(runtime)
        setActive(true)
        setFocused(true)
        setPriorityHint(GeckoSession.PRIORITY_HIGH)
        loadUri(initialUrl)
    }
}

private suspend fun resolveThemeColorForUrl(url: String): Color? = withContext(Dispatchers.IO) {
    val normalizedUrl = normalizeUrl(url) ?: return@withContext null
    val html = fetchText(normalizedUrl) ?: return@withContext null
    extractHtmlThemeColor(html)?.let(::parseCssColor)
        ?: extractManifestUrl(html, normalizedUrl)?.let(::extractManifestThemeColor)?.let(::parseCssColor)
}

private fun extractHtmlThemeColor(html: String): String? {
    return META_TAG_REGEX.findAll(html).firstNotNullOfOrNull { match ->
        val tag = match.value
        val name = extractHtmlAttribute(tag, "name")?.lowercase(Locale.ROOT) ?: return@firstNotNullOfOrNull null
        if (name != "theme-color") return@firstNotNullOfOrNull null
        extractHtmlAttribute(tag, "content")
    }
}

private fun extractManifestThemeColor(manifestUrl: String): String? {
    val manifest = fetchText(manifestUrl) ?: return null
    return runCatching {
        JSONObject(manifest).let { manifestJson ->
            manifestJson.optString("theme_color").trim().ifBlank {
                manifestJson.optString("background_color").trim()
            }.ifBlank { null }
        }
    }.getOrNull()
}

private fun handleAppNavigationRequest(
    context: Context,
    uri: Uri,
    onFallbackUrl: (String) -> Unit
): Boolean {
    val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
    return when (scheme) {
        in INTERNAL_BROWSER_SCHEMES -> false
        "intent" -> handleIntentScheme(context, uri.toString(), onFallbackUrl)
        else -> launchExternalUri(context, uri)
    }
}

private fun handleIntentScheme(
    context: Context,
    uriString: String,
    onFallbackUrl: (String) -> Unit
): Boolean {
    val intent = runCatching {
        Intent.parseUri(uriString, Intent.URI_INTENT_SCHEME)
    }.getOrNull() ?: return false

    val fallbackUrl = intent.getStringExtra("browser_fallback_url")
    intent.addCategory(Intent.CATEGORY_BROWSABLE)
    intent.component = null
    intent.selector = null

    if (launchIntent(context, intent)) return true

    if (!fallbackUrl.isNullOrBlank()) {
        onFallbackUrl(fallbackUrl)
        return true
    }

    return false
}

private fun launchExternalUri(context: Context, uri: Uri): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    return launchIntent(context, intent)
}

private fun launchIntent(context: Context, intent: Intent): Boolean {
    return runCatching {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

private fun parseCssColor(rawValue: String?): Color? {
    val colorString = rawValue?.trim()?.trim('"', '\'').orEmpty()
    if (colorString.isBlank()) return null

    RGB_COLOR_REGEX.matchEntire(colorString)?.let { match ->
        val red = match.groupValues[1].toIntOrNull() ?: return null
        val green = match.groupValues[2].toIntOrNull() ?: return null
        val blue = match.groupValues[3].toIntOrNull() ?: return null
        val alpha = match.groupValues.getOrNull(4)?.toFloatOrNull() ?: 1f
        val parsed = Color(red / 255f, green / 255f, blue / 255f, alpha.coerceIn(0f, 1f))
        return parsed.takeIf { it.alpha > 0.2f }
    }

    return runCatching {
        Color(android.graphics.Color.parseColor(colorString))
    }.getOrNull()
}

private fun normalizeUrl(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
    val uri = runCatching { Uri.parse(withScheme) }.getOrNull() ?: return null
    if (uri.scheme.isNullOrBlank() || uri.host.isNullOrBlank()) return null
    return uri.toString()
}

private suspend fun resolveSiteIconUrl(url: String): String? = withContext(Dispatchers.IO) {
    val normalizedUrl = normalizeUrl(url) ?: return@withContext null
    val html = fetchText(normalizedUrl)
    val manifestUrl = html?.let { extractManifestUrl(it, normalizedUrl) }
    val candidates = buildList {
        if (html != null) addAll(extractHtmlIconUrls(html, normalizedUrl))
        if (manifestUrl != null) addAll(extractManifestIconUrls(manifestUrl))
        addAll(commonIconCandidates(normalizedUrl))
    }.distinct()

    candidates.firstOrNull { isReachableIcon(it) }
}

private fun extractHtmlIconUrls(html: String, pageUrl: String): List<String> {
    return LINK_TAG_REGEX.findAll(html).mapNotNull { match ->
        val tag = match.value
        val rel = extractHtmlAttribute(tag, "rel")?.lowercase(Locale.ROOT) ?: return@mapNotNull null
        if (!rel.contains("icon")) return@mapNotNull null
        val href = extractHtmlAttribute(tag, "href") ?: return@mapNotNull null
        resolveRelativeUrl(pageUrl, href)
    }.toList()
}

private fun extractManifestUrl(html: String, pageUrl: String): String? {
    return LINK_TAG_REGEX.findAll(html).mapNotNull { match ->
        val tag = match.value
        val rel = extractHtmlAttribute(tag, "rel")?.lowercase(Locale.ROOT) ?: return@mapNotNull null
        if (!rel.contains("manifest")) return@mapNotNull null
        val href = extractHtmlAttribute(tag, "href") ?: return@mapNotNull null
        resolveRelativeUrl(pageUrl, href)
    }.firstOrNull()
}

private fun extractManifestIconUrls(manifestUrl: String): List<String> {
    val manifest = fetchText(manifestUrl) ?: return emptyList()
    val icons = runCatching {
        val manifestJson = JSONObject(manifest)
        val iconArray = manifestJson.optJSONArray("icons") ?: JSONArray()
        buildList {
            for (i in 0 until iconArray.length()) {
                val icon = iconArray.optJSONObject(i) ?: continue
                val src = icon.optString("src").trim()
                if (src.isBlank()) continue
                add(
                    ManifestIcon(
                        src = resolveRelativeUrl(manifestUrl, src) ?: continue,
                        sizes = icon.optString("sizes")
                    )
                )
            }
        }.sortedByDescending { it.maxSize }
            .map { it.src }
    }.getOrDefault(emptyList())
    return icons
}

private fun commonIconCandidates(url: String): List<String> {
    val origin = originForUrl(url) ?: return emptyList()
    return listOf(
        "$origin/favicon-192x192.png",
        "$origin/favicon-96x96.png",
        "$origin/favicon.png",
        "$origin/apple-touch-icon.png",
        "$origin/apple-touch-icon-precomposed.png",
        "$origin/favicon.svg",
        "$origin/favicon.webp"
    )
}

private fun extractHtmlAttribute(tag: String, attribute: String): String? {
    val regex = Pattern.compile(
        "$attribute\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
        Pattern.CASE_INSENSITIVE
    )
    val match = regex.matcher(tag)
    if (!match.find()) return null
    return (1..3).firstNotNullOfOrNull { index -> match.group(index) }?.trim()
}

private fun resolveRelativeUrl(baseUrl: String, candidate: String): String? {
    val trimmed = candidate.trim()
    if (trimmed.isBlank()) return null
    return runCatching { URI(baseUrl).resolve(trimmed).toString() }.getOrNull()
}

private fun fetchText(url: String): String? {
    val connection = openConnection(url) ?: return null
    return runCatching {
        connection.inputStream.bufferedReader().use { it.readText() }
    }.getOrNull()
}

private fun isReachableIcon(url: String): Boolean {
    if (!looksLikeImageUrl(url)) return false
    return isImageResponse(openConnection(url, requestMethod = "HEAD")) ||
        isImageResponse(openConnection(url, requestMethod = "GET"))
}

private fun isImageResponse(connection: HttpURLConnection?): Boolean {
    connection ?: return false
    return runCatching {
        val code = connection.responseCode
        val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
        code in 200..299 && contentType.startsWith("image/")
    }.getOrDefault(false)
}

private fun openConnection(
    url: String,
    requestMethod: String = "GET"
): HttpURLConnection? {
    return runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            this.requestMethod = requestMethod
            setRequestProperty("User-Agent", APP_USER_AGENT)
            setRequestProperty("Accept", "text/html,application/json,image/*,*/*")
            setRequestProperty("Cache-Control", "no-cache")
            doInput = true
            connect()
        }
    }.getOrNull()
}

private fun looksLikeImageUrl(url: String): Boolean {
    val lowercaseUrl = url.lowercase(Locale.ROOT)
    return lowercaseUrl.endsWith(".png") ||
        lowercaseUrl.endsWith(".webp") ||
        lowercaseUrl.endsWith(".jpg") ||
        lowercaseUrl.endsWith(".jpeg") ||
        lowercaseUrl.endsWith(".svg")
}

private fun defaultNameForUrl(url: String): String {
    val host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
    if (host.isBlank()) return url
    return host.removePrefix("www.")
}

private fun siteIdFromUrl(url: String): String = url.lowercase(Locale.ROOT)

private fun originForUrl(url: String): String? {
    val uri = Uri.parse(url)
    val scheme = uri.scheme ?: return null
    val host = uri.host ?: return null
    val port = if (uri.port != -1) ":${uri.port}" else ""
    return "$scheme://$host$port"
}

private fun loadSites(context: Context): List<SavedSite> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SITES, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        buildList {
            for (i in 0 until arr.length()) {
                val entry = arr.opt(i)
                when (entry) {
                    is String -> {
                        val normalized = normalizeUrl(entry) ?: continue
                        add(
                            SavedSite(
                                id = siteIdFromUrl(normalized),
                                name = defaultNameForUrl(normalized),
                                url = normalized,
                                iconUrl = null
                            )
                        )
                    }
                    is JSONObject -> {
                        val normalized = normalizeUrl(entry.optString("url")) ?: continue
                        val name = entry.optString("name").trim().ifBlank {
                            defaultNameForUrl(normalized)
                        }
                        add(
                            SavedSite(
                                id = entry.optString("id").ifBlank { siteIdFromUrl(normalized) },
                                name = name,
                                url = normalized,
                                iconUrl = entry.optString("iconUrl").trim().ifBlank { null }
                            )
                        )
                    }
                }
            }
        }.distinctBy { it.url.lowercase(Locale.ROOT) }
    }.getOrDefault(emptyList())
}

private fun saveSites(context: Context, sites: List<SavedSite>) {
    val json = JSONArray().apply {
        sites.forEach { site ->
            put(
                JSONObject().apply {
                    put("id", site.id)
                    put("name", site.name)
                    put("url", site.url)
                    put("iconUrl", site.iconUrl)
                }
            )
        }
    }.toString()
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit { putString(KEY_SITES, json) }
}

private suspend fun loadShortcutIconBitmap(context: Context, site: SavedSite): Bitmap? {
    val iconUrl = site.iconUrl ?: resolveSiteIconUrl(site.url) ?: return null
    return withContext(Dispatchers.IO) {
        val imageLoader = SingletonImageLoader.get(context)
        val result = imageLoader.execute(
            ImageRequest.Builder(context)
                .data(iconUrl)
                .build()
        )
        val success = result as? SuccessResult ?: return@withContext null
        success.image.asDrawable(context.resources).toBitmap()
    }
}

private fun createPinnedShortcut(
    context: Context,
    site: SavedSite,
    shortcutBitmap: Bitmap?
): Boolean {
    if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return false
    val openIntent = Intent(context, MainActivity::class.java).apply {
        action = Intent.ACTION_VIEW
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_SHORTCUT_URL, site.url)
    }
    val icon = shortcutBitmap?.let { IconCompat.createWithBitmap(it) }
        ?: IconCompat.createWithResource(context, R.mipmap.ic_launcher)
    val shortcut = ShortcutInfoCompat.Builder(context, site.id)
        .setShortLabel(site.name.take(MAX_SHORTCUT_LABEL_LENGTH))
        .setLongLabel(site.name)
        .setIcon(icon)
        .setIntent(openIntent)
        .build()
    return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
}

private data class PendingAndroidPermissionRequest(
    val permissions: List<String>,
    val onCompleted: (Map<String, Boolean>) -> Unit
) {
    fun onResult(grantResults: Map<String, Boolean>) {
        onCompleted(grantResults)
    }
}

private data class PendingIntentSenderRequest(
    val result: GeckoResult<Intent>
)

private data class BuiltFilePromptRequest(
    val intent: Intent,
    val captureOutputUri: Uri?,
    val persistable: Boolean
)

private data class PendingFilePromptRequest(
    val prompt: GeckoSession.PromptDelegate.FilePrompt,
    val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    val captureOutputUri: Uri?,
    val persistable: Boolean
)

private data class PendingAlertPromptDialog(
    val title: String?,
    val message: String?,
    val onDismiss: () -> Unit
)

private data class PendingButtonPromptDialog(
    val title: String?,
    val message: String?,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit
)

private data class PendingTextPromptDialog(
    val title: String?,
    val message: String?,
    val defaultValue: String?,
    val onConfirm: (String) -> Unit,
    val onDismiss: () -> Unit
)

private data class PendingAuthPromptDialog(
    val title: String?,
    val message: String?,
    val defaultUsername: String?,
    val defaultPassword: String?,
    val passwordOnly: Boolean,
    val onConfirm: (String, String) -> Unit,
    val onDismiss: () -> Unit
)

private data class SavedDownload(
    val uri: Uri,
    val fileName: String,
    val mimeType: String?,
    val openAfterSave: Boolean
)

private data class SavedSite(
    val id: String,
    val name: String,
    val url: String,
    val iconUrl: String?
)

private data class PendingSiteActionDialog(
    val site: SavedSite,
    val action: SiteAction
)

private enum class SiteAction {
    DeleteSite,
    ClearSession
}

private data class ManifestIcon(
    val src: String,
    val sizes: String
) {
    val maxSize: Int
        get() = sizes.split(" ")
            .mapNotNull { entry ->
                entry.substringBefore("x").toIntOrNull()
            }
            .maxOrNull() ?: 0
}

private fun transparentSystemBarStyle(): SystemBarStyle {
    return SystemBarStyle.auto(
        android.graphics.Color.TRANSPARENT,
        android.graphics.Color.TRANSPARENT
    ) { resources ->
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }
}

private const val PREFS_NAME = "pwa2app_prefs"
private const val KEY_SITES = "saved_sites"
private const val EXTRA_SHORTCUT_URL = "shortcut_url"
private const val EXTRA_WEB_NOTIFICATION = "web_notification"
private const val EXTRA_WEB_NOTIFICATION_ACTION = "web_notification_action"
private const val EXTRA_WEB_NOTIFICATION_ID = "web_notification_id"
private const val MAX_SHORTCUT_LABEL_LENGTH = 30
private const val MAX_NOTIFICATION_ACTIONS = 3
private const val HTTP_TIMEOUT_MS = 5500
private const val MIN_SPLASH_DURATION_MS = 280L
private const val SPLASH_EXIT_DURATION_MS = 360L
private const val HOME_REVEAL_DELAY_MS = 90L
private const val CHROME_COLOR_SAMPLE_INTERVAL_MS = 900L
private const val APP_USER_AGENT = "PWADock/1.0"
private const val WEB_NOTIFICATION_CHANNEL_ID = "pwa_dock_messages"
private const val GECKO_SESSION_CONTEXT_PREFIX = "site"
private const val DEFAULT_GECKO_SESSION_CONTEXT_ID = "site_default"
private const val ACTION_WEB_NOTIFICATION_CLICK = "com.norns.pwa2app.WEB_NOTIFICATION_CLICK"
private const val ACTION_WEB_NOTIFICATION_DISMISS = "com.norns.pwa2app.WEB_NOTIFICATION_DISMISS"
private const val PENDING_INTENT_FLAGS =
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
private val LINK_TAG_REGEX = Regex("<link\\b[^>]*>", setOf(RegexOption.IGNORE_CASE))
private val META_TAG_REGEX = Regex("<meta\\b[^>]*>", setOf(RegexOption.IGNORE_CASE))
private val RGB_COLOR_REGEX = Regex(
    "rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})(?:\\s*,\\s*([0-9.]+))?\\s*\\)",
    RegexOption.IGNORE_CASE
)
private val INTERNAL_BROWSER_SCHEMES = setOf("http", "https", "about", "javascript", "data", "blob")

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    PWA2APPTheme {
        HomeScreen(
            sites = listOf(
                SavedSite(
                    id = "https://example.com",
                    name = "example.com",
                    url = "https://example.com",
                    iconUrl = null
                )
            ),
            inputName = "Example",
            inputUrl = "example.com",
            errorText = null,
            onNameChanged = {},
            onInputChanged = {},
            onAddClicked = {},
            onOpenClicked = {},
            onCreateShortcutClicked = {},
            onDeleteClicked = {},
            onClearSessionClicked = {},
            onSiteIconResolved = { _, _ -> }
        )
    }
}
