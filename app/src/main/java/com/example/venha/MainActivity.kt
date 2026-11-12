package com.example.venha

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Background = Color(0xFF080B14)
private val CardTop = Color(0xFF181A2C)
private val CardBottom = Color(0xFF0D121E)
private val Lavender = Color(0xFFD7CEFF)
private val Muted = Color(0xFFA7A9BA)
private val Accent = Color(0xFFA98DFF)
private val VietnamZone = ZoneId.of("Asia/Ho_Chi_Minh")
private val DateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
private const val TWELVE_HOURS_SECONDS = 12L * 60L * 60L

private val AppColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF171221),
    secondary = Color(0xFFFFD18B),
    background = Background,
    surface = CardTop,
    onSurface = Color(0xFFF5F6FB),
)

class MainActivity : ComponentActivity() {
    private var startupError by mutableStateOf<String?>(null)
    private var firebaseAuth by mutableStateOf<FirebaseAuth?>(null)
    private var database by mutableStateOf<FirebaseDatabase?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8, 11, 20)
        window.navigationBarColor = android.graphics.Color.rgb(8, 11, 20)
        initializeFirebase()

        setContent {
            MaterialTheme(colorScheme = AppColors) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    val auth = firebaseAuth
                    val db = database
                    when {
                        startupError != null -> SetupRequiredScreen(startupError.orEmpty())
                        auth != null && db != null -> AuthenticatedApp(auth, db)
                        else -> SetupRequiredScreen("Đang khởi tạo Firebase…")
                    }
                }
            }
        }
    }

    private fun initializeFirebase() {
        val requiredValues = mapOf(
            "FIREBASE_API_KEY" to BuildConfig.FIREBASE_API_KEY,
            "FIREBASE_APP_ID" to BuildConfig.FIREBASE_APP_ID,
            "FIREBASE_PROJECT_ID" to BuildConfig.FIREBASE_PROJECT_ID,
            "FIREBASE_DATABASE_URL" to BuildConfig.FIREBASE_DATABASE_URL,
            "FIREBASE_SENDER_ID" to BuildConfig.FIREBASE_SENDER_ID,
        )
        val missing = requiredValues.filterValues(String::isBlank).keys
        if (missing.isNotEmpty()) {
            startupError = "Thiếu cấu hình Firebase: ${missing.joinToString()}. " +
                "Tạo firebase.properties từ firebase.properties.example rồi nhập cấu hình Firebase."
            return
        }

        try {
            val options = FirebaseOptions.Builder()
                .setApiKey(BuildConfig.FIREBASE_API_KEY)
                .setApplicationId(BuildConfig.FIREBASE_APP_ID)
                .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
                .setDatabaseUrl(BuildConfig.FIREBASE_DATABASE_URL)
                .setStorageBucket(BuildConfig.FIREBASE_STORAGE_BUCKET)
                .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
                .build()
            val app = FirebaseApp.getApps(this)
                .firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }
                ?: FirebaseApp.initializeApp(this, options)
                ?: throw IllegalStateException("Không thể khởi tạo Firebase.")
            firebaseAuth = FirebaseAuth.getInstance(app)
            database = FirebaseDatabase.getInstance(app, BuildConfig.FIREBASE_DATABASE_URL)
        } catch (error: IllegalArgumentException) {
            startupError = "Cấu hình Firebase không hợp lệ: ${error.localizedMessage ?: "hãy kiểm tra lại firebase.properties."}"
        } catch (error: IllegalStateException) {
            startupError = "Không khởi tạo được Firebase: ${error.localizedMessage ?: "hãy kiểm tra cấu hình dự án."}"
        }
    }
}

private data class Milestone(
    val id: String,
    val title: String,
    val timestampMillis: Long,
    val mode: String,
    val createdAt: Long,
)

private enum class Page { HOME, CREATE, DETAIL }

@Composable
private fun AuthenticatedApp(auth: FirebaseAuth, database: FirebaseDatabase) {
    var user by remember(auth) { mutableStateOf(auth.currentUser) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val currentUser = user
    if (currentUser == null) {
        AuthScreen(auth)
    } else {
        MilestoneApp(auth, database, currentUser)
    }
}

@Composable
private fun AuthScreen(auth: FirebaseAuth) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(CardTop, CardBottom)))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppMark()
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Đăng nhập",
                color = Color(0xFFF5F6FB),
                fontSize = 27.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Đăng nhập để đồng bộ các mốc thời gian của bạn",
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mật khẩu") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )
            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                InlineMessage(it, isError = true)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        errorMessage = "Nhập email và mật khẩu."
                    } else {
                        busy = true
                        errorMessage = null
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { result ->
                                busy = false
                                if (!result.isSuccessful) {
                                    errorMessage = authError(result.exception)
                                }
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
            ) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Đăng nhập")
                }
            }
        }
    }
}

@Composable
private fun MilestoneApp(auth: FirebaseAuth, database: FirebaseDatabase, user: FirebaseUser) {
    var page by remember { mutableStateOf(Page.HOME) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var milestones by remember { mutableStateOf<List<Milestone>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val milestoneRef = remember(database, user.uid) {
        database.reference.child("users").child(user.uid).child("milestones")
    }

    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1_000)
        }
    }

    DisposableEffect(milestoneRef) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                milestones = snapshot.children.mapNotNull { child ->
                    val title = child.child("title").getValue(String::class.java) ?: return@mapNotNull null
                    val timestamp = child.child("timestampMillis").getValue(Long::class.java) ?: return@mapNotNull null
                    val mode = child.child("mode").getValue(String::class.java) ?: return@mapNotNull null
                    val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                    val id = child.key ?: return@mapNotNull null
                    Milestone(id, title, timestamp, mode, createdAt)
                }.sortedByDescending { it.createdAt }
                loading = false
                message = null
            }

            override fun onCancelled(error: DatabaseError) {
                loading = false
                message = "Không tải được dữ liệu: ${error.message}"
            }
        }
        milestoneRef.addValueEventListener(listener)
        onDispose { milestoneRef.removeEventListener(listener) }
    }

    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
        ) {
            AppHeader(
                email = user.email.orEmpty(),
                onSignOut = { auth.signOut(); page = Page.HOME },
            )
            Spacer(Modifier.height(22.dp))

            when (page) {
                Page.HOME -> HomeScreen(
                    milestones = milestones,
                    loading = loading,
                    nowMillis = nowMillis,
                    message = message,
                    modifier = Modifier.weight(1f),
                    onCreate = { message = null; page = Page.CREATE },
                    onSelect = { id -> selectedId = id; message = null; page = Page.DETAIL },
                )
                Page.CREATE -> CreateMilestoneScreen(
                    saving = saving,
                    message = message,
                    onBack = { message = null; page = Page.HOME },
                    onSave = { title, timestamp, mode ->
                        val key = milestoneRef.push().key
                        if (key == null) {
                            message = "Không tạo được mã cho mốc thời gian. Thử lại."
                        } else {
                            saving = true
                            milestoneRef.child(key).setValue(
                                mapOf(
                                    "title" to title,
                                    "timestampMillis" to timestamp,
                                    "mode" to mode,
                                    "createdAt" to System.currentTimeMillis(),
                                ),
                            ).addOnCompleteListener { result ->
                                saving = false
                                if (result.isSuccessful) {
                                    selectedId = key
                                    message = null
                                    page = Page.DETAIL
                                } else {
                                    message = "Không lưu được mốc: ${result.exception?.localizedMessage ?: "hãy thử lại."}"
                                }
                            }
                        }
                    },
                )
                Page.DETAIL -> {
                    val selected = milestones.firstOrNull { it.id == selectedId }
                    if (selected == null) {
                        HomeScreen(
                            milestones = milestones,
                            loading = loading,
                            nowMillis = nowMillis,
                            message = message,
                            modifier = Modifier.weight(1f),
                            onCreate = { page = Page.CREATE },
                            onSelect = { id -> selectedId = id; page = Page.DETAIL },
                        )
                    } else {
                        MilestoneDetailScreen(
                            milestone = selected,
                            nowMillis = nowMillis,
                            message = message,
                            onBack = { message = null; page = Page.HOME },
                            onDelete = {
                                milestoneRef.child(selected.id).removeValue().addOnCompleteListener { result ->
                                    if (result.isSuccessful) {
                                        selectedId = null
                                        message = null
                                        page = Page.HOME
                                    } else {
                                        message = "Không xóa được mốc: ${result.exception?.localizedMessage ?: "hãy thử lại."}"
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    milestones: List<Milestone>,
    loading: Boolean,
    nowMillis: Long,
    message: String?,
    modifier: Modifier,
    onCreate: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Mốc thời gian của bạn", color = Color(0xFFF5F6FB), fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Theo dõi thời gian đã qua hoặc còn lại đến một sự kiện.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
            Text("+  Tạo mốc thời gian")
        }
        message?.let {
            Spacer(Modifier.height(12.dp))
            InlineMessage(it, isError = true)
        }
        Spacer(Modifier.height(18.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            milestones.isEmpty() -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦", color = Lavender, fontSize = 38.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Chưa có mốc nào", color = Color(0xFFF5F6FB), fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("Tạo mốc đầu tiên để bắt đầu theo dõi.", color = Muted, fontSize = 13.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(milestones, key = { it.id }) { milestone ->
                    MilestoneCard(milestone, nowMillis) { onSelect(milestone.id) }
                }
            }
        }
    }
}

@Composable
private fun MilestoneCard(milestone: Milestone, nowMillis: Long, onClick: () -> Unit) {
    val date = Instant.ofEpochMilli(milestone.timestampMillis)
        .atZone(VietnamZone)
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")))
    val delta = if (milestone.mode == "since") nowMillis - milestone.timestampMillis else milestone.timestampMillis - nowMillis
    val status = when {
        milestone.mode == "since" && delta >= 0 -> "Đã qua · ${formatCompactDuration(delta)}"
        milestone.mode == "since" -> "Bắt đầu sau · ${formatCompactDuration(-delta)}"
        delta >= 0 -> "Còn lại · ${formatCompactDuration(delta)}"
        else -> "Đã qua mốc · ${formatCompactDuration(-delta)}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(CardTop, CardBottom)))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (milestone.mode == "since") "◷" else "⌛", color = Lavender, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(milestone.title, color = Color(0xFFF5F6FB), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(date, color = Muted, fontSize = 12.sp)
            }
            Text("›", color = Muted, fontSize = 24.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(status, color = if (milestone.mode == "until" && delta >= 0) Color(0xFFFFD18B) else Lavender, fontSize = 13.sp)
    }
}

@Composable
private fun CreateMilestoneScreen(
    saving: Boolean,
    message: String?,
    onBack: () -> Unit,
    onSave: (String, Long, String) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var title by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now(VietnamZone)) }
    var selectedTime by remember { mutableStateOf(LocalTime.now(VietnamZone).withSecond(0).withNano(0)) }
    var mode by remember { mutableStateOf("since") }
    var localError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("‹  Quay lại") }
        Text("Tạo mốc mới", color = Color(0xFFF5F6FB), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("Chọn sự kiện và thời điểm cần theo dõi.", color = Muted, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(60); localError = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tên mốc") },
            placeholder = { Text("Ví dụ: Ngày bắt đầu công việc") },
            singleLine = true,
        )
        Spacer(Modifier.height(18.dp))
        Text("Cách tính", color = Color(0xFFE9E6F3), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (mode == "since") {
                Button(onClick = { mode = "since" }, modifier = Modifier.weight(1f)) { Text("Đếm từ mốc") }
            } else {
                OutlinedButton(onClick = { mode = "since" }, modifier = Modifier.weight(1f)) { Text("Đếm từ mốc") }
            }
            if (mode == "until") {
                Button(onClick = { mode = "until" }, modifier = Modifier.weight(1f)) { Text("Đếm đến mốc") }
            } else {
                OutlinedButton(onClick = { mode = "until" }, modifier = Modifier.weight(1f)) { Text("Đếm đến mốc") }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Thời điểm (giờ Việt Nam)", color = Color(0xFFE9E6F3), fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day -> selectedDate = LocalDate.of(year, month + 1, day) },
                        selectedDate.year,
                        selectedDate.monthValue - 1,
                        selectedDate.dayOfMonth,
                    ).show()
                },
                modifier = Modifier.weight(1f),
            ) { Text("▦  ${selectedDate.format(DateFormatter)}") }
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> selectedTime = LocalTime.of(hour, minute) },
                        selectedTime.hour,
                        selectedTime.minute,
                        true,
                    ).show()
                },
                modifier = Modifier.weight(1f),
            ) { Text("◷  ${selectedTime.format(TimeFormatter)}") }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (mode == "since") "Theo dõi thời gian đã trôi qua từ mốc này." else "Đếm ngược đến thời điểm đã chọn.",
            color = Muted,
            fontSize = 13.sp,
        )
        (localError ?: message)?.let {
            Spacer(Modifier.height(14.dp))
            InlineMessage(it, isError = true)
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                if (title.isBlank()) {
                    localError = "Hãy đặt tên cho mốc thời gian."
                } else {
                    val timestamp = selectedDate.atTime(selectedTime).atZone(VietnamZone).toInstant().toEpochMilli()
                    onSave(title.trim(), timestamp, mode)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving,
        ) {
            if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Lưu mốc")
        }
    }
}

@Composable
private fun MilestoneDetailScreen(
    milestone: Milestone,
    nowMillis: Long,
    message: String?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val localTime = Instant.ofEpochMilli(milestone.timestampMillis).atZone(VietnamZone)
    val delta = if (milestone.mode == "since") nowMillis - milestone.timestampMillis else milestone.timestampMillis - nowMillis
    val active = delta >= 0
    val parts = splitDuration(kotlin.math.abs(delta))
    val title = when {
        milestone.mode == "since" && active -> "Thời gian đã trôi qua"
        milestone.mode == "since" -> "Thời gian đến khi bắt đầu"
        active -> "Thời gian còn lại"
        else -> "Thời gian sau mốc"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TextButton(onClick = onBack) { Text("‹  Tất cả mốc") }
        Text(milestone.title, color = Color(0xFFF5F6FB), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${localTime.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy · HH:mm", Locale.forLanguageTag("vi-VN")))} · Giờ Việt Nam",
            color = Muted,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(CardTop, CardBottom)))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, color = Muted, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeUnitCard(parts.days.toString(), "ngày", Modifier.weight(1f))
                TimeUnitCard(parts.hours.toString().padStart(2, '0'), "giờ", Modifier.weight(1f))
                TimeUnitCard(parts.minutes.toString().padStart(2, '0'), "phút", Modifier.weight(1f))
                TimeUnitCard(parts.seconds.toString().padStart(2, '0'), "giây", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (milestone.mode == "since") "ĐẾM TỪ MỐC" else "ĐẾM ĐẾN MỐC",
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
        }

        if (milestone.mode == "since") {
            Spacer(Modifier.height(20.dp))
            val periods = if (active) calculateDayNight(milestone.timestampMillis, nowMillis) else DayNightElapsed(0, 0)
            Text("Tách theo khung giờ", color = Color(0xFFF5F6FB), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DayNightCard("☀  Ban ngày", periods.day, "06:00 – 18:00", Modifier.weight(1f))
                DayNightCard("☾  Ban đêm", periods.night, "18:00 – 06:00", Modifier.weight(1f))
            }
            if (!active) {
                Spacer(Modifier.height(8.dp))
                Text("Mốc này chưa bắt đầu; phần tách ngày/đêm sẽ hiện khi đến thời điểm.", color = Muted, fontSize = 12.sp)
            }
        }

        message?.let {
            Spacer(Modifier.height(14.dp))
            InlineMessage(it, isError = true)
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Xóa mốc này", color = Color(0xFFFF9B9B))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa mốc thời gian?") },
            text = { Text("“${milestone.title}” sẽ bị xóa khỏi tài khoản của bạn.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Hủy") }
            },
        )
    }
}

@Composable
private fun TimeUnitCard(value: String, unit: String, modifier: Modifier) {
    val valueFontSize = (22 - (value.length - 2).coerceAtLeast(0) * 2)
        .coerceAtLeast(14)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Lavender, fontSize = valueFontSize.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(unit, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun DayNightCard(title: String, value: PeriodValue, range: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(title, color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Text(value.periods.toString(), color = Lavender, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("chu kỳ 12 giờ", color = Color(0xFFE9E6F3), fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text("+ ${formatDuration(value.remainderSeconds)}", color = Color(0xFFC9C4DF), fontSize = 12.sp)
        Text(range, color = Color(0xFF777D91), fontSize = 10.sp)
    }
}

@Composable
private fun AppHeader(email: String, onSignOut: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppMark()
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Đếm ngày đêm", color = Color(0xFFF5F6FB), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(email, color = Muted, fontSize = 12.sp, maxLines = 1)
        }
        TextButton(onClick = onSignOut) { Text("Thoát") }
    }
}

@Composable
private fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF27204A), Background),
                    center = androidx.compose.ui.geometry.Offset(520f, 0f),
                    radius = 900f,
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun SetupRequiredScreen(message: String) {
    AppBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(CardTop, CardBottom)))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppMark()
            Spacer(Modifier.height(18.dp))
            Text("Cần cấu hình Firebase", color = Color(0xFFF5F6FB), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(message, color = Muted, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InlineMessage(message: String, isError: Boolean) {
    Text(
        text = message,
        color = if (isError) Color(0xFFFFA6A6) else Color(0xFFB8E6C7),
        fontSize = 13.sp,
    )
}

@Composable
private fun AppMark() {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF51427F), Color(0xFF25243B))))
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("☀︎☾", color = Lavender, fontSize = 23.sp)
    }
}

private data class DurationParts(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

private fun splitDuration(durationMillis: Long): DurationParts {
    var seconds = durationMillis / 1_000
    val days = seconds / 86_400
    seconds %= 86_400
    val hours = seconds / 3_600
    seconds %= 3_600
    val minutes = seconds / 60
    seconds %= 60
    return DurationParts(days, hours, minutes, seconds)
}

private fun formatCompactDuration(durationMillis: Long): String {
    val parts = splitDuration(durationMillis)
    return when {
        parts.days > 0 -> "${parts.days} ngày ${parts.hours} giờ"
        parts.hours > 0 -> "${parts.hours} giờ ${parts.minutes} phút"
        parts.minutes > 0 -> "${parts.minutes} phút"
        else -> "${parts.seconds} giây"
    }
}

private data class PeriodValue(val periods: Long, val remainderSeconds: Long)
private data class DayNightElapsed(val day: PeriodValue, val night: PeriodValue) {
    constructor(daySeconds: Long, nightSeconds: Long) : this(
        PeriodValue(daySeconds / TWELVE_HOURS_SECONDS, daySeconds % TWELVE_HOURS_SECONDS),
        PeriodValue(nightSeconds / TWELVE_HOURS_SECONDS, nightSeconds % TWELVE_HOURS_SECONDS),
    )
}

private fun calculateDayNight(fromMillis: Long, untilMillis: Long): DayNightElapsed {
    if (untilMillis <= fromMillis) return DayNightElapsed(0, 0)
    var cursor = Instant.ofEpochMilli(fromMillis)
    val end = Instant.ofEpochMilli(untilMillis)
    var daylightSeconds = 0L
    var nighttimeSeconds = 0L

    while (cursor < end) {
        val local = cursor.atZone(VietnamZone)
        val time = local.toLocalTime()
        val isDaylight = time >= LocalTime.of(6, 0) && time < LocalTime.of(18, 0)
        val boundaryDate = when {
            isDaylight || time < LocalTime.of(6, 0) -> local.toLocalDate()
            else -> local.toLocalDate().plusDays(1)
        }
        val boundaryTime = if (isDaylight) LocalTime.of(18, 0) else LocalTime.of(6, 0)
        val boundary = boundaryDate.atTime(boundaryTime).atZone(VietnamZone).toInstant()
        val segmentEnd = minOf(end, boundary)
        val seconds = segmentEnd.epochSecond - cursor.epochSecond
        if (isDaylight) daylightSeconds += seconds else nighttimeSeconds += seconds
        cursor = segmentEnd
    }
    return DayNightElapsed(daylightSeconds, nighttimeSeconds)
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun authError(exception: Exception?): String {
    val code = (exception as? FirebaseAuthException)?.errorCode
    return when (code) {
        "ERROR_INVALID_EMAIL" -> "Địa chỉ email không hợp lệ."
        "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "Email hoặc mật khẩu không chính xác."
        "ERROR_USER_NOT_FOUND" -> "Không tìm thấy tài khoản này."
        "ERROR_NETWORK_REQUEST_FAILED" -> "Không thể kết nối mạng. Kiểm tra kết nối rồi thử lại."
        else -> exception?.localizedMessage ?: "Không thể xác thực. Vui lòng thử lại."
    }
}
