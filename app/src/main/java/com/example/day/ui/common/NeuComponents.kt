package com.example.day.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.day.theme.neuExtruded
import com.example.day.theme.neuInset

@Composable
fun isAppDark(): Boolean {
    return MaterialTheme.colorScheme.background.luminance() < 0.5f
}

/**
 * NeuCard: Primary container component that extrudes from the background surface.
 */
@Composable
fun NeuCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isAppDark()
    val shape = RoundedCornerShape(cornerRadius)
    val bevelColor = if (isDark) Color(0xFF505050).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.75f)

    val cardModifier = if (onClick != null) {
        modifier
            .neuExtruded(cornerRadius = cornerRadius, isDark = isDark)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(0.75.dp, bevelColor, shape)
            .clickable(onClick = onClick)
            .padding(contentPadding)
    } else {
        modifier
            .neuExtruded(cornerRadius = cornerRadius, isDark = isDark)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(0.75.dp, bevelColor, shape)
            .padding(contentPadding)
    }

    Box(modifier = cardModifier, content = content)
}

/**
 * Ensures button contents, text, and icons have sufficient contrast against their background surface.
 */
fun ensureContrast(color: Color, surface: Color, isDark: Boolean): Color {
    val contrast = com.example.day.theme.calculateContrast(color, surface)
    if (contrast >= 3.0f) return color
    return if (isDark) {
        com.example.day.theme.adaptAccentForDarkMode(color, surface)
    } else {
        com.example.day.theme.adaptAccentForLightMode(color, surface)
    }
}

/**
 * NeuButton: Primary action button that extrudes at rest and sinks in when pressed.
 */
@Composable
fun NeuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable RowScope.() -> Unit
) {
    val isDark = isAppDark()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val defaultPrimary = MaterialTheme.colorScheme.primary
    val effectiveContentColor = remember(contentColor, surfaceColor, isDark, defaultPrimary) {
        if (contentColor == Color.Unspecified) defaultPrimary else contentColor
    }
    val readableContentColor = ensureContrast(effectiveContentColor, surfaceColor, isDark)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(cornerRadius)

    val shadowModifier = if (isPressed) {
        Modifier.neuInset(cornerRadius = cornerRadius, isDark = isDark)
    } else {
        Modifier.neuExtruded(cornerRadius = cornerRadius, isDark = isDark, blurRadius = 8.dp, offset = 3.dp)
    }

    Box(
        modifier = modifier
            .then(shadowModifier)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides readableContentColor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = content
            )
        }
    }
}

/**
 * NeuIconButton: 44.dp circular icon button with springy neumorphic press states.
 */
@Composable
fun NeuIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 44.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    val isDark = isAppDark()
    val surfaceColor = MaterialTheme.colorScheme.surface
    val readableTint = ensureContrast(tint, surfaceColor, isDark)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val shadowModifier = if (isPressed) {
        Modifier.neuInset(cornerRadius = size / 2, isDark = isDark, blurRadius = 6.dp, offset = 2.dp)
    } else {
        Modifier.neuExtruded(cornerRadius = size / 2, isDark = isDark, blurRadius = 8.dp, offset = 3.dp)
    }

    Box(
        modifier = modifier
            .size(size)
            .then(shadowModifier)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides readableTint) {
            content()
        }
    }
}

/**
 * NeuCheckbox: Custom drawn neumorphic checkbox.
 * Unchecked: pressed-in inset look. Checked: filled primary accent with white check.
 */
@Composable
fun NeuCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val isDark = isAppDark()
    val cornerRadius = 7.dp
    val shape = RoundedCornerShape(cornerRadius)
    val primaryColor = MaterialTheme.colorScheme.primary

    val scale by animateFloatAsState(
        targetValue = if (checked) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "NeuCheckboxScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (checked) primaryColor else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 200),
        label = "NeuCheckboxBg"
    )

    val shadowModifier = if (checked) {
        Modifier.neuExtruded(cornerRadius = cornerRadius, isDark = isDark, blurRadius = 6.dp, offset = 2.dp)
    } else {
        Modifier.neuInset(cornerRadius = cornerRadius, isDark = isDark, blurRadius = 6.dp, offset = 2.dp)
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .then(shadowModifier)
            .clip(shape)
            .background(bgColor, shape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = Color.White,
                modifier = Modifier.size(size * 0.7f)
            )
        }
    }
}

/**
 * NeuTextField: Inset style input field that looks physically pressed into the surface.
 */
@Composable
fun NeuTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    singleLine: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val isDark = isAppDark()
    val shape = RoundedCornerShape(cornerRadius)
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .neuInset(cornerRadius = cornerRadius, isDark = isDark, blurRadius = 6.dp, offset = 2.dp)
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
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    singleLine = singleLine,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    cursorBrush = SolidColor(primaryColor),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailingIcon()
            }
        }
    }
}

/**
 * NeuTabBar: Custom Neumorphic Bottom Navigation Bar.
 * Top extruded shadow with inset pill indicator on the active tab using dynamic primary color.
 */
data class NeuTabItem(
    val label: String,
    val icon: ImageVector? = null,
    val index: Int,
    val iconResId: Int? = null
) {
    constructor(label: String, icon: ImageVector, index: Int) : this(label, icon, index, null)
    constructor(label: String, iconResId: Int, index: Int) : this(label, null, index, iconResId)
}

@Composable
fun NeuTabBar(
    items: List<NeuTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppDark()
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .neuExtruded(
                cornerRadius = 0.dp,
                isDark = isDark,
                blurRadius = 12.dp,
                offset = 4.dp
            )
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { tab ->
                val isSelected = selectedIndex == tab.index
                val pillShape = RoundedCornerShape(20.dp)

                val tabModifier = if (isSelected) {
                    Modifier
                        .neuInset(cornerRadius = 20.dp, isDark = isDark, blurRadius = 6.dp, offset = 2.dp)
                        .clip(pillShape)
                        .background(MaterialTheme.colorScheme.surface, pillShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                } else {
                    Modifier
                        .clip(pillShape)
                        .clickable { onTabSelected(tab.index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                }

                Row(
                    modifier = tabModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (tab.iconResId != null) {
                        Icon(
                            painter = painterResource(id = tab.iconResId),
                            contentDescription = tab.label,
                            tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(25.dp)
                        )
                    } else if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * NeuTimePickerDialog: Interactive Material 3 Clock Dial Time Picker ("sliding thing of time numbers").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeuTimePickerDialog(
    initialTime: String = "09:00",
    title: String = "Select Time",
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = initialTime.trim().split(":")
    val initialHour = initialParts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = initialParts.getOrNull(1)?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectorColor = MaterialTheme.colorScheme.primary,
                        periodSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                        periodSelectorSelectedContentColor = Color.White,
                        timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val min = timePickerState.minute.toString().padStart(2, '0')
                    onTimeSelected("$hour:$min")
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("OK", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
