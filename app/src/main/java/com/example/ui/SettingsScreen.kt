package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.SalmonAccent
import com.example.ui.theme.SignalOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltronBackground
import com.example.ui.theme.UltronBorder
import com.example.ui.theme.UltronSurface
import com.example.ui.theme.UltronSurfaceHighlight

@Composable
fun SettingsScreen(
  modifier: Modifier = Modifier,
  onNavigateToChat: () -> Unit = {}
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  // Real permission check helpers
  fun isContactsGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED
  }

  fun isBluetoothGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  fun isOverlayGranted(): Boolean {
    return Settings.canDrawOverlays(context)
  }

  // Permission states initialized from real status
  var contactsEnabled by remember { mutableStateOf(isContactsGranted()) }
  var bluetoothEnabled by remember { mutableStateOf(isBluetoothGranted()) }
  var overlayModeEnabled by remember { mutableStateOf(isOverlayGranted()) }
  var accessibilityEnabled by remember { mutableStateOf(false) }

  var statusNotice by remember { mutableStateOf<String?>(null) }
  val scrollState = rememberScrollState()

  // Refresh states on lifecycle resume (especially when returning from system Settings screen for Overlay)
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        contactsEnabled = isContactsGranted()
        bluetoothEnabled = isBluetoothGranted()
        overlayModeEnabled = isOverlayGranted()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  // Permission Request Launchers
  val contactsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    contactsEnabled = isGranted
    statusNotice = if (isGranted) {
      "PERMISSION // READ_CONTACTS GRANTED"
    } else {
      "PERMISSION // READ_CONTACTS DENIED"
    }
  }

  val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    bluetoothEnabled = isGranted
    statusNotice = if (isGranted) {
      "PERMISSION // BLUETOOTH_CONNECT GRANTED"
    } else {
      "PERMISSION // BLUETOOTH_CONNECT DENIED"
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(UltronBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // 1. Top Bar: Wordmark + Telemetry Badge
    SettingsTopBar()

    // 2. Settings Content Area
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Header & Subtitle
      Column {
        Text(
          text = "SYSTEM PREFERENCES",
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          letterSpacing = 2.sp,
          color = TextPrimary,
          modifier = Modifier.testTag("settings_header_title")
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "KERNEL VERSION // OPERATIONAL",
          fontFamily = FontFamily.Monospace,
          fontSize = 11.sp,
          letterSpacing = 1.2.sp,
          color = SignalOrange
        )
      }

      // Thin Divider Line
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(UltronBorder)
      )

      // Toggle Row 1: Overlay Mode (Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
      SettingToggleRow(
        icon = Icons.Default.Layers,
        title = "OVERLAY MODE",
        description = if (overlayModeEnabled) {
          "Overlay active: ULTRON listening over other apps"
        } else {
          "Tap to enable system overlay permission in settings"
        },
        isChecked = overlayModeEnabled,
        onCheckedChange = { targetState ->
          if (targetState) {
            val intent = Intent(
              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            statusNotice = "OPENING SYSTEM OVERLAY SETTINGS..."
          } else {
            // Can't revoke system alert permission programmatically, redirect to settings
            val intent = Intent(
              Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
              Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
            statusNotice = "ADJUST OVERLAY PERMISSION IN SETTINGS"
          }
        },
        testTag = "toggle_overlay_mode"
      )

      // Toggle Row 2: Accessibility (Screen Reading)
      SettingToggleRow(
        icon = Icons.Default.Visibility,
        title = "ACCESSIBILITY",
        description = "Screen reading for UI context (requires manual enabling in system settings)",
        isChecked = accessibilityEnabled,
        onCheckedChange = {
          accessibilityEnabled = it
          statusNotice = if (it) {
            "ACCESSIBILITY // MANUAL SYSTEM ENABLING REQUIRED"
          } else {
            "ACCESSIBILITY // STANDBY"
          }
        },
        testTag = "toggle_accessibility"
      )

      // Toggle Row 3: Contacts (READ_CONTACTS runtime permission)
      SettingToggleRow(
        icon = Icons.Default.Contacts,
        title = "CONTACTS",
        description = if (contactsEnabled) {
          "Contact lookup granted for voice calls"
        } else {
          "Tap to request contact lookup permission"
        },
        isChecked = contactsEnabled,
        onCheckedChange = { targetState ->
          if (targetState) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
          } else {
            // Inform user to manage in system app settings if already granted
            statusNotice = "PERMISSION CAN BE REVOKED IN APP SETTINGS"
          }
        },
        testTag = "toggle_contacts"
      )

      // Toggle Row 4: Bluetooth (BLUETOOTH_CONNECT runtime permission)
      SettingToggleRow(
        icon = Icons.Default.Bluetooth,
        title = "BLUETOOTH",
        description = if (bluetoothEnabled) {
          "Bluetooth controller link authorized"
        } else {
          "Tap to request Bluetooth connect permission"
        },
        isChecked = bluetoothEnabled,
        onCheckedChange = { targetState ->
          if (targetState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
              bluetoothEnabled = true
              statusNotice = "BLUETOOTH LINK // ACTIVE"
            }
          } else {
            statusNotice = "PERMISSION CAN BE REVOKED IN APP SETTINGS"
          }
        },
        testTag = "toggle_bluetooth"
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Apply Changes Button: Solid salmon-colored button with dark text
      Button(
        onClick = {
          // Re-evaluate all real permission states
          contactsEnabled = isContactsGranted()
          bluetoothEnabled = isBluetoothGranted()
          overlayModeEnabled = isOverlayGranted()
          statusNotice = "PREFERENCES COMMITTED // SYNC OK"
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("apply_changes_button"),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = SalmonAccent,
          contentColor = UltronBackground
        )
      ) {
        Text(
          text = "APPLY CHANGES",
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          letterSpacing = 1.8.sp,
          color = UltronBackground
        )
      }

      // Feedback notice if applied
      if (statusNotice != null) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SignalOrange, RoundedCornerShape(6.dp))
            .background(UltronSurface)
            .padding(vertical = 8.dp, horizontal = 12.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = statusNotice ?: "",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = SignalOrange
          )
        }
      }
    }

    // 3. Bottom Navigation (Chat / Settings)
    SettingsBottomNavigationBar(
      activeTab = "settings",
      onTabSelected = { tab ->
        if (tab == "chat") {
          onNavigateToChat()
        }
      }
    )
  }
}

@Composable
private fun SettingsTopBar() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(UltronBackground)
      .drawBehind {
        drawLine(
          color = UltronBorder,
          start = Offset(0f, size.height),
          end = Offset(size.width, size.height),
          strokeWidth = 1.dp.toPx()
        )
      }
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(SignalOrange, shape = RoundedCornerShape(1.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "ULTRON",
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp,
          letterSpacing = 3.sp,
          color = TextPrimary
        )
      }

      // Telemetry badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .border(1.dp, UltronBorder, RoundedCornerShape(4.dp))
          .padding(horizontal = 6.dp, vertical = 3.dp)
      ) {
        Box(
          modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(SignalOrange)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "NODE.01",
          fontFamily = FontFamily.Monospace,
          fontSize = 10.sp,
          letterSpacing = 1.sp,
          color = TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "SYS.CONFIG // STAGED",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        color = SalmonAccent
      )
      Text(
        text = "PREFS.ACTIVE",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun SettingToggleRow(
  icon: ImageVector,
  title: String,
  description: String,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  testTag: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(UltronSurface, RoundedCornerShape(8.dp))
      .border(1.dp, UltronBorder, RoundedCornerShape(8.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onCheckedChange(!isChecked) }
      .padding(horizontal = 14.dp, vertical = 14.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Icon Box
      Box(
        modifier = Modifier
          .size(38.dp)
          .background(UltronSurfaceHighlight, RoundedCornerShape(6.dp))
          .border(
            1.dp,
            if (isChecked) SignalOrange.copy(alpha = 0.6f) else UltronBorder,
            RoundedCornerShape(6.dp)
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = if (isChecked) SignalOrange else TextSecondary,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(
          text = title,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.SemiBold,
          fontSize = 13.sp,
          letterSpacing = 1.sp,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          fontFamily = FontFamily.SansSerif,
          fontSize = 12.sp,
          lineHeight = 16.sp,
          color = TextSecondary
        )
      }
    }

    Spacer(modifier = Modifier.width(12.dp))

    // Square Toggle Switch matching UltronBorder / SignalOrange styling
    SquareToggleSwitch(
      checked = isChecked,
      onCheckedChange = onCheckedChange
    )
  }
}

@Composable
private fun SquareToggleSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  val switchWidth = 46.dp
  val switchHeight = 26.dp
  val thumbSize = 18.dp
  val padding = 3.dp

  val targetOffset = if (checked) (switchWidth - thumbSize - padding * 2) else 0.dp
  val animatedOffset by animateDpAsState(
    targetValue = targetOffset,
    animationSpec = tween(durationMillis = 180),
    label = "thumbOffset"
  )

  val borderColor by animateColorAsState(
    targetValue = if (checked) SignalOrange else UltronBorder,
    animationSpec = tween(durationMillis = 180),
    label = "borderColor"
  )

  val thumbColor by animateColorAsState(
    targetValue = if (checked) SignalOrange else TextMuted,
    animationSpec = tween(durationMillis = 180),
    label = "thumbColor"
  )

  Box(
    modifier = Modifier
      .size(width = switchWidth, height = switchHeight)
      .background(UltronBackground, RoundedCornerShape(4.dp))
      .border(1.dp, borderColor, RoundedCornerShape(4.dp))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null
      ) { onCheckedChange(!checked) }
      .padding(padding),
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .offset(x = animatedOffset)
        .size(thumbSize)
        .background(thumbColor, RoundedCornerShape(2.dp))
    )
  }
}

@Composable
private fun SettingsBottomNavigationBar(
  activeTab: String,
  onTabSelected: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(56.dp)
      .background(UltronBackground)
      .drawBehind {
        drawLine(
          color = UltronBorder,
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1.dp.toPx()
        )
      },
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Tab 1: Chat
    val isChatActive = activeTab == "chat"
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { onTabSelected("chat") }
        .testTag("tab_chat"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.ChatBubbleOutline,
        contentDescription = "Chat",
        tint = if (isChatActive) SignalOrange else TextMuted,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "CHAT",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = if (isChatActive) FontWeight.Bold else FontWeight.Normal,
        letterSpacing = 1.sp,
        color = if (isChatActive) SignalOrange else TextMuted
      )
    }

    // Tab 2: Settings
    val isSettingsActive = activeTab == "settings"
    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxSize()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null
        ) { onTabSelected("settings") }
        .testTag("tab_settings"),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = if (isSettingsActive) SignalOrange else TextMuted,
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "SETTINGS",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = if (isSettingsActive) FontWeight.Bold else FontWeight.Normal,
        letterSpacing = 1.sp,
        color = if (isSettingsActive) SignalOrange else TextMuted
      )
    }
  }
}
