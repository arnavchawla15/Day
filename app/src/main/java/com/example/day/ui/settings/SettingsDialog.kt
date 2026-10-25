package com.example.day.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.day.R
import com.example.day.theme.isValidHexColor
import com.example.day.theme.normalizeHex
import com.example.day.theme.parseHexColor
import com.example.day.ui.DayViewModel
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import com.example.day.theme.neuInset
import com.example.day.ui.common.isAppDark
import com.example.day.ui.common.NeuButton
import com.example.day.ui.common.NeuCard
import com.example.day.ui.common.NeuIconButton
import com.example.day.ui.common.NeuTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: DayViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val primaryColor = MaterialTheme.colorScheme.primary

    // Quotes state
    val isDailyQuote by viewModel.isDailyQuoteEnabled.collectAsState()
    val personalQuote by viewModel.personalQuote.collectAsState()
    var quoteEnabledState by remember { mutableStateOf(isDailyQuote) }
    var customQuoteInput by remember(personalQuote) { mutableStateOf(personalQuote) }

    // App Theme state
    val appPrimaryHex by viewModel.appPrimaryHex.collectAsState()
    val appDarkMode by viewModel.appDarkMode.collectAsState()
    var selectedAppHex by remember(appPrimaryHex) { mutableStateOf(appPrimaryHex) }
    var appHexInput by remember(appPrimaryHex) { mutableStateOf(appPrimaryHex) }
    var isAppDarkState by remember(appDarkMode) { mutableStateOf(appDarkMode) }

    // Independent Widget Theme state
    val widgetPrimaryHex by viewModel.widgetPrimaryHex.collectAsState()
    val widgetDarkMode by viewModel.widgetDarkMode.collectAsState()
    var selectedWidgetHex by remember(widgetPrimaryHex) { mutableStateOf(widgetPrimaryHex) }
    var widgetHexInput by remember(widgetPrimaryHex) { mutableStateOf(widgetPrimaryHex) }
    var isWidgetDarkState by remember(widgetDarkMode) { mutableStateOf(widgetDarkMode) }

    // Gemini API key state
    val savedApiKey by viewModel.geminiApiKey.collectAsState()
    var apiKeyInput by remember(savedApiKey) { mutableStateOf(savedApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var isTestingApiKey by remember { mutableStateOf(false) }

    // AI Schedule state (Feature 2B)
    val schedulePrefs by viewModel.schedulePreferences.collectAsState()
    var autoScheduleEnabledState by remember(schedulePrefs) { mutableStateOf(schedulePrefs.autoScheduleEnabled) }
    var wakeUpTimeInput by remember(schedulePrefs) { mutableStateOf(schedulePrefs.wakeUpTime) }
    var sleepTimeInput by remember(schedulePrefs) { mutableStateOf(schedulePrefs.sleepTime) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(scrollState)
            ) {
                // Top Header with Day Logo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuIconButton(
                        onClick = onDismiss,
                        size = 40.dp,
                        tint = MaterialTheme.colorScheme.onBackground
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_day_logo),
                        contentDescription = "Day Logo",
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Quote Preferences NeuCard
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Quote Preferences",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Daily Rotating Quote",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Inspiring daily productivity quotes on Today tab and home screen widget.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = quoteEnabledState,
                                    onCheckedChange = {
                                        quoteEnabledState = it
                                        viewModel.updateQuoteSettings(it, customQuoteInput.trim())
                                        Toast.makeText(context, if (it) "Daily rotating quote enabled" else "Custom quote mode enabled", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor
                                    )
                                )
                            }

                            // Custom Quote Input shown when rotating quote is toggled off
                            if (!quoteEnabledState) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Personal Custom Quote",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Displayed across the app and on the widget instead of the daily rotating quote.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NeuTextField(
                                    value = customQuoteInput,
                                    onValueChange = { customQuoteInput = it },
                                    placeholder = "e.g. Focus on what matters today.",
                                    singleLine = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                NeuButton(
                                    onClick = {
                                        viewModel.updateQuoteSettings(false, customQuoteInput.trim())
                                        Toast.makeText(context, "Custom quote saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.align(Alignment.End),
                                    cornerRadius = 10.dp
                                ) {
                                    Text("Save Quote", fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }
                        }
                    }

                    // 2. App Theme NeuCard (8-Color Palette Row + Dark Mode)
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "App Theme",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Customize the visual appearance inside the Day application.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dark Mode (App)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Physical dark neumorphic surface",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isAppDarkState,
                                    onCheckedChange = {
                                        isAppDarkState = it
                                        viewModel.saveAppThemeSettings(selectedAppHex, isAppDarkState)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "App Accent Hex Color",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val previewColor = parseHexColor(appHexInput, primaryColor)
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )

                                NeuTextField(
                                    value = appHexInput,
                                    onValueChange = { input ->
                                        appHexInput = input.trim()
                                    },
                                    placeholder = "#436B52",
                                    modifier = Modifier.weight(1f)
                                )

                                NeuButton(
                                    onClick = {
                                        if (isValidHexColor(appHexInput)) {
                                            val normalized = normalizeHex(appHexInput)
                                            appHexInput = normalized
                                            selectedAppHex = normalized
                                            viewModel.saveAppThemeSettings(normalized, isAppDarkState)
                                            Toast.makeText(context, "App color updated: $normalized", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Invalid hex! Use #RRGGBB format", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    cornerRadius = 10.dp
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter any 6-digit hex color code (e.g. #233C26, #3B82F6).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 3. Independent Widget Theme NeuCard
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Widget Theme (Independent)",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Choose a unique dark/light mode and accent color specifically for the home screen widget.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dark Mode (Widget)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "High contrast dark widget tile",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isWidgetDarkState,
                                    onCheckedChange = {
                                        isWidgetDarkState = it
                                        viewModel.saveWidgetThemeSettings(selectedWidgetHex, isWidgetDarkState)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Widget Accent Hex Color",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val previewColor = parseHexColor(widgetHexInput, parseHexColor(selectedWidgetHex))
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(previewColor)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                )

                                NeuTextField(
                                    value = widgetHexInput,
                                    onValueChange = { input ->
                                        widgetHexInput = input.trim()
                                    },
                                    placeholder = "#436B52",
                                    modifier = Modifier.weight(1f)
                                )

                                NeuButton(
                                    onClick = {
                                        if (isValidHexColor(widgetHexInput)) {
                                            val normalized = normalizeHex(widgetHexInput)
                                            widgetHexInput = normalized
                                            selectedWidgetHex = normalized
                                            viewModel.saveWidgetThemeSettings(normalized, isWidgetDarkState)
                                            Toast.makeText(context, "Widget color updated: $normalized", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Invalid hex! Use #RRGGBB format", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    cornerRadius = 10.dp
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enter any 6-digit hex color code for the widget.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 4. Gemini API Key NeuCard with Guidance & Testing
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 16.dp,
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Gemini AI API Key",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Powers the smart input bar at the bottom. You can type natural sentences like 'Dentist at 3pm tomorrow' or 'Pay bills Friday p1' to automatically create schedule blocks and todos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You can get a free Gemini API key from Google AI Studio:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Link to Google AI Studio
                            NeuButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                    context.startActivity(intent)
                                },
                                cornerRadius = 10.dp,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = primaryColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Get Free Key at Google AI Studio",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Neumorphic Inset API Key Field with working Hide/Show Password Masking
                            val isDark = isAppDark()
                            val shape = RoundedCornerShape(16.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .neuInset(cornerRadius = 16.dp, isDark = isDark, blurRadius = 6.dp, offset = 2.dp)
                                    .clip(shape)
                                    .background(MaterialTheme.colorScheme.surface, shape)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (apiKeyInput.isEmpty()) {
                                            Text(
                                                text = "Paste Gemini API Key (e.g. AIzaSy...)",
                                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            )
                                        }
                                        BasicTextField(
                                            value = apiKeyInput,
                                            onValueChange = { apiKeyInput = it },
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                                            singleLine = true,
                                            visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            cursorBrush = SolidColor(primaryColor),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = { isApiKeyVisible = !isApiKeyVisible },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isApiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = if (isApiKeyVisible) "Hide API key" else "Show API key",
                                            tint = if (isApiKeyVisible) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Test Key Button
                                NeuButton(
                                    onClick = {
                                        val trimmedKey = apiKeyInput.trim()
                                        if (trimmedKey.isBlank()) {
                                            Toast.makeText(context, "Please enter an API key first", Toast.LENGTH_SHORT).show()
                                            return@NeuButton
                                        }
                                        isTestingApiKey = true
                                        coroutineScope.launch {
                                            val result = viewModel.validateGeminiKey(trimmedKey)
                                            isTestingApiKey = false
                                            result.fold(
                                                onSuccess = {
                                                    viewModel.saveGeminiApiKey(trimmedKey)
                                                    Toast.makeText(context, "API Key is valid and saved ✓", Toast.LENGTH_LONG).show()
                                                },
                                                onFailure = { error ->
                                                    Toast.makeText(context, "Test failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 12.dp,
                                    enabled = !isTestingApiKey
                                ) {
                                    Text(
                                        text = if (isTestingApiKey) "Testing..." else "Test Connection",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Save Key Button
                                NeuButton(
                                    onClick = {
                                        val trimmedKey = apiKeyInput.trim()
                                        viewModel.saveGeminiApiKey(trimmedKey)
                                        Toast.makeText(context, "API Key saved successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    cornerRadius = 12.dp
                                ) {
                                    Text(
                                        text = "Save Key",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // ── AI Schedule Card (Feature 2B) ──────────────────────────
                    NeuCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Schedule",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Auto-generates daily schedule from timetable",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = autoScheduleEnabledState,
                                    onCheckedChange = { autoScheduleEnabledState = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = primaryColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wake-up Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    NeuTextField(
                                        value = wakeUpTimeInput,
                                        onValueChange = { wakeUpTimeInput = it },
                                        placeholder = "07:00",
                                        singleLine = true
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Sleep Time",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    NeuTextField(
                                        value = sleepTimeInput,
                                        onValueChange = { sleepTimeInput = it },
                                        placeholder = "23:00",
                                        singleLine = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Day AI Pointer Note (Feature 2B)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_day_logo),
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Your timetable and preferences are now managed directly through conversation with Day! Just tell Day your schedule in the Day tab. 💬",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            NeuButton(
                                onClick = {
                                    viewModel.saveSchedulePreferences(
                                        schedulePrefs.copy(
                                            autoScheduleEnabled = autoScheduleEnabledState,
                                            wakeUpTime = wakeUpTimeInput.trim().ifBlank { "07:00" },
                                            sleepTime = sleepTimeInput.trim().ifBlank { "23:00" }
                                        )
                                    )
                                    Toast.makeText(context, "Schedule times saved successfully ✓", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = 12.dp
                            ) {
                                Text(
                                    text = "Save Schedule Times",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = primaryColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
