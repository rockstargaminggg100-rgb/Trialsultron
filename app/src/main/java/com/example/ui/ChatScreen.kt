package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.services.ScreenReaderService
import com.example.ui.theme.SalmonAccent
import com.example.ui.theme.SignalOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltronBackground
import com.example.ui.theme.UltronBorder
import com.example.ui.theme.UltronBorderSubtle
import com.example.ui.theme.UltronSurface
import com.example.ui.theme.UltronUserBubble
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChatMessage(
  val id: String = java.util.UUID.randomUUID().toString(),
  val text: String,
  val isUser: Boolean,
  val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
)

@Composable
fun ChatScreen(
  modifier: Modifier = Modifier,
  onNavigateToSettings: () -> Unit = {}
) {
  val coroutineScope = rememberCoroutineScope()
  val listState = rememberLazyListState()

  var inputText by remember { mutableStateOf("") }
  var isAiThinking by remember { mutableStateOf(false) }
  var activeTab by remember { mutableStateOf("chat") }

  var messages by remember {
    mutableStateOf(
      listOf(
        ChatMessage(
          text = "ULTRON Core initialized. System monitoring and voice telemetry are online. State your command or inquiry.",
          isUser = false
        )
      )
    )
  }

  // Scroll to bottom when messages update or when AI responds
  LaunchedEffect(messages.size, isAiThinking) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  fun sendMessage() {
    val trimmed = inputText.trim()
    if (trimmed.isEmpty() || isAiThinking) return

    val userMessage = ChatMessage(text = trimmed, isUser = true)
    messages = messages + userMessage
    inputText = ""
    isAiThinking = true

    // Check if recent screen text is available from ScreenReaderService (within last 30 seconds)
    val now = System.currentTimeMillis()
    val screenText = ScreenReaderService.lastCapturedText.trim()
    val isRecent = (now - ScreenReaderService.lastCapturedTimestamp) <= 30_000L

    val prompt = if (screenText.isNotEmpty() && isRecent) {
      "Current screen content: $screenText\n\nUser: $trimmed"
    } else {
      trimmed
    }

    coroutineScope.launch {
      try {
        val generativeModel = Firebase.ai.generativeModel("gemini-2.5-flash")
        val response = withContext(Dispatchers.IO) {
          generativeModel.generateContent(prompt)
        }
        val responseText = response.text?.trim()
        val finalAiText = if (!responseText.isNullOrEmpty()) {
          responseText
        } else {
          "SYSTEM // NO RESPONSE PAYLOAD RECEIVED"
        }
        messages = messages + ChatMessage(text = finalAiText, isUser = false)
      } catch (e: Exception) {
        messages = messages + ChatMessage(
          text = "SYSTEM // CONNECTION FAILED",
          isUser = false
        )
      } finally {
        isAiThinking = false
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(UltronBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
  ) {
    // 1. Top Bar: Wordmark + Telemetry Status Line
    TopBar()

    // 2. Scrollable Message Thread
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      items(messages, key = { it.id }) { message ->
        if (message.isUser) {
          UserMessageItem(message = message)
        } else {
          AiMessageItem(message = message)
        }
      }

      if (isAiThinking) {
        item(key = "thinking_indicator") {
          AiThinkingIndicator()
        }
      }
    }

    // 3. Bottom Input Bar
    BottomInputBar(
      inputText = inputText,
      onTextChanged = { inputText = it },
      onSend = { sendMessage() },
      onMicClick = {
        // Quick input toggle for testing
        inputText = "What is the current system status?"
      },
      isSendEnabled = inputText.trim().isNotEmpty() && !isAiThinking
    )

    // 4. Bottom Navigation (Chat / Settings)
    BottomNavigationBar(
      activeTab = activeTab,
      onTabSelected = { tab ->
        activeTab = tab
        if (tab == "settings") {
          onNavigateToSettings()
        }
      }
    )
  }
}

@Composable
private fun TopBar() {
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
        // Signal Orange Accent Square/Indicator
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

    // Status line: SYS.LINK_ACTIVE // UPTIME
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "SYS.LINK_ACTIVE // READY",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
        color = SignalOrange
      )
      Text(
        text = "UPTIME 99.8%",
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = TextMuted
      )
    }
  }
}

@Composable
private fun UserMessageItem(message: ChatMessage) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("message_item_user"),
    horizontalAlignment = Alignment.End
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 4.dp)
    ) {
      Text(
        text = message.timestamp,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = TextMuted
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "USER",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = SalmonAccent
      )
    }

    Box(
      modifier = Modifier
        .widthIn(max = 280.dp)
        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 2.dp))
        .background(UltronUserBubble)
        .border(1.dp, UltronBorder, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 14.dp, bottomEnd = 2.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Text(
        text = message.text,
        style = TextStyle(
          fontFamily = FontFamily.SansSerif,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          color = TextPrimary
        )
      )
    }
  }
}

@Composable
private fun AiMessageItem(message: ChatMessage) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("message_item_ai"),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(bottom = 4.dp)
    ) {
      Text(
        text = "ULTRON",
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = SignalOrange
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = message.timestamp,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        color = TextMuted
      )
    }

    // AI message: 1px orange left border, no background
    Box(
      modifier = Modifier
        .fillMaxWidth(0.92f)
        .drawBehind {
          // 1px Signal Orange left border
          drawLine(
            color = SignalOrange,
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 2.dp.toPx()
          )
        }
        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
    ) {
      Text(
        text = message.text,
        style = TextStyle(
          fontFamily = FontFamily.SansSerif,
          fontSize = 14.sp,
          lineHeight = 21.sp,
          letterSpacing = 0.2.sp,
          color = TextPrimary
        )
      )
    }
  }
}

@Composable
private fun AiThinkingIndicator() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Text(
      text = "ULTRON // PROCESSING",
      fontFamily = FontFamily.Monospace,
      fontSize = 10.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = 1.sp,
      color = SignalOrange
    )
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .drawBehind {
          drawLine(
            color = SignalOrange.copy(alpha = 0.5f),
            start = Offset(0f, 0f),
            end = Offset(0f, size.height),
            strokeWidth = 2.dp.toPx()
          )
        }
        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
      Text(
        text = "Awaiting subsystem response...",
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = TextSecondary
      )
    }
  }
}

@Composable
private fun BottomInputBar(
  inputText: String,
  onTextChanged: (String) -> Unit,
  onSend: () -> Unit,
  onMicClick: () -> Unit,
  isSendEnabled: Boolean
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(UltronBackground)
      .drawBehind {
        drawLine(
          color = UltronBorder,
          start = Offset(0f, 0f),
          end = Offset(size.width, 0f),
          strokeWidth = 1.dp.toPx()
        )
      }
      .padding(horizontal = 12.dp, vertical = 10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Text Input Box with 1px border
      Box(
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .background(UltronSurface, RoundedCornerShape(8.dp))
          .border(1.dp, UltronBorder, RoundedCornerShape(8.dp))
          .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        if (inputText.isEmpty()) {
          Text(
            text = "Type a command or message...",
            style = TextStyle(
              fontFamily = FontFamily.SansSerif,
              fontSize = 14.sp,
              color = TextMuted
            )
          )
        }
        BasicTextField(
          value = inputText,
          onValueChange = onTextChanged,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_input_field"),
          textStyle = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontSize = 14.sp,
            color = TextPrimary
          ),
          cursorBrush = SolidColor(SignalOrange),
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
          keyboardActions = KeyboardActions(onSend = { onSend() })
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Mic Icon Button
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(UltronSurface, RoundedCornerShape(8.dp))
          .border(1.dp, UltronBorder, RoundedCornerShape(8.dp))
          .clickable { onMicClick() }
          .testTag("mic_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Mic,
          contentDescription = "Voice Input",
          tint = SignalOrange,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Send Arrow Button
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(
            if (isSendEnabled) SignalOrange else UltronSurface,
            RoundedCornerShape(8.dp)
          )
          .border(
            1.dp,
            if (isSendEnabled) SignalOrange else UltronBorder,
            RoundedCornerShape(8.dp)
          )
          .clickable(enabled = isSendEnabled) { onSend() }
          .testTag("send_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send Message",
          tint = if (isSendEnabled) UltronBackground else TextMuted,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun BottomNavigationBar(
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
