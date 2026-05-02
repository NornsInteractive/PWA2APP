package com.norns.pwa2app

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern

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
                sites = sites.filterNot { it.id == site.id }
                saveSites(context, sites)
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
    } else {
        val displayName = sites.firstOrNull { it.url.equals(currentUrl, ignoreCase = true) }?.name
            ?: defaultNameForUrl(currentUrl!!)
        PwaWebViewScreen(
            title = displayName,
            url = currentUrl!!,
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

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PwaWebViewScreen(
    title: String,
    url: String,
    showTopBar: Boolean,
    onClose: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val defaultChromeColor = MaterialTheme.colorScheme.surface
    var chromeColor by remember(url) { mutableStateOf(defaultChromeColor) }
    val onChromeColor = if (chromeColor.luminance() > 0.5f) Color(0xFF111827) else Color.White
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    SystemBarsEffect(
        statusBarColor = chromeColor,
        navigationBarColor = MaterialTheme.colorScheme.background
    )

    BackHandler {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onClose()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    modifier = Modifier.statusBarsPadding(),
                    title = { Text(title) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = chromeColor,
                        titleContentColor = onChromeColor,
                        navigationIconContentColor = onChromeColor,
                        actionIconContentColor = onChromeColor
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    navigationIcon = {
                        TextButton(onClick = {
                            val webView = webViewRef
                            if (webView != null && webView.canGoBack()) {
                                webView.goBack()
                            } else {
                                onClose()
                            }
                        }) {
                            Text("Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = onClose) { Text("Close") }
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
            if (!showTopBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(statusBarHeight)
                        .background(chromeColor)
                )
            }
            AndroidView(
                modifier = Modifier
                    .padding(top = if (showTopBar) 0.dp else statusBarHeight)
                    .fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(true)
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                super.onPageFinished(view, finishedUrl)
                                view?.captureThemeColor { parsedColor ->
                                    chromeColor = parsedColor ?: defaultChromeColor
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        loadUrl(url)
                        webViewRef = this
                    }
                },
                update = { _ -> },
                onRelease = { webView ->
                    webView.stopLoading()
                    webView.clearHistory()
                    webView.removeAllViews()
                    webView.destroy()
                    webViewRef = null
                }
            )
        }
    }
}

@Composable
private fun HeroPanel(siteCount: Int) {
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
                    text = "PWA Dock",
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
        TextButton(
            onClick = onDeleteClicked,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Delete")
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
    navigationBarColor: Color
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

private fun WebView.captureThemeColor(onColorResolved: (Color?) -> Unit) {
    evaluateJavascript(JS_CAPTURE_THEME_COLOR) { rawResult ->
        onColorResolved(parseJavascriptColor(rawResult))
    }
}

private fun parseJavascriptColor(rawResult: String?): Color? {
    if (rawResult.isNullOrBlank() || rawResult == "null") return null
    val colorString = runCatching {
        JSONArray("[$rawResult]").optString(0)
    }.getOrNull()?.trim().orEmpty()
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

private data class SavedSite(
    val id: String,
    val name: String,
    val url: String,
    val iconUrl: String?
)

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
private const val MAX_SHORTCUT_LABEL_LENGTH = 30
private const val HTTP_TIMEOUT_MS = 5500
private const val MIN_SPLASH_DURATION_MS = 280L
private const val SPLASH_EXIT_DURATION_MS = 360L
private const val HOME_REVEAL_DELAY_MS = 90L
private const val APP_USER_AGENT = "PWA2APP/1.0"
private val LINK_TAG_REGEX = Regex("<link\\b[^>]*>", setOf(RegexOption.IGNORE_CASE))
private val RGB_COLOR_REGEX = Regex(
    "rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})(?:\\s*,\\s*([0-9.]+))?\\s*\\)",
    RegexOption.IGNORE_CASE
)
private const val JS_CAPTURE_THEME_COLOR = """
(function() {
  function pickColor(selector) {
    var node = document.querySelector(selector);
    if (!node) return '';
    return window.getComputedStyle(node).backgroundColor || '';
  }
  function normalized(color) {
    if (!color || color === 'transparent' || color === 'rgba(0, 0, 0, 0)') return '';
    var probe = document.createElement('span');
    probe.style.color = color;
    (document.body || document.documentElement).appendChild(probe);
    var computed = window.getComputedStyle(probe).color || '';
    probe.remove();
    return computed;
  }
  var meta = document.querySelector('meta[name="theme-color"]');
  var candidates = [
    meta ? meta.getAttribute('content') : '',
    pickColor('header'),
    pickColor('[role="banner"]'),
    window.getComputedStyle(document.body || document.documentElement).backgroundColor,
    window.getComputedStyle(document.documentElement).backgroundColor
  ];
  for (var i = 0; i < candidates.length; i++) {
    var color = normalized(candidates[i]);
    if (color) return color;
  }
  return '';
})()
"""

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
            onSiteIconResolved = { _, _ -> }
        )
    }
}
