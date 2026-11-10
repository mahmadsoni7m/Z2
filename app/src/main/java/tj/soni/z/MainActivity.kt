package tj.soni.z

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val DarkGreen = Color(0xFF102D24)
private val CardGreen = Color(0xFF1C4938)
private val Gold = Color(0xFFE8C875)
private val Bg = Color(0xFF0B1713)
private val UserBubble = Color(0xFF254F3E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = ZStore(this)
        setContent {
            ZTheme {
                ZChatScreen(
                    store = store,
                    onSend = { prompt, history -> GeminiClient(store.apiKey()).ask(prompt, history) }
                )
            }
        }
    }
}

@Composable
private fun ZTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold,
            secondary = CardGreen,
            background = Bg,
            surface = DarkGreen,
            onPrimary = DarkGreen,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@Composable
private fun ZChatScreen(
    store: ZStore,
    onSend: suspend (String, List<ChatMessage>) -> String
) {
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(store.loadMessages()) } }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var apiKey by remember { mutableStateOf(store.apiKey()) }
    var status by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun save() = store.saveMessages(messages.toList())

    Column(
        modifier = Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().background(DarkGreen).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(Gold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("Z", color = DarkGreen, fontSize = 25.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Z", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Gemini Assistant · v5.0", color = Gold, fontSize = 11.sp)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "Танзимот", tint = Gold)
            }
            IconButton(onClick = {
                messages.clear()
                save()
                status = "Суҳбати нав оғоз шуд"
            }) {
                Icon(Icons.Default.Add, contentDescription = "Суҳбати нав", tint = Color.White)
            }
        }

        if (messages.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Салом! Ман З ҳастам.", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Саволи худро нависед. Ман кӯмак мекунам.", color = Color.LightGray, fontSize = 14.sp)
                Spacer(Modifier.height(22.dp))
                Text("Масалан: Пойтахти Тоҷикистон кадом аст?", color = Gold, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { msg -> MessageBubble(msg) }
                if (busy) {
                    item {
                        Text("Z ҷавоб омода мекунад…", color = Gold, fontSize = 13.sp)
                    }
                }
            }
        }

        if (status.isNotBlank()) {
            Text(status, color = Gold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), fontSize = 12.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth().background(DarkGreen).padding(10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).background(Color(0xFF203B30), RoundedCornerShape(22.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Gold),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (input.isNotBlank() && !busy) {
                        val prompt = input.trim()
                        input = ""
                        status = ""
                        val history = messages.toList()
                        messages.add(ChatMessage("user", prompt))
                        save()
                        busy = true
                        scope.launch {
                            try {
                                val answer = onSend(prompt, history)
                                messages.add(ChatMessage("model", answer))
                                save()
                            } catch (e: Exception) {
                                messages.add(ChatMessage("model", "Хато: ${e.message ?: "Пайвастшавӣ иҷро нашуд"}. API Key ва интернетро санҷед."))
                                save()
                            } finally {
                                busy = false
                            }
                        }
                    }
                }),
                decorationBox = { inner ->
                    Box {
                        if (input.isEmpty()) Text("Саволи худро нависед…", color = Color(0xFFB5C5BC), fontSize = 14.sp)
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                enabled = input.isNotBlank() && !busy,
                onClick = {
                    val prompt = input.trim()
                    input = ""
                    status = ""
                    val history = messages.toList()
                    messages.add(ChatMessage("user", prompt))
                    save()
                    busy = true
                    scope.launch {
                        try {
                            val answer = onSend(prompt, history)
                            messages.add(ChatMessage("model", answer))
                            save()
                        } catch (e: Exception) {
                            messages.add(ChatMessage("model", "Хато: ${e.message ?: "Пайвастшавӣ иҷро нашуд"}. API Key ва интернетро санҷед."))
                            save()
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.size(50.dp).background(if (input.isNotBlank() && !busy) Gold else Color(0xFF52665B), CircleShape)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Ирсол", tint = DarkGreen)
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Танзимоти Gemini") },
            text = {
                Column {
                    Text("Gemini API Key", color = Gold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("API Key-ро гузоред") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Калид танҳо дар ҳамин телефон нигоҳ дошта мешавад. Онро ба касе нафиристед.", color = Color.LightGray, fontSize = 11.sp)
                    if (status.isNotBlank()) Text(status, color = Gold, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.saveApiKey(apiKey.trim())
                    status = if (apiKey.isBlank()) "API Key сабт нашуд" else "API Key сабт шуд"
                    showSettings = false
                }) { Text("Сабт кардан") }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) { Text("Бекор") }
            },
            containerColor = DarkGreen,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 310.dp)
                .background(if (isUser) UserBubble else CardGreen, RoundedCornerShape(18.dp))
                .padding(13.dp)
        ) {
            Text(if (isUser) "Шумо" else "Z", color = if (isUser) Color(0xFFDCECE2) else Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(message.text, color = Color.White, fontSize = 15.sp, lineHeight = 21.sp)
        }
    }
}
