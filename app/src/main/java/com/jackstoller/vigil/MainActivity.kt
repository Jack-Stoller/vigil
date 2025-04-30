package com.jackstoller.vigil

import android.app.AlertDialog
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.jackstoller.vigil.ui.theme.VigilTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.graphics.drawable.toBitmap


class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VigilTheme {
                MainScreen(viewModel, ::promptEnableAccessibilityService)
            }
        }
    }

    private fun promptEnableAccessibilityService() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("To function properly, Vigil requires accessibility service access.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()

        Handler(Looper.getMainLooper()).postDelayed({
            viewModel.checkServiceStatus()
        }, 100)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, promptEnableAccessibilityService: () -> Unit) {
    val isServiceEnabled = viewModel.isServiceEnabled.collectAsState()
    val events = viewModel.events.collectAsState()
    val isRefreshing = remember { mutableStateOf(false) }
    val eventTypes = viewModel.eventTypes.collectAsState()
    val selectedEventType = viewModel.selectedEventType.collectAsState()

    val expanded = remember { mutableStateOf(false) }

    val onRefresh = {
        isRefreshing.value = true
        viewModel.refreshEvents()
        isRefreshing.value = false
    }

    LaunchedEffect(Unit) {
        viewModel.checkServiceStatus()
        viewModel.refreshEvents()
        viewModel.startObservingServiceStatus()
    }

    LaunchedEffect(isServiceEnabled.value) {
        if (!isServiceEnabled.value) {
            promptEnableAccessibilityService()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    buildAnnotatedString {
                        append("Vigil Status: ")

                        if (isServiceEnabled.value) {
                            withStyle(style = SpanStyle(color = Color.Green)) {
                                append("✔ Enabled")
                            }
                        } else {
                            withStyle(style = SpanStyle(color = Color.Red)) {
                                append("️✖ Disabled")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                val context = LocalContext.current

                Button(onClick = { openAccessibilitySettings(context) }) {
                    Text("Accessibility Settings")
                }

            }

            Text("Captured Events:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dropdown to select event type
                ExposedDropdownMenuBox(
                    expanded = expanded.value,
                    onExpandedChange = { expanded.value = !expanded.value }
                ) {
                    TextField(
                        readOnly = true,
                        value = selectedEventType.value ?: "All",
                        onValueChange = {},
                        label = { Text("Filter by Event Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value) },
                        modifier = Modifier.menuAnchor()
                    )
                    DropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All") },
                            onClick = {
                                viewModel.selectEventType(null)
                                expanded.value = false
                            }
                        )
                        eventTypes.value.forEach { eventType ->
                            DropdownMenuItem(
                                text = { Text(eventType) },
                                onClick = {
                                    viewModel.selectEventType(eventType)
                                    expanded.value = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))


                Button(onClick = { viewModel.clearEvents() }) {
                    Text("Clear")
                }

            }

            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing.value),
                onRefresh = onRefresh
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(events.value) { event ->
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val drawable = LocalContext.current.packageManager.getApplicationIcon(event.packageName ?: "")
                                Image(
                                    drawable.toBitmap(config = Bitmap.Config.ARGB_8888).asImageBitmap()
                                    , contentDescription = "Image", modifier = Modifier
                                        .size(40.dp)
                                        .padding(8.dp)

                                )
                                Column {
                                    Text(
                                        text = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp))} — ${event.eventType}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${event.packageName}/${event.className}",
                                        fontWeight = FontWeight.Light,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Text: ${event.text}",
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "ContentDesc: ${event.contentDescription ?: "None"}"
                            )
                        }
                    }

                }
            }
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _isServiceEnabled = MutableStateFlow(false)
    val isServiceEnabled: StateFlow<Boolean> = _isServiceEnabled

    private var settingsObserver: ContentObserver? = null

    private val db = EventLoggerDatabase(application)

    private val _events = MutableStateFlow<List<EventEntry>>(emptyList())
    val events: StateFlow<List<EventEntry>> = _events

    private val _eventTypes = MutableStateFlow<List<String>>(emptyList())
    val eventTypes: StateFlow<List<String>> = _eventTypes

    private val _selectedEventType = MutableStateFlow<String?>(null)
    val selectedEventType: StateFlow<String?> = _selectedEventType

    init {
        checkServiceStatus()
    }

    fun startObservingServiceStatus() {
        val context = getApplication<Application>().applicationContext
        val resolver = context.contentResolver

        settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)

                Log.d("VigilApp", "ContentObserver triggered onChange()")
                checkServiceStatus()
            }
        }

        resolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            true,
            settingsObserver!!
        )
    }

    override fun onCleared() {
        super.onCleared()
        settingsObserver?.let {
            getApplication<Application>().contentResolver.unregisterContentObserver(it)
        }
    }

    fun checkServiceStatus() {
        val context = getApplication<Application>().applicationContext
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val service = ComponentName(context, KeyloggerService::class.java)
        _isServiceEnabled.value = enabledServices
            ?.split(":")
            ?.any { it.equals(service.flattenToString(), ignoreCase = true) } == true
    }

    fun refreshEvents() {
        loadEvents()
    }

    fun clearEvents() {
        db.clearEvents()
        refreshEvents()
    }

    private fun loadEvents() {
        Log.d("VigilApp", "Getting events from the database")

        val allEvents = db.getAllEvents()

        val filteredEvents = _selectedEventType.value?.let { type ->
            allEvents.filter { it.eventType == type }
        } ?: allEvents

        _events.value = filteredEvents.take(50)

        // Update event types dynamically
        _eventTypes.value = allEvents.map { it.eventType }.distinct()
    }

    fun selectEventType(type: String?) {
        _selectedEventType.value = type
        refreshEvents()
    }
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
}
