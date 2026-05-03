package com.norns.nornsai

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
import androidx.compose.runtime.key
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
import com.norns.nornsai.ui.theme.NornsAITheme
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.regex.Pattern
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private var launchUrl by mutableStateOf(DEDICATED_SITE_URL)

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
        launchUrl = resolveLaunchUrl(intent)
        setContent {
            NornsAITheme {
                NornsAiApp(
                    launchUrl = launchUrl,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchUrl = resolveLaunchUrl(intent)
    }
}

class WebNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notification = intent.parcelableExtra<WebNotification>(EXTRA_WEB_NOTIFICATION)
        val notificationId = intent.getIntExtra(
            EXTRA_WEB_NOTIFICATION_ID,
            notification?.tag?.hashCode() ?: 0
        )
        val launchUrl = resolveLaunchUrl(intent)

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
fun NornsAiApp(
    launchUrl: String,
    onClose: () -> Unit
) {
    key(launchUrl) {
        PwaBrowserScreen(
            initialUrl = launchUrl,
            onClose = onClose
        )
    }
}

@Composable
private fun PwaBrowserScreen(
    initialUrl: String,
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
    var pageUrl by remember(initialUrl) { mutableStateOf(initialUrl) }
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
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!isFullscreen) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarHeight)
                    .background(chromeColor)
            )
        }
        AndroidView(
            modifier = Modifier
                .padding(top = if (isFullscreen) 0.dp else statusBarHeight)
                .fillMaxSize(),
            factory = { viewContext ->
                val session = createAppGeckoSession(
                    context = viewContext,
                    runtime = runtime,
                    initialUrl = initialUrl,
                    onCanGoBackChanged = { canGoBack = it },
                    onPageUrlChanged = { resolvedUrl ->
                        if (resolvedUrl.isNotBlank()) {
                            pageUrl = resolvedUrl
                        }
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
                            val savedDownload = saveExternalResponse(viewContext, response)
                            if (savedDownload == null) {
                                Toast.makeText(
                                    viewContext,
                                    "Unable to download this file",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            Toast.makeText(
                                viewContext,
                                "Downloaded ${savedDownload.fileName}",
                                Toast.LENGTH_SHORT
                            ).show()

                            if (savedDownload.openAfterSave) {
                                openDownloadedFile(
                                    context = viewContext,
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
                                    if (granted) {
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
                                GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
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
                        val request = buildFilePromptRequest(viewContext, prompt)
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
                    onChoicePromptRequested = { prompt ->
                        launchChoicePrompt(viewContext, prompt)
                    },
                    onColorPromptRequested = { prompt ->
                        launchColorPrompt(viewContext, prompt)
                    },
                    onDateTimePromptRequested = { prompt ->
                        launchDateTimePrompt(viewContext, prompt)
                    },
                    onSharePromptRequested = { prompt ->
                        GeckoResult<GeckoSession.PromptDelegate.PromptResponse>().apply {
                            val launched = launchSharePrompt(viewContext, prompt)
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
                GeckoView(viewContext).apply {
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

private fun buildDedicatedSessionSettings(): GeckoSessionSettings {
    return GeckoSessionSettings.Builder()
        .usePrivateMode(false)
        .contextId(GECKO_SESSION_CONTEXT_ID)
        .build()
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

private fun launchMainActivity(context: Context, launchUrl: String) {
    context.startActivity(
        Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LAUNCH_URL, launchUrl)
        }
    )
}

private fun resolveNotificationLaunchUrl(sourceUrl: String?): String {
    val normalizedUrl = sourceUrl
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.let(::normalizeUrl)
        ?: return DEDICATED_SITE_URL
    return if (isDedicatedSiteUri(Uri.parse(normalizedUrl))) {
        normalizedUrl
    } else {
        DEDICATED_SITE_URL
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
            putExtra(EXTRA_LAUNCH_URL, launchUrl)
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
            putExtra(EXTRA_LAUNCH_URL, launchUrl)
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
                putExtra(EXTRA_LAUNCH_URL, launchUrl)
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

private fun launchChoicePrompt(
    context: Context,
    prompt: GeckoSession.PromptDelegate.ChoicePrompt
): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
    val selectableChoices = flattenSelectableChoices(prompt.choices)
    if (selectableChoices.isEmpty()) {
        result.complete(prompt.dismiss())
        return result
    }

    val labels = selectableChoices.map { it.label }.toTypedArray()
    val completed = PromptCompletionGuard(result, onDismiss = { prompt.dismiss() })
    val builder = android.app.AlertDialog.Builder(context)
        .setTitle(prompt.title?.takeIf { it.isNotBlank() } ?: "Choose an option")
        .setOnCancelListener { completed.dismiss() }

    prompt.message?.takeIf { it.isNotBlank() }?.let(builder::setMessage)

    when (prompt.type) {
        GeckoSession.PromptDelegate.ChoicePrompt.Type.MENU -> {
            builder.setItems(labels) { _, which ->
                val selected = selectableChoices.getOrNull(which) ?: return@setItems
                completed.complete(prompt.confirm(selected.id))
            }
        }

        GeckoSession.PromptDelegate.ChoicePrompt.Type.SINGLE -> {
            var selectedIndex = selectableChoices.indexOfFirst { it.selected }
            builder.setSingleChoiceItems(labels, selectedIndex) { _, which ->
                selectedIndex = which
            }
            builder.setPositiveButton("OK") { _, _ ->
                val selected = selectableChoices.getOrNull(selectedIndex)
                if (selected != null) {
                    completed.complete(prompt.confirm(selected.id))
                } else {
                    completed.dismiss()
                }
            }
            builder.setNegativeButton("Cancel") { _, _ -> completed.dismiss() }
        }

        GeckoSession.PromptDelegate.ChoicePrompt.Type.MULTIPLE -> {
            val checkedItems = selectableChoices.map { it.selected }.toBooleanArray()
            builder.setMultiChoiceItems(labels, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            builder.setPositiveButton("OK") { _, _ ->
                val selectedIds = selectableChoices
                    .mapIndexedNotNull { index, choice -> choice.id.takeIf { checkedItems[index] } }
                    .toTypedArray()
                completed.complete(prompt.confirm(selectedIds))
            }
            builder.setNegativeButton("Cancel") { _, _ -> completed.dismiss() }
        }

        else -> {
            completed.dismiss()
            return result
        }
    }

    val dialog = builder.create()
    dialog.setOnDismissListener { completed.dismiss() }
    dialog.show()
    return result
}

private fun launchColorPrompt(
    context: Context,
    prompt: GeckoSession.PromptDelegate.ColorPrompt
): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
    val completed = PromptCompletionGuard(result, onDismiss = { prompt.dismiss() })
    val presets = prompt.predefinedValues
        ?.mapNotNull(::normalizeColorValueOrNull)
        ?.distinct()
        .orEmpty()
    val initialColor = normalizeColorValue(
        prompt.defaultValue
            ?: presets.firstOrNull()
            ?: "#2563EB"
    )

    val container = android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        val padding = (20 * context.resources.displayMetrics.density).toInt()
        setPadding(padding, padding / 2, padding, padding / 2)
    }
    val preview = android.view.View(context).apply {
        layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            (56 * context.resources.displayMetrics.density).toInt()
        ).apply {
            bottomMargin = (14 * context.resources.displayMetrics.density).toInt()
        }
        background = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 18 * context.resources.displayMetrics.density
            setColor(android.graphics.Color.parseColor(initialColor))
        }
    }
    val input = android.widget.EditText(context).apply {
        setText(initialColor)
        setSelection(text.length)
        inputType = android.text.InputType.TYPE_CLASS_TEXT
        hint = "#2563EB"
    }
    input.addTextChangedListener(
        object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: android.text.Editable?) {
                val parsed = normalizeColorValueOrNull(s?.toString()) ?: return
                (preview.background as? android.graphics.drawable.GradientDrawable)
                    ?.setColor(android.graphics.Color.parseColor(parsed))
            }
        }
    )

    container.addView(preview)
    container.addView(input)

    if (presets.isNotEmpty()) {
        container.addView(
            android.widget.TextView(context).apply {
                text = "Suggested colors"
                setPadding(0, (12 * context.resources.displayMetrics.density).toInt(), 0, 0)
            }
        )
        container.addView(
            android.widget.GridLayout(context).apply {
                columnCount = 6
                useDefaultMargins = true
                presets.forEach { colorValue ->
                    addView(
                        android.widget.FrameLayout(context).apply {
                            layoutParams = android.widget.GridLayout.LayoutParams().apply {
                                width = (36 * context.resources.displayMetrics.density).toInt()
                                height = (36 * context.resources.displayMetrics.density).toInt()
                            }
                            background = android.graphics.drawable.GradientDrawable().apply {
                                shape = android.graphics.drawable.GradientDrawable.OVAL
                                setColor(android.graphics.Color.parseColor(colorValue))
                                setStroke(
                                    (1 * context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                                    android.graphics.Color.parseColor("#33000000")
                                )
                            }
                            isClickable = true
                            isFocusable = true
                            setOnClickListener {
                                input.setText(colorValue)
                                input.setSelection(input.text.length)
                            }
                        }
                    )
                }
            }
        )
    }

    val dialog = android.app.AlertDialog.Builder(context)
        .setTitle(prompt.title?.takeIf { it.isNotBlank() } ?: "Choose color")
        .setView(container)
        .setPositiveButton("OK") { _, _ ->
            completed.complete(prompt.confirm(normalizeColorValue(input.text?.toString().orEmpty())))
        }
        .setNegativeButton("Cancel") { _, _ -> completed.dismiss() }
        .setOnCancelListener { completed.dismiss() }
        .create()

    dialog.setOnDismissListener { completed.dismiss() }
    dialog.show()
    return result
}

private fun launchDateTimePrompt(
    context: Context,
    prompt: GeckoSession.PromptDelegate.DateTimePrompt
): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
    val completed = PromptCompletionGuard(result, onDismiss = { prompt.dismiss() })

    when (prompt.type) {
        GeckoSession.PromptDelegate.DateTimePrompt.Type.DATE -> {
            val initial = parsePromptDate(prompt.defaultValue)
            val minDate = parsePromptDateOrNull(prompt.minValue)
            val maxDate = parsePromptDateOrNull(prompt.maxValue)
            val dialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selected = LocalDate.of(year, month + 1, dayOfMonth)
                    completed.complete(
                        prompt.confirm(
                            coerceDateWithinBounds(selected, minDate, maxDate)
                                .format(DateTimeFormatter.ISO_LOCAL_DATE)
                        )
                    )
                },
                initial.year,
                initial.monthValue - 1,
                initial.dayOfMonth
            )
            applyDateBounds(dialog, minDate, maxDate)
            dialog.setOnCancelListener { completed.dismiss() }
            dialog.show()
        }

        GeckoSession.PromptDelegate.DateTimePrompt.Type.TIME -> {
            val initial = parsePromptTime(prompt.defaultValue)
            val minTime = parsePromptTimeOrNull(prompt.minValue)
            val maxTime = parsePromptTimeOrNull(prompt.maxValue)
            val dialog = android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    completed.complete(
                        prompt.confirm(
                            coerceTimeWithinBounds(LocalTime.of(hourOfDay, minute), minTime, maxTime)
                                .format(HTML_TIME_FORMATTER)
                        )
                    )
                },
                initial.hour,
                initial.minute,
                true
            )
            dialog.setOnCancelListener { completed.dismiss() }
            dialog.show()
        }

        GeckoSession.PromptDelegate.DateTimePrompt.Type.DATETIME_LOCAL -> {
            val initial = parsePromptDateTime(prompt.defaultValue)
            val minDateTime = parsePromptDateTimeOrNull(prompt.minValue)
            val maxDateTime = parsePromptDateTimeOrNull(prompt.maxValue)
            val dateDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val timeDialog = android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val selected = LocalDateTime.of(year, month + 1, dayOfMonth, hourOfDay, minute)
                            completed.complete(
                                prompt.confirm(
                                    coerceDateTimeWithinBounds(selected, minDateTime, maxDateTime)
                                        .format(HTML_DATETIME_LOCAL_FORMATTER)
                                )
                            )
                        },
                        initial.hour,
                        initial.minute,
                        true
                    )
                    timeDialog.setOnCancelListener { completed.dismiss() }
                    timeDialog.show()
                },
                initial.year,
                initial.monthValue - 1,
                initial.dayOfMonth
            )
            applyDateBounds(
                dialog = dateDialog,
                minDate = minDateTime?.toLocalDate(),
                maxDate = maxDateTime?.toLocalDate()
            )
            dateDialog.setOnCancelListener { completed.dismiss() }
            dateDialog.show()
        }

        GeckoSession.PromptDelegate.DateTimePrompt.Type.MONTH -> {
            return launchMonthPrompt(context, prompt)
        }

        GeckoSession.PromptDelegate.DateTimePrompt.Type.WEEK -> {
            return launchWeekPrompt(context, prompt)
        }

        else -> completed.dismiss()
    }

    return result
}

private fun flattenSelectableChoices(
    choices: Array<GeckoSession.PromptDelegate.ChoicePrompt.Choice>,
    parentLabel: String? = null
): List<SelectableChoice> = buildList {
    choices.forEach { choice ->
        if (choice.separator) return@forEach
        val label = buildString {
            if (!parentLabel.isNullOrBlank()) {
                append(parentLabel)
                append(" / ")
            }
            append(choice.label)
        }
        val childItems = choice.items
        if (!childItems.isNullOrEmpty()) {
            addAll(flattenSelectableChoices(childItems, label))
        } else if (!choice.disabled) {
            add(SelectableChoice(id = choice.id, label = label, selected = choice.selected))
        }
    }
}

private fun parsePromptDate(value: String?): LocalDate {
    return runCatching { LocalDate.parse(value?.take(10).orEmpty()) }.getOrElse { LocalDate.now() }
}

private fun parsePromptTime(value: String?): LocalTime {
    val normalized = value?.substring(0, min(value.length, 5)).orEmpty()
    return runCatching { LocalTime.parse(normalized, HTML_TIME_FORMATTER) }
        .getOrElse { LocalTime.now().withSecond(0).withNano(0) }
}

private fun parsePromptDateTime(value: String?): LocalDateTime {
    val normalized = value?.take(16).orEmpty()
    return runCatching { LocalDateTime.parse(normalized, HTML_DATETIME_LOCAL_FORMATTER) }
        .getOrElse { LocalDateTime.now().withSecond(0).withNano(0) }
}

private fun normalizeColorValue(value: String): String {
    return normalizeColorValueOrNull(value) ?: "#2563EB"
}

private fun normalizeColorValueOrNull(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.matches(Regex("#?[0-9a-fA-F]{6}"))) {
        return if (trimmed.startsWith("#")) trimmed.uppercase(Locale.ROOT) else "#${trimmed.uppercase(Locale.ROOT)}"
    }
    if (trimmed.matches(Regex("#?[0-9a-fA-F]{3}"))) {
        val raw = trimmed.removePrefix("#").uppercase(Locale.ROOT)
        return "#${raw[0]}${raw[0]}${raw[1]}${raw[1]}${raw[2]}${raw[2]}"
    }
    return null
}

private fun launchMonthPrompt(
    context: Context,
    prompt: GeckoSession.PromptDelegate.DateTimePrompt
): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
    val completed = PromptCompletionGuard(result, onDismiss = { prompt.dismiss() })
    val initial = parsePromptMonth(prompt.defaultValue) ?: YearMonth.now()
    val minMonth = parsePromptMonth(prompt.minValue)
    val maxMonth = parsePromptMonth(prompt.maxValue)
    val yearPicker = buildNumberPicker(context)
    val monthPicker = buildNumberPicker(context)

    configureYearPicker(
        picker = yearPicker,
        initialYear = initial.year,
        minYear = minMonth?.year,
        maxYear = maxMonth?.year
    )
    fun updateMonthPicker(year: Int, preferredMonth: Int = monthPicker.value.takeIf { it != 0 } ?: initial.monthValue) {
        val minValue = if (minMonth?.year == year) minMonth.monthValue else 1
        val maxValue = if (maxMonth?.year == year) maxMonth.monthValue else 12
        monthPicker.displayedValues = null
        monthPicker.minValue = minValue
        monthPicker.maxValue = maxValue
        monthPicker.displayedValues = (minValue..maxValue)
            .map { String.format(Locale.ROOT, "%02d", it) }
            .toTypedArray()
        monthPicker.value = preferredMonth.coerceIn(minValue, maxValue)
    }
    updateMonthPicker(yearPicker.value, initial.monthValue)
    yearPicker.setOnValueChangedListener { _, _, newVal ->
        updateMonthPicker(newVal)
    }

    val dialog = android.app.AlertDialog.Builder(context)
        .setTitle(prompt.title?.takeIf { it.isNotBlank() } ?: "Choose month")
        .setView(buildPickerLayout(context, yearPicker, monthPicker))
        .setPositiveButton("OK") { _, _ ->
            completed.complete(prompt.confirm(YearMonth.of(yearPicker.value, monthPicker.value).toString()))
        }
        .setNegativeButton("Cancel") { _, _ -> completed.dismiss() }
        .setOnCancelListener { completed.dismiss() }
        .create()
    dialog.setOnDismissListener { completed.dismiss() }
    dialog.show()
    return result
}

private fun launchWeekPrompt(
    context: Context,
    prompt: GeckoSession.PromptDelegate.DateTimePrompt
): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
    val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
    val completed = PromptCompletionGuard(result, onDismiss = { prompt.dismiss() })
    val initial = parsePromptWeek(prompt.defaultValue) ?: currentWeekValueData()
    val minWeek = parsePromptWeek(prompt.minValue)
    val maxWeek = parsePromptWeek(prompt.maxValue)
    val yearPicker = buildNumberPicker(context)
    val weekPicker = buildNumberPicker(context)

    configureYearPicker(
        picker = yearPicker,
        initialYear = initial.year,
        minYear = minWeek?.year,
        maxYear = maxWeek?.year
    )
    fun updateWeekPicker(year: Int, preferredWeek: Int = weekPicker.value.takeIf { it != 0 } ?: initial.week) {
        val minValue = if (minWeek?.year == year) minWeek.week else 1
        val maxValue = if (maxWeek?.year == year) maxWeek.week else weeksInWeekBasedYear(year)
        weekPicker.displayedValues = null
        weekPicker.minValue = minValue
        weekPicker.maxValue = maxValue
        weekPicker.displayedValues = (minValue..maxValue)
            .map { String.format(Locale.ROOT, "W%02d", it) }
            .toTypedArray()
        weekPicker.value = preferredWeek.coerceIn(minValue, maxValue)
    }
    updateWeekPicker(yearPicker.value, initial.week)
    yearPicker.setOnValueChangedListener { _, _, newVal ->
        updateWeekPicker(newVal)
    }

    val dialog = android.app.AlertDialog.Builder(context)
        .setTitle(prompt.title?.takeIf { it.isNotBlank() } ?: "Choose week")
        .setView(buildPickerLayout(context, yearPicker, weekPicker))
        .setPositiveButton("OK") { _, _ ->
            completed.complete(prompt.confirm(formatWeekValue(yearPicker.value, weekPicker.value)))
        }
        .setNegativeButton("Cancel") { _, _ -> completed.dismiss() }
        .setOnCancelListener { completed.dismiss() }
        .create()
    dialog.setOnDismissListener { completed.dismiss() }
    dialog.show()
    return result
}

private fun buildNumberPicker(context: Context): android.widget.NumberPicker {
    return android.widget.NumberPicker(context).apply {
        descendantFocusability = android.widget.NumberPicker.FOCUS_BLOCK_DESCENDANTS
        wrapSelectorWheel = false
    }
}

private fun configureYearPicker(
    picker: android.widget.NumberPicker,
    initialYear: Int,
    minYear: Int?,
    maxYear: Int?
) {
    val resolvedMin = minYear ?: min(initialYear, LocalDate.now().year) - 20
    val resolvedMax = maxYear ?: max(initialYear, LocalDate.now().year) + 20
    picker.minValue = resolvedMin
    picker.maxValue = max(resolvedMin, resolvedMax)
    picker.value = initialYear.coerceIn(picker.minValue, picker.maxValue)
}

private fun buildPickerLayout(
    context: Context,
    vararg pickers: android.widget.NumberPicker
): android.widget.LinearLayout {
    val density = context.resources.displayMetrics.density
    return android.widget.LinearLayout(context).apply {
        orientation = android.widget.LinearLayout.HORIZONTAL
        val padding = (20 * density).toInt()
        setPadding(padding, padding / 2, padding, padding / 2)
        pickers.forEach { picker ->
            addView(
                picker,
                android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )
        }
    }
}

private fun parsePromptDateOrNull(value: String?): LocalDate? {
    val normalized = value?.take(10)?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { LocalDate.parse(normalized) }.getOrNull()
}

private fun parsePromptTimeOrNull(value: String?): LocalTime? {
    val normalized = value?.take(5)?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { LocalTime.parse(normalized, HTML_TIME_FORMATTER) }.getOrNull()
}

private fun parsePromptDateTimeOrNull(value: String?): LocalDateTime? {
    val normalized = value?.take(16)?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { LocalDateTime.parse(normalized, HTML_DATETIME_LOCAL_FORMATTER) }.getOrNull()
}

private fun parsePromptMonth(value: String?): YearMonth? {
    val normalized = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { YearMonth.parse(normalized) }.getOrNull()
}

private fun parsePromptWeek(value: String?): WeekValue? {
    val normalized = value?.trim()?.takeIf { it.matches(Regex("\\d{4}-W\\d{2}")) } ?: return null
    return WeekValue(
        year = normalized.substring(0, 4).toInt(),
        week = normalized.substring(6, 8).toInt()
    )
}

private fun currentWeekValueData(): WeekValue {
    val today = LocalDate.now()
    return WeekValue(
        year = today.get(java.time.temporal.WeekFields.ISO.weekBasedYear()),
        week = today.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
    )
}

private fun formatWeekValue(year: Int, week: Int): String {
    return String.format(Locale.ROOT, "%04d-W%02d", year, week)
}

private fun weeksInWeekBasedYear(year: Int): Int {
    return LocalDate.of(year, 12, 28).get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
}

private fun applyDateBounds(
    dialog: android.app.DatePickerDialog,
    minDate: LocalDate?,
    maxDate: LocalDate?
) {
    val datePicker = dialog.datePicker
    minDate?.let {
        val millis = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePicker.minDate = millis
    }
    maxDate?.let {
        val millis = it.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePicker.maxDate = millis
    }
}

private fun coerceDateWithinBounds(
    value: LocalDate,
    minDate: LocalDate?,
    maxDate: LocalDate?
): LocalDate {
    var resolved = value
    if (minDate != null && resolved.isBefore(minDate)) resolved = minDate
    if (maxDate != null && resolved.isAfter(maxDate)) resolved = maxDate
    return resolved
}

private fun coerceTimeWithinBounds(
    value: LocalTime,
    minTime: LocalTime?,
    maxTime: LocalTime?
): LocalTime {
    var resolved = value
    if (minTime != null && resolved.isBefore(minTime)) resolved = minTime
    if (maxTime != null && resolved.isAfter(maxTime)) resolved = maxTime
    return resolved
}

private fun coerceDateTimeWithinBounds(
    value: LocalDateTime,
    minDateTime: LocalDateTime?,
    maxDateTime: LocalDateTime?
): LocalDateTime {
    var resolved = value
    if (minDateTime != null && resolved.isBefore(minDateTime)) resolved = minDateTime
    if (maxDateTime != null && resolved.isAfter(maxDateTime)) resolved = maxDateTime
    return resolved
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
    onPageUrlChanged: (String) -> Unit,
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
    onChoicePromptRequested: (
        GeckoSession.PromptDelegate.ChoicePrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onColorPromptRequested: (
        GeckoSession.PromptDelegate.ColorPrompt
    ) -> GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    onDateTimePromptRequested: (
        GeckoSession.PromptDelegate.DateTimePrompt
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
    return GeckoSession(buildDedicatedSessionSettings()).apply {
        contentDelegate = object : GeckoSession.ContentDelegate {
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
                    shouldOpenExternallyFromDedicatedApp(targetUri) &&
                    launchExternalUri(context, targetUri)
                ) {
                    return GeckoResult.deny()
                }
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

            override fun onChoicePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ChoicePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onChoicePromptRequested(prompt)
            }

            override fun onColorPrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.ColorPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onColorPromptRequested(prompt)
            }

            override fun onDateTimePrompt(
                session: GeckoSession,
                prompt: GeckoSession.PromptDelegate.DateTimePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
                return onDateTimePromptRequested(prompt)
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

private fun shouldOpenExternallyFromDedicatedApp(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    if (scheme != "http" && scheme != "https") return false
    return !isDedicatedSiteUri(uri)
}

private fun isDedicatedSiteUri(uri: Uri): Boolean {
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    val host = uri.host?.lowercase(Locale.ROOT)
    return (scheme == "http" || scheme == "https") && host == DEDICATED_SITE_HOST
}

private fun resolveLaunchUrl(intent: Intent?): String {
    val launchCandidates = listOf(
        intent?.getStringExtra(EXTRA_LAUNCH_URL),
        intent?.dataString
    )
    return launchCandidates
        .filterNotNull()
        .mapNotNull { candidate -> normalizeUrl(candidate) }
        .firstOrNull { url -> isDedicatedSiteUri(Uri.parse(url)) }
        ?: DEDICATED_SITE_URL
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

private fun extractManifestUrl(html: String, pageUrl: String): String? {
    return LINK_TAG_REGEX.findAll(html).mapNotNull { match ->
        val tag = match.value
        val rel = extractHtmlAttribute(tag, "rel")?.lowercase(Locale.ROOT) ?: return@mapNotNull null
        if (!rel.contains("manifest")) return@mapNotNull null
        val href = extractHtmlAttribute(tag, "href") ?: return@mapNotNull null
        resolveRelativeUrl(pageUrl, href)
    }.firstOrNull()
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

private fun defaultNameForUrl(url: String): String {
    val host = runCatching { Uri.parse(url).host }.getOrNull().orEmpty()
    if (host.isBlank()) return url
    return host.removePrefix("www.")
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

private data class SelectableChoice(
    val id: String,
    val label: String,
    val selected: Boolean
)

private data class WeekValue(
    val year: Int,
    val week: Int
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

private class PromptCompletionGuard(
    private val result: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>,
    private val onDismiss: () -> GeckoSession.PromptDelegate.PromptResponse
) {
    private var completed = false

    fun complete(response: GeckoSession.PromptDelegate.PromptResponse) {
        if (completed) return
        completed = true
        result.complete(response)
    }

    fun dismiss() {
        if (completed) return
        completed = true
        result.complete(onDismiss())
    }
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

private const val EXTRA_LAUNCH_URL = "launch_url"
private const val EXTRA_WEB_NOTIFICATION = "web_notification"
private const val EXTRA_WEB_NOTIFICATION_ACTION = "web_notification_action"
private const val EXTRA_WEB_NOTIFICATION_ID = "web_notification_id"
private const val MAX_NOTIFICATION_ACTIONS = 3
private const val HTTP_TIMEOUT_MS = 5500
private const val MIN_SPLASH_DURATION_MS = 280L
private const val SPLASH_EXIT_DURATION_MS = 360L
private const val CHROME_COLOR_SAMPLE_INTERVAL_MS = 900L
private const val APP_USER_AGENT = "NornsAI/1.0"
private const val DEDICATED_SITE_URL = "https://ai.norns.dpdns.org/"
private const val DEDICATED_SITE_HOST = "ai.norns.dpdns.org"
private const val WEB_NOTIFICATION_CHANNEL_ID = "nornsai_messages"
private const val GECKO_SESSION_CONTEXT_ID = "nornsai"
private const val ACTION_WEB_NOTIFICATION_CLICK = "com.norns.nornsai.WEB_NOTIFICATION_CLICK"
private const val ACTION_WEB_NOTIFICATION_DISMISS = "com.norns.nornsai.WEB_NOTIFICATION_DISMISS"
private const val PENDING_INTENT_FLAGS =
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
private val HTML_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val HTML_DATETIME_LOCAL_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
private val LINK_TAG_REGEX = Regex("<link\\b[^>]*>", setOf(RegexOption.IGNORE_CASE))
private val META_TAG_REGEX = Regex("<meta\\b[^>]*>", setOf(RegexOption.IGNORE_CASE))
private val RGB_COLOR_REGEX = Regex(
    "rgba?\\(\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})\\s*,\\s*(\\d{1,3})(?:\\s*,\\s*([0-9.]+))?\\s*\\)",
    RegexOption.IGNORE_CASE
)
private val INTERNAL_BROWSER_SCHEMES = setOf("http", "https", "about", "javascript", "data", "blob")
