package org.nayagram.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- Data Models ---
data class Chat(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isOnline: Boolean = false,
    val isVerified: Boolean = false,
    val category: String = "Personal",
    val avatarBg: Color = Color(0xFF0088CC)
)

data class Message(
    val id: String,
    val text: String,
    val time: String,
    val isFromMe: Boolean,
    val isRead: Boolean = true
)

data class CallLog(
    val id: String,
    val name: String,
    val time: String,
    val isMissed: Boolean,
    val isVideo: Boolean
)

// --- ViewModel ---
class NayaGramViewModel : ViewModel() {
    private val _chats = MutableStateFlow(
        listOf(
            Chat("1", "NayaGram Official Channel", "🔥 NayaGram v2.0 released with Anti-Delete & Turbo Speed!", "10:42 AM", unreadCount = 3, isPinned = true, isVerified = true, category = "Channels", avatarBg = Color(0xFF0088CC)),
            Chat("2", "Azmat Bhaiyya", "Bhai, APK build is completely tested and working!", "09:15 AM", isPinned = true, isOnline = true, category = "Personal", avatarBg = Color(0xFF2E7D32)),
            Chat("3", "Android Dev Group", "Check out Jetpack Compose M3 speed improvements", "08:30 AM", unreadCount = 12, category = "Groups", avatarBg = Color(0xFF1565C0)),
            Chat("4", "NayaGram Assistant Bot", "Type /help to see all available commands and features.", "Yesterday", isVerified = true, category = "Bots", avatarBg = Color(0xFF6A1B9A)),
            Chat("5", "Sarah Miller", "Let's schedule our sync call for tomorrow at 3 PM.", "Yesterday", isOnline = true, category = "Personal", avatarBg = Color(0xFFD81B60)),
            Chat("6", "Crypto & Tech News", "Bitcoin surpasses new landmark as market trends upward.", "Oct 12", category = "Channels", avatarBg = Color(0xFFE65100))
        )
    )
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _messages = MutableStateFlow<Map<String, List<Message>>>(
        mapOf(
            "1" to listOf(
                Message("m1", "Welcome to NayaGram Official Channel!", "10:30 AM", isFromMe = false),
                Message("m2", "🔥 NayaGram v2.0 released with Anti-Delete & Turbo Speed!", "10:42 AM", isFromMe = false)
            ),
            "2" to listOf(
                Message("m10", "Assalam-o-Alaikum Azmat Bhai!", "09:10 AM", isFromMe = true),
                Message("m11", "Walaikum Assalam! How is the project going?", "09:12 AM", isFromMe = false),
                Message("m12", "Bhai, APK build is completely tested and working!", "09:15 AM", isFromMe = false)
            )
        )
    )

    private val _selectedChat = MutableStateFlow<Chat?>(null)
    val selectedChat: StateFlow<Chat?> = _selectedChat.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _antiDeleteEnabled = MutableStateFlow(true)
    val antiDeleteEnabled: StateFlow<Boolean> = _antiDeleteEnabled.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _isSupported = MutableStateFlow(false)
    val isSupported: StateFlow<Boolean> = _isSupported.asStateFlow()

    val callLogs = listOf(
        CallLog("c1", "Azmat Bhaiyya", "Today, 09:16 AM", isMissed = false, isVideo = true),
        CallLog("c2", "Sarah Miller", "Yesterday, 04:20 PM", isMissed = true, isVideo = false),
        CallLog("c3", "Android Dev Group", "Oct 11, 02:00 PM", isMissed = false, isVideo = true)
    )

    fun selectChat(chat: Chat?) {
        _selectedChat.value = chat
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleAntiDelete(enabled: Boolean) {
        _antiDeleteEnabled.value = enabled
    }

    fun toggleDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
    }

    fun toggleSupport() {
        _isSupported.value = !_isSupported.value
    }

    fun sendMessage(chatId: String, text: String) {
        if (text.isBlank()) return
        val currentList = _messages.value[chatId] ?: emptyList()
        val newMessage = Message(
            id = System.currentTimeMillis().toString(),
            text = text,
            time = "Just now",
            isFromMe = true
        )
        _messages.value = _messages.value.toMutableMap().apply {
            put(chatId, currentList + newMessage)
        }
        // Update last message in chat list
        _chats.value = _chats.value.map {
            if (it.id == chatId) it.copy(lastMessage = text, time = "Just now") else it
        }
    }

    fun getMessagesForChat(chatId: String): List<Message> {
        return _messages.value[chatId] ?: emptyList()
    }
}

// --- Activity ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: NayaGramViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            val colorScheme = if (isDarkMode) {
                darkColorScheme(
                    primary = Color(0xFF2FA6DE),
                    onPrimary = Color.White,
                    surface = Color(0xFF1C2733),
                    background = Color(0xFF0F1721),
                    onSurface = Color.White
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF0088CC),
                    onPrimary = Color.White,
                    surface = Color.White,
                    background = Color(0xFFF0F2F5),
                    onSurface = Color(0xFF222222)
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppContent(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: NayaGramViewModel) {
    val selectedChat by viewModel.selectedChat.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) } // 0: Chats, 1: Calls, 2: Settings

    if (selectedChat != null) {
        BackHandler { viewModel.selectChat(null) }
        ChatDetailScreen(
            chat = selectedChat!!,
            viewModel = viewModel,
            onBack = { viewModel.selectChat(null) }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chats") },
                        label = { Text("Chats") },
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        modifier = Modifier.testTag("nav_chats")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Call, contentDescription = "Calls") },
                        label = { Text("Calls") },
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        modifier = Modifier.testTag("nav_calls")
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        modifier = Modifier.testTag("nav_settings")
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (activeTab) {
                    0 -> ChatListScreen(viewModel = viewModel)
                    1 -> CallsScreen(viewModel = viewModel)
                    2 -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: NayaGramViewModel) {
    val chats by viewModel.chats.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val categories = listOf("All", "Personal", "Groups", "Channels", "Bots")

    val filteredChats = chats.filter { chat ->
        val matchesCategory = if (selectedCategory == "All") true else chat.category == selectedCategory
        val matchesSearch = chat.name.contains(searchQuery, ignoreCase = true) ||
                chat.lastMessage.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesSearch
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "NayaGram Pro",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            },
            actions = {
                IconButton(onClick = { /* Search toggle */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
                }
                IconButton(onClick = { /* More options */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        // Search Input Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search messages or contacts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            } else null,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("search_input")
        )

        // Category Chips Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("chip_$category")
                )
            }
        }

        Divider(modifier = Modifier.padding(top = 4.dp), color = Color.LightGray.copy(alpha = 0.3f))

        // Chat List
        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No chats found in this folder.",
                    color = Color.Gray,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredChats) { chat ->
                    ChatItemRow(
                        chat = chat,
                        onClick = { viewModel.selectChat(chat) }
                    )
                    Divider(color = Color.LightGray.copy(alpha = 0.2f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun ChatItemRow(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("chat_item_${chat.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(chat.avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.take(1).uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (chat.isVerified) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = chat.time,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.lastMessage,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (chat.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = Color.Gray,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 4.dp)
                    )
                }

                if (chat.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = chat.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    chat: Chat,
    viewModel: NayaGramViewModel,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val messages = viewModel.getMessagesForChat(chat.id)

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Top Bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(chat.avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chat.name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = chat.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (chat.isOnline) "online" else "last seen recently",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { /* Call */ }) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = Color.White)
                }
                IconButton(onClick = { /* Video Call */ }) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        // Messages Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
            }
        }

        // Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Write a message...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("message_input"),
                shape = RoundedCornerShape(24.dp),
                trailingIcon = {
                    IconButton(onClick = { /* Attachment */ }) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach File")
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(chat.id, inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.size(48.dp).testTag("btn_send"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(message: Message) {
    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isFromMe) Color(0xFFE3F2FD) else Color.White
    val textColor = Color(0xFF111111)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.time,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                if (message.isFromMe) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(viewModel: NayaGramViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Calls", fontWeight = FontWeight.Bold, color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(viewModel.callLogs) { call ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (call.isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = if (call.isMissed) Color.Red else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = call.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = call.time,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = { /* Start Call */ }) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Divider(color = Color.LightGray.copy(alpha = 0.2f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: NayaGramViewModel) {
    val antiDeleteEnabled by viewModel.antiDeleteEnabled.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
        )

        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            item {
                // Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("N", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("NayaGram Pro User", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("+92 300 1234567", fontSize = 14.sp, color = Color.Gray)
                            Text("@nayagram_pro", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "NayaGram Pro Exclusive Features",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anti-Delete Messages", fontWeight = FontWeight.Medium)
                        Text("Prevent senders from deleting messages in your chat", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = antiDeleteEnabled,
                        onCheckedChange = { viewModel.toggleAntiDelete(it) },
                        modifier = Modifier.testTag("switch_antidelete")
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Night Mode", fontWeight = FontWeight.Medium)
                        Text("Dark theme for comfortable reading at night", fontSize = 12.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        modifier = Modifier.testTag("switch_darkmode")
                    )
                }

                val isSupported by viewModel.isSupported.collectAsState()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { viewModel.toggleSupport() },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSupported) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSupported) Icons.Default.CheckCircle else Icons.Default.Favorite,
                            contentDescription = "Support",
                            tint = if (isSupported) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSupported) "Supported" else "Support NayaGram",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isSupported) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isSupported) "Thank you for supporting NayaGram Platform!" else "Tap to show support for NayaGram development.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Button(
                            onClick = { viewModel.toggleSupport() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSupported) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("btn_support")
                        ) {
                            Text(if (isSupported) "Supported" else "Support")
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "General Settings",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                SettingsRow(icon = Icons.Default.Lock, title = "Privacy & Security")
                SettingsRow(icon = Icons.Default.Folder, title = "Chat Folders")
                SettingsRow(icon = Icons.Default.Storage, title = "Data & Storage")
                SettingsRow(icon = Icons.Default.Language, title = "Language (English)")
                SettingsRow(icon = Icons.Default.Info, title = "About NayaGram Pro v2.0")
            }
        }
    }
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
