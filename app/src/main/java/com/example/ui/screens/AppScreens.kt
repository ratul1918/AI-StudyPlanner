package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.FlashcardEntity
import com.example.data.database.NoteEntity
import com.example.data.database.SummaryEntity
import com.example.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

// --- Vibrant Palette Custom Theme Colors ---
val Slate900 = Color(0xFFFBF8FD)    // Main soft lilac background
val Slate800 = Color(0xFFFFFFFF)    // Pure white card background
val Indigo950 = Color(0xFFE8DEF8)   // Soft lilac transition or accent container
val Indigo900 = Color(0xFF6750A4)   // Vibrant deep theme purple as primary
val AccentCyan = Color(0xFF6750A4)  // Accent purple links/highlights
val SoftPurple = Color(0xFFD0BCFF)  // Lighter purple details
val GridWhite = Color(0xFF1C1B1F)   // Crisp charcoal dark text readability


sealed class ActiveScreen {
    object Dashboard : ActiveScreen()
    object Notes : ActiveScreen()
    object Summaries : ActiveScreen()
    object Chat : ActiveScreen()
    object Quizzes : ActiveScreen()
    object Flashcards : ActiveScreen()
    object Planner : ActiveScreen()
    object Research : ActiveScreen()
    object Progress : ActiveScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainStudyScreen(viewModel: StudyViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    var currentTab by remember { mutableStateOf<ActiveScreen>(ActiveScreen.Dashboard) }
    var showNotifsDialog by remember { mutableStateOf(false) }
    val notificationsList by viewModel.notifications.collectAsState()

    // Base background layout with slate-indigo gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Slate900, Indigo950)
                )
            )
    ) {
        if (currentUser == null) {
            AuthScreen(
                onLogin = { email, name -> viewModel.login(email, name) },
                isLoading = viewModel.isAuthenticating.collectAsState().value,
                errorMsg = viewModel.authError.collectAsState().value
            )
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Slate900.copy(alpha = 0.95f),
                            titleContentColor = GridWhite
                        ),
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "AI Study",
                                    tint = AccentCyan,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "AI Study Assistant",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        },
                        actions = {
                            // Notification Alert Icon (Module 12)
                            IconButton(onClick = { showNotifsDialog = true }) {
                                Box {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Reminders",
                                        tint = AccentCyan
                                    )
                                    if (notificationsList.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .align(Alignment.TopEnd)
                                                .background(Color.Red, CircleShape)
                                        )
                                    }
                                }
                            }
                            // Logout button
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Logout",
                                    tint = Indigo900
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    StudyNavigationBar(
                        activeScreen = currentTab,
                        onScreenSelected = { currentTab = it }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        ActiveScreen.Dashboard -> DashboardScreen(viewModel, onNavigate = { currentTab = it })
                        ActiveScreen.Notes -> NotesManagerScreen(viewModel)
                        ActiveScreen.Summaries -> SummariesScreen(viewModel)
                        ActiveScreen.Chat -> ChatWithNotesScreen(viewModel)
                        ActiveScreen.Quizzes -> InteractiveQuizScreen(viewModel)
                        ActiveScreen.Flashcards -> FlashcardTrainerScreen(viewModel)
                        ActiveScreen.Planner -> StudyPlannerScreen(viewModel)
                        ActiveScreen.Research -> PaperAssistantScreen(viewModel)
                        ActiveScreen.Progress -> ProgressVisualizationScreen(viewModel)
                    }
                }
            }
        }

        // Module 12: Notification / Reminders Dialog Center
        if (showNotifsDialog) {
            Dialog(onDismissRequest = { showNotifsDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Notifications, "Alerts", tint = AccentCyan)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Alerts & Countdown Hub",
                                fontWeight = FontWeight.Bold,
                                color = GridWhite,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        
                        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                            items(notificationsList) { notif ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(notif.title, fontWeight = FontWeight.Bold, color = AccentCyan)
                                    Text(notif.description, color = GridWhite, fontSize = 14.sp)
                                    Divider(color = Slate900.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }

                        Button(
                            onClick = { showNotifsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo900),
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 12.dp)
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }
    }
}

// --- Module 1: Auth Screen ---
@Composable
fun AuthScreen(onLogin: (String, String) -> Unit, isLoading: Boolean, errorMsg: String?) {
    var email by remember { mutableStateOf("rafiurrahman1918@gmail.com") }
    var name by remember { mutableStateOf("rafiur") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = AccentCyan,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "AI Study Assistant",
            color = GridWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )
        Text(
            "Smart Learning and Exam Preparation",
            color = Indigo900,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name", color = Indigo900) },
            textStyle = TextStyle(color = GridWhite),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = SoftPurple
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Indigo900) },
            textStyle = TextStyle(color = GridWhite),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = SoftPurple
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input")
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = AccentCyan)
        } else {
            Button(
                onClick = { onLogin(email, name) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button")
            ) {
                Text("Secure Login & Sync UI", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        if (errorMsg != null) {
            Text(errorMsg, color = Color.Red, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

// --- Module 2: Dashboard Screen ---
@Composable
fun DashboardScreen(viewModel: StudyViewModel, onNavigate: (ActiveScreen) -> Unit) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val notes by viewModel.notesList.collectAsState()
    val rawProgress by viewModel.progressHistory.collectAsState()
    val flashcards by viewModel.flashcardList.collectAsState()

    // Calculate metrics
    val streakCount = 4
    val hoursStudied = rawProgress.sumOf { it.studyMinutes } / 60f
    val accuracy = if (rawProgress.isNotEmpty()) rawProgress.map { it.quizAccuracy }.average().toFloat() else 85.0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak & Progress Card (Vibrant Gradient Theme)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF6750A4), Color(0xFFD0BCFF))
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("Current Streak", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("12 Days 🔥", color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Exam: June 15", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Daily Goal: 4/5 hrs", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text("80%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.8f) // 80% progress
                                .fillMaxHeight()
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // Quick Actions (Vibrant Palette Mockup Row)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                QuickActionButton(
                    icon = Icons.Default.CloudUpload,
                    label = "Upload",
                    bgColor = Color(0xFFFFEDD5), // Orange-100
                    iconColor = Color(0xFFEA580C), // Orange-600
                    onClick = { onNavigate(ActiveScreen.Notes) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Forum,
                    label = "Ask AI",
                    bgColor = Color(0xFFE0E7FF), // Indigo-100
                    iconColor = Color(0xFF4F46E5), // Indigo-600
                    onClick = { onNavigate(ActiveScreen.Chat) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Quiz,
                    label = "Quiz",
                    bgColor = Color(0xFFD1FAE5), // Emerald-100
                    iconColor = Color(0xFF059669), // Emerald-600
                    onClick = { onNavigate(ActiveScreen.Quizzes) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Style,
                    label = "Cards",
                    bgColor = Color(0xFFFFE4E6), // Rose-100
                    iconColor = Color(0xFFE11D48), // Rose-600
                    onClick = { onNavigate(ActiveScreen.Flashcards) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Metrics Widget Row (Module 2 Dashboard)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricWidget(
                    title = "Total Notes",
                    value = notes.size.toString(),
                    icon = Icons.Default.Book,
                    modifier = Modifier.weight(1f)
                )
                MetricWidget(
                    title = "Flashcards",
                    value = flashcards.size.coerceAtLeast(3).toString(),
                    icon = Icons.Default.Style,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricWidget(
                    title = "Study Hours",
                    value = String.format("%.1f hrs", hoursStudied.coerceAtLeast(3.2f)),
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )
                MetricWidget(
                    title = "Study Streak",
                    value = "🔥 $streakCount Days",
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Active Note Selector Details
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = BorderStroke(1.dp, Color(0xFFE8DEF8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Note Context", fontWeight = FontWeight.Bold, color = GridWhite)
                        AssistChip(
                            onClick = { onNavigate(ActiveScreen.Notes) },
                            label = { Text("Change notes") },
                            colors = AssistChipDefaults.assistChipColors(
                                labelColor = Indigo900,
                                leadingIconContentColor = Indigo900
                            ),
                            border = BorderStroke(1.dp, SoftPurple)
                        )
                    }

                    selectedNote?.let { note ->
                        Text(note.title, fontWeight = FontWeight.Bold, color = Indigo900, fontSize = 18.sp)
                        Text(
                            text = note.contentText,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = GridWhite.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = { onNavigate(ActiveScreen.Summaries) },
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo900, contentColor = Color.White)
                            ) {
                                Text("Summary")
                            }
                            Button(
                                onClick = { onNavigate(ActiveScreen.Chat) },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple, contentColor = Indigo900)
                            ) {
                                Text("Ask Chat")
                            }
                        }
                    } ?: Text(
                        text = "No notes uploaded. Please add one!",
                        color = Color(0xFFEA580C),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // AI Recommendations & Tips
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Indigo950.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, Indigo900.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Tips",
                        tint = Indigo900,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("AI Study Strategy Tip", fontWeight = FontWeight.Bold, color = GridWhite)
                        Text(
                            text = "Based on CPU Scheduling quizzes, try active recall on 'Round Robin vs FCFS' today! It is predicted to be 30% of your exam grade.",
                            fontSize = 13.sp,
                            color = GridWhite.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricWidget(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = BorderStroke(1.dp, Color(0xFFE8DEF8)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Indigo900, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, color = GridWhite, fontSize = 20.sp)
            Text(title, color = GridWhite.copy(alpha = 0.7f), fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

// --- Module 3: Notes Management Screen & OCR Uploads ---
@Composable
fun NotesManagerScreen(viewModel: StudyViewModel) {
    val notes by viewModel.notesList.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("Computer Science") }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    
    // OCR Simulation inputs
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()
    val ocrTextResult by viewModel.ocrText.collectAsState()

    // Folder listing
    val folders = listOf("All Notes", "Computer Science", "Research Papers", "OCR Extracts")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Resource Library", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Organize and manage your PDFs, scanned notes, and images", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        // Folder Row Tab Selectors
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(folders) { folder ->
                val isSelected = (selectedFolder == folder)
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectFolder(folder) },
                    label = { Text(folder) },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = GridWhite,
                        selectedLabelColor = Color.White,
                        selectedContainerColor = AccentCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Large Create/Upload Floating button trigger
        Button(
            onClick = { showAddDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Lecture Notes / Run OCR Scanner", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Notes List Container
        val filteredList = if (selectedFolder == "All Notes") notes else notes.filter { it.folderName == selectedFolder }
        
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No notes found in '$selectedFolder'. Click Add above!", color = Color(0xFFEA580C))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList) { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        border = BorderStroke(1.dp, Color(0xFFE8DEF8)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectNote(note) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(note.title, fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 16.sp)
                                IconButton(onClick = { viewModel.deleteNote(note.noteId) }) {
                                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f))
                                }
                            }
                            Text("Folder: ${note.folderName} | ${note.fileUrl}", color = SoftPurple, fontSize = 12.sp)
                            Text(
                                text = note.contentText,
                                color = GridWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Dialog for adding notes manually or running the simulated OCR System (Module 4)
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Add Study Materials", fontWeight = FontWeight.Bold, color = GridWhite, fontSize = 20.sp)
                        
                        // Option A: Raw Input
                        Text("Option 1: Type or Paste Lecture Content", color = Indigo900, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = noteTitleInput,
                            onValueChange = { noteTitleInput = it },
                            label = { Text("Note Title", color = GridWhite) },
                            textStyle = TextStyle(color = GridWhite),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = noteContentInput,
                            onValueChange = { noteContentInput = it },
                            label = { Text("Lecture Notes Content Text...", color = GridWhite) },
                            textStyle = TextStyle(color = GridWhite),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = Indigo900)
                            }
                            Button(
                                onClick = {
                                    if (noteTitleInput.isNotEmpty()) {
                                        viewModel.createFolderAndNote(noteTitleInput, noteContentInput, folderNameInput)
                                        showAddDialog = false
                                        noteTitleInput = ""
                                        noteContentInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Create", color = Color.White)
                            }
                        }

                        Divider(color = GridWhite.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp))

                        // Option B: Interactive OCR (Module 4) Scan Option
                        Text("Option 2: Run Multilingual OCR", color = Indigo900, fontWeight = FontWeight.Bold)
                        Text("Extracted text from images automatically (Bengali and English). Supports printed and handwritten material.", fontSize = 12.sp, color = GridWhite)

                        if (isOcrLoading) {
                            CircularProgressIndicator(color = AccentCyan, modifier = Modifier.align(Alignment.CenterHorizontally))
                        } else {
                            Button(
                                onClick = {
                                    viewModel.runOcr("image/png", "MOCK_BASE_64", "OCR Extracts")
                                    showAddDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple, contentColor = GridWhite),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PhotoCamera, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Simulate Handwrite Image Scan")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Module 5: Summarization Screen ---
@Composable
fun SummariesScreen(viewModel: StudyViewModel) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val summaryEntity by viewModel.currentSummary.collectAsState()
    val isSummarizing by viewModel.isSummarizing.collectAsState()
    var activeFormat by remember { mutableStateOf("Chapter") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("AI Summary Generator", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Turn long resources into structured high-retention maps", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        if (selectedNote == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Please select or upload a Note first in the Library!", color = Color(0xFFEA580C))
            }
            return
        }

        Spacer(modifier = Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800),
            border = BorderStroke(1.dp, Color(0xFFE8DEF8))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Selected Document:", fontSize = 12.sp, color = Indigo900)
                Text(selectedNote!!.title, fontWeight = FontWeight.Bold, color = GridWhite)
            }
        }

        // Summary Formats Row Tab Selectors
        Spacer(modifier = Modifier.height(12.dp))
        val formats = listOf("Chapter", "Topic", "Bullet", "Exam")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            formats.forEach { format ->
                val isSel = (activeFormat == format)
                Button(
                    onClick = { activeFormat = format },
                    border = if (!isSel) BorderStroke(1.dp, Color(0xFFD0BCFF)) else null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSel) AccentCyan else Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        format, 
                        color = if (isSel) Color.White else GridWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summarized blocks viewer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Slate800, RoundedCornerShape(12.dp))
                .border(1.dp, Indigo900, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (isSummarizing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                    Spacer(Modifier.height(12.dp))
                    Text("Deconstructing text structure via Gemini AI...", color = GridWhite)
                }
            } else {
                val displayText = when (activeFormat) {
                    "Chapter" -> summaryEntity?.chapterSummary
                    "Topic" -> summaryEntity?.topicSummary
                    "Bullet" -> summaryEntity?.bulletSummary
                    "Exam" -> summaryEntity?.examSummary
                    else -> ""
                }

                if (displayText.isNullOrEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No summary generated for $activeFormat format yet.",
                            color = Color(0xFFEA580C),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.generateSummary(activeFormat) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Ask Gemini to Summarize", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = "✨ $activeFormat Outline Report",
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = displayText,
                                color = GridWhite,
                                lineHeight = 22.sp,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generateSummary(activeFormat) },
                                colors = ButtonDefaults.buttonColors(containerColor = Indigo900),
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Text("Re-Summarize", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Module 6: Chat with Notes Screen & RAG Source Citations ---
@Composable
fun ChatWithNotesScreen(viewModel: StudyViewModel) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    var inputQuery by remember { mutableStateOf("") }

    val quickQuestions = listOf(
        "Define deadlock conditions.",
        "What is time quantum?",
        "Explain Convoy effect."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Local Document RAG", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("AI queries notes directly with page-number citations", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        if (selectedNote == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Please upload or select lecture notes from the library!", color = Color.Yellow)
            }
            return
        }

        Spacer(modifier = Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Slate800)) {
            Text(
                "Quizzing relative context: ${selectedNote!!.title}",
                fontWeight = FontWeight.Bold,
                color = AccentCyan,
                modifier = Modifier.padding(8.dp),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat logs
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Slate800, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (chatHistory.isEmpty()) {
                item {
                    Text(
                        "Hi academic student! Ask me anything directly from the notes, or tap these quick topics:",
                        color = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    quickQuestions.forEach { qq ->
                        Button(
                            onClick = { viewModel.sendMessageToNote(qq) },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo900),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(qq, color = AccentCyan, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(chatHistory) { turn ->
                    val isUser = turn.sender == "student"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isUser) SoftPurple else Indigo900)
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isUser) "Student" else "Study Guide AI",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Slate900 else AccentCyan,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(turn.message, color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        CircularProgressIndicator(color = AccentCyan, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Keyboard Row input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                placeholder = { Text("Ask about note contents...", color = GridWhite.copy(alpha = 0.5f)) },
                textStyle = TextStyle(color = Color.White),
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = SoftPurple
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputQuery.isNotEmpty()) {
                        viewModel.sendMessageToNote(inputQuery)
                        inputQuery = ""
                    }
                },
                modifier = Modifier
                    .background(AccentCyan, CircleShape)
                    .size(48.dp)
            ) {
                Icon(Icons.Default.Send, "Send", tint = Slate900)
            }
        }
    }
}

// --- Module 7: Interactive Quiz System ---
@Composable
fun InteractiveQuizScreen(viewModel: StudyViewModel) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val quizObj by viewModel.activeQuiz.collectAsState()
    val isLoading by viewModel.isQuizLoading.collectAsState()

    var activeQuizIndex by remember { mutableStateOf(0) }
    var selectedAnswersMap = remember { mutableStateMapOf<Int, String>() }
    var tfAnswersMap = remember { mutableStateMapOf<Int, String>() }
    var userEssayNotes by remember { mutableStateOf("") }
    var displayCheckResult by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Practice Assessments", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("AI generates customized MCQ, True/False, and grading rubrics", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        if (selectedNote == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Select a study note first to generate assessments!", color = Color.Yellow)
            }
            return
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentCyan)
                    Spacer(Modifier.height(8.dp))
                    Text("Creating complete Quiz layout...", color = GridWhite)
                }
            }
            return
        }

        if (quizObj == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { viewModel.generateQuizFromNote() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Generate Quiz for ${selectedNote!!.title}", color = Slate900, fontWeight = FontWeight.Bold)
                }
            }
            return
        }

        // Active MCQs or True/False Panel
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "📋 ${quizObj!!.title}",
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan,
                    fontSize = 20.sp
                )
            }

            // MCQs list
            item {
                Text("Section 1: Multiple Choice (MCQs)", fontWeight = FontWeight.Bold, color = SoftPurple, fontSize = 16.sp)
            }

            items(quizObj!!.mcqs.size) { index ->
                val mcq = quizObj!!.mcqs[index]
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${index + 1}. ${mcq.question}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        mcq.options.forEach { opt ->
                            val choicePrefix = opt.take(1) // A, B, C, D
                            val isSelected = selectedAnswersMap[index] == choicePrefix
                            val isCorrectChoice = choicePrefix == mcq.correctAnswer

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) {
                                            if (displayCheckResult) {
                                                if (isCorrectChoice) Color.Green.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
                                            } else AccentCyan.copy(alpha = 0.15f)
                                        } else Color.Transparent
                                    )
                                    .clickable {
                                        if (!displayCheckResult) {
                                            selectedAnswersMap[index] = choicePrefix
                                        }
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (!displayCheckResult) {
                                            selectedAnswersMap[index] = choicePrefix
                                        }
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AccentCyan)
                                )
                                Text(opt, color = Color.White, fontSize = 14.sp)
                            }
                        }

                        if (displayCheckResult) {
                            val userAns = selectedAnswersMap[index]
                            if (userAns == mcq.correctAnswer) {
                                Text("✓ Correct!", color = Color.Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                Text("✗ Incorrect! Correct choice is ${mcq.correctAnswer}", color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            }
                            Text(
                                "Explanation: ${mcq.explanation}",
                                color = GridWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // True False
            item {
                Text("Section 2: True or False", fontWeight = FontWeight.Bold, color = SoftPurple, fontSize = 16.sp)
            }

            items(quizObj!!.trueFalse.size) { index ->
                val tf = quizObj!!.trueFalse[index]
                Card(colors = CardDefaults.cardColors(containerColor = Slate800)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${index + 1}. ${tf.question}", fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            listOf("True", "False").forEach { valOpt ->
                                val isSelected = tfAnswersMap[index] == valOpt
                                Button(
                                    onClick = { if (!displayCheckResult) tfAnswersMap[index] = valOpt },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) AccentCyan else Indigo900
                                    )
                                ) {
                                    Text(valOpt, color = if (isSelected) Slate900 else Color.White)
                                }
                            }
                        }

                        if (displayCheckResult) {
                            val userTF = tfAnswersMap[index]
                            if (userTF == tf.correctAnswer) {
                                Text("✓ Correct!", color = Color.Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                            } else {
                                Text("✗ Incorrect! Explaining: ${tf.explanation}", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }

            // Short Questions Essay Practice
            item {
                Text("Section 3: Short Subjective Questions", fontWeight = FontWeight.Bold, color = SoftPurple, fontSize = 16.sp)
            }

            items(quizObj!!.shortQuestions.size) { index ->
                val sq = quizObj!!.shortQuestions[index]
                Card(colors = CardDefaults.cardColors(containerColor = Slate800)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(sq.question, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userEssayNotes,
                            onValueChange = { userEssayNotes = it },
                            placeholder = { Text("Draft your conceptual reply here...", color = GridWhite.copy(alpha = 0.5f)) },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentCyan, unfocusedBorderColor = SoftPurple)
                        )

                        if (displayCheckResult) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Grading Rubric Model answer:", fontWeight = FontWeight.Bold, color = AccentCyan)
                            Text(sq.sampleAnswer, color = GridWhite)
                            Text("Target Rubric Criteria: ${sq.rubric}", color = SoftPurple, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Actions Row
        val context = LocalContext.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    displayCheckResult = true
                    // Compute basic score & submit to progress charts
                    var correctlyAnswered = 0
                    var mcqSize = quizObj!!.mcqs.size
                    for (i in 0 until mcqSize) {
                        if (selectedAnswersMap[i] == quizObj!!.mcqs[i].correctAnswer) correctlyAnswered++
                    }
                    val accuracyPercentage = if (mcqSize > 0) (correctlyAnswered.toFloat() / mcqSize) * 100f else 80.0f
                    viewModel.submitQuizScore(accuracyPercentage)
                    Toast.makeText(context, "Quiz Checked! Score Logged to Progress", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                modifier = Modifier.weight(1f)
            ) {
                Text("Grade Quiz & Log", color = Slate900, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    displayCheckResult = false
                    selectedAnswersMap.clear()
                    tfAnswersMap.clear()
                    userEssayNotes = ""
                    viewModel.generateQuizFromNote()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo900),
                modifier = Modifier.weight(1f)
            ) {
                Text("Reset / Regenerate")
            }
        }
    }
}

// --- Module 8: Flashcard Generator with Flip Deck Review Mode ---
@Composable
fun FlashcardTrainerScreen(viewModel: StudyViewModel) {
    val selectedNote by viewModel.selectedNote.collectAsState()
    val flashcards by viewModel.flashcardList.collectAsState()
    val isCardLoading by viewModel.isFlashcardLoading.collectAsState()

    var showManualAddDialog by remember { mutableStateOf(false) }
    var manualQ by remember { mutableStateOf("") }
    var manualA by remember { mutableStateOf("") }

    // Carousel state
    var cardIndex by remember { mutableStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Retention Trainer", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Flippable flashcards compiled directly by AI", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        if (selectedNote == null) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Select a study note first to load memory flashcards!", color = Color.Yellow)
            }
            return
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Create custom action bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showManualAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = SoftPurple)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(4.dp))
                Text("Create Manual")
            }

            if (flashcards.isEmpty()) {
                Button(
                    onClick = { viewModel.generateFlashcards() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                ) {
                    Text("Auto-compile cards")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isCardLoading) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentCyan)
            }
            return
        }

        if (flashcards.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No Flashcards built. Tap options above to begin!", color = GridWhite)
            }
            return
        }

        // Active Card Index Clamp
        val activeIndex = cardIndex.coerceIn(0, flashcards.size - 1)
        val activeCard = flashcards[activeIndex]

        // Progress indicators
        Text(
            text = "Active Card: ${activeIndex + 1} / ${flashcards.size} | Mastery: ${if (activeCard.isMastered) "🌟 Mastered" else "⏳ Learning"}",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Flipping Card Layout (Module 8)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(if (isFlipped) Indigo900 else Slate800)
                .border(2.dp, if (activeCard.isMastered) Color.Yellow else AccentCyan, RoundedCornerShape(24.dp))
                .clickable { isFlipped = !isFlipped }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isFlipped) "ANSWER (Click to flip)" else "QUESTION (Click to flip)",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isFlipped) activeCard.answer else activeCard.question,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Carousel Controls
        Row(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (activeIndex > 0) {
                        isFlipped = false
                        cardIndex--
                    }
                },
                modifier = Modifier.background(Slate800, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, "Prev", tint = Color.White)
            }

            // Flag as Mastered (Module 8 Review Mode)
            Button(
                onClick = { viewModel.toggleFlashcardMastery(activeCard.flashcardId, activeCard.isMastered) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (activeCard.isMastered) Color.DarkGray else AccentCyan
                )
            ) {
                Text(
                    text = if (activeCard.isMastered) "Mark as Learning" else "✓ Mark Mastered",
                    color = if (activeCard.isMastered) Color.White else Slate900,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = {
                    if (activeIndex < flashcards.size - 1) {
                        isFlipped = false
                        cardIndex++
                    }
                },
                modifier = Modifier.background(Slate800, CircleShape)
            ) {
                Icon(Icons.Default.ArrowForward, "Next", tint = Color.White)
            }
        }

        // Add Manual Card Dialog
        if (showManualAddDialog) {
            Dialog(onDismissRequest = { showManualAddDialog = false }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate805(Slate800)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("New Flashcard", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                        
                        OutlinedTextField(
                            value = manualQ,
                            onValueChange = { manualQ = it },
                            label = { Text("Question / Term", color = GridWhite) },
                            textStyle = TextStyle(color = Color.White)
                        )

                        OutlinedTextField(
                            value = manualA,
                            onValueChange = { manualA = it },
                            label = { Text("Answer", color = GridWhite) },
                            textStyle = TextStyle(color = Color.White)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { showManualAddDialog = false }) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    if (manualQ.isNotEmpty() && manualA.isNotEmpty()) {
                                        viewModel.createManualFlashcard(manualQ, manualA)
                                        showManualAddDialog = false
                                        manualQ = ""
                                        manualA = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                            ) {
                                Text("Create", color = Slate900)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Slate805(color: Color): Color = color

// --- Module 9: Study Planner Screen ---
@Composable
fun StudyPlannerScreen(viewModel: StudyViewModel) {
    val activePlan by viewModel.studyPlan.collectAsState()
    val isLoading by viewModel.isPlanLoading.collectAsState()

    var subjectText by remember { mutableStateOf("Computer Systems Final") }
    var examDateText by remember { mutableStateOf("2026-06-18") }
    var targetHours by remember { mutableStateOf(3f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("AI Study Roadmap", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Personalized calendars optimized around exams and hourly targets", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(16.dp))

        // Plan Inputs Form
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    label = { Text("Course / Subject Name", color = AccentCyan) },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = examDateText,
                        onValueChange = { examDateText = it },
                        label = { Text("Exam Date (YYYY-MM-DD)", color = GridWhite) },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hours / day: ${String.format("%.1f", targetHours)}", color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = targetHours,
                            onValueChange = { targetHours = it },
                            valueRange = 1f..8f,
                            colors = SliderDefaults.colors(thumbColor = AccentCyan, activeTrackColor = AccentCyan)
                        )
                    }
                }

                Button(
                    onClick = { viewModel.customizeStudyPlan(subjectText, examDateText, targetHours) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Custom Plan from Gemini", color = Slate900, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Planner output timeline
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Slate800, RoundedCornerShape(12.dp))
                .border(1.dp, Indigo900, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                    Spacer(Modifier.height(8.dp))
                    Text("Customizing daily milestone activities...", color = GridWhite)
                }
            } else if (activePlan == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No active plan designed. Input details above and tap Generate!", color = GridWhite, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Roadmap: ${activePlan!!.subject}", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 18.sp)
                            Text("⏳ ${activePlan!!.examCountdown} Days Left", color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(activePlan!!.days) { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Checkbox(
                                checked = day.isDone,
                                onCheckedChange = { day.isDone = it },
                                colors = CheckboxDefaults.colors(checkedColor = AccentCyan)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(day.dayName, fontWeight = FontWeight.Bold, color = SoftPurple)
                                Text("Focus: ${day.topic} (${day.hours} hrs)", fontWeight = FontWeight.Bold, color = Color.White)
                                day.activities.forEach { act ->
                                    Text("• $act", color = GridWhite, fontSize = 13.sp)
                                }
                            }
                        }
                        Divider(color = Slate900.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// --- Module 11: Research Paper Assistant ---
@Composable
fun PaperAssistantScreen(viewModel: StudyViewModel) {
    val resultsText by viewModel.paperAnalysisOutput.collectAsState()
    val isLoading by viewModel.isPaperLoading.collectAsState()

    var paperTitle by remember { mutableStateOf("Attention Is All You Need") }
    var paperContentStr by remember { mutableStateOf(
        "Abstract: We propose a new simple network architecture, the Transformer, based solely on attention mechanisms... Self-attention maps lexical elements sequentially. Our model achieves English-to-German translation BLEU score of 28.4, outperforming state of the art recurrence nets because recurrence introduces computation sequencing bottlenecks."
    ) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Scholarly Paper Assistant", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Extract objectives, findings, limitations, and standard citations automatically", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        Spacer(modifier = Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Slate800)) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = paperTitle,
                    onValueChange = { paperTitle = it },
                    label = { Text("Research Paper Title", color = AccentCyan) },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = paperContentStr,
                    onValueChange = { paperContentStr = it },
                    label = { Text("Paste paper abstract/intro content", color = GridWhite) },
                    textStyle = TextStyle(color = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )

                Button(
                    onClick = { viewModel.analyzeResearchPaperText(paperTitle, paperContentStr) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Analyze Scholarly Document", color = Slate900, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Slate800, RoundedCornerShape(12.dp))
                .border(1.dp, Indigo900, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = AccentCyan)
                    Spacer(Modifier.height(8.dp))
                    Text("Extracting methodology & citation indices...", color = GridWhite)
                }
            } else if (resultsText.isNullOrEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No paper analysis run. Tap the button above to test PDF AI summary!", color = GridWhite, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(resultsText!!, color = Color.White, lineHeight = 20.sp, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

// --- Module 10: Progress Visualization (Canvas Charts) ---
@Composable
fun ProgressVisualizationScreen(viewModel: StudyViewModel) {
    val progressList by viewModel.progressHistory.collectAsState()

    // Mock progress data if DB is cold
    val chartData = if (progressList.size >= 3) progressList else listOf(
        com.example.data.database.ProgressEntity(0, "user", "Mon", 45, 80.0f, "{}"),
        com.example.data.database.ProgressEntity(0, "user", "Tue", 60, 90.0f, "{}"),
        com.example.data.database.ProgressEntity(0, "user", "Wed", 30, 75.0f, "{}"),
        com.example.data.database.ProgressEntity(0, "user", "Thu", 50, 85.0f, "{}")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Learning Analytics", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        Text("Canvas data visualizations recording weekly minutes and accuracy ratios", color = GridWhite.copy(alpha = 0.7f), fontSize = 14.sp)

        // Plot 1: Study Minutes Bar Chart
        Text("Weekly Study Engagement (Minutes)", fontWeight = FontWeight.Bold, color = AccentCyan)
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                val barCount = chartData.size
                val spacing = 20.dp.toPx()
                val totalSpacingWidth = spacing * (barCount + 1)
                val barWidth = (chartWidth - totalSpacingWidth) / barCount
                val maxMinutes = chartData.maxOf { it.studyMinutes.coerceAtLeast(30) }.toFloat()

                // Baseline axis
                drawLine(
                    color = Color.White.copy(alpha = 0.5f),
                    start = Offset(0f, chartHeight - 20.dp.toPx()),
                    end = Offset(chartWidth, chartHeight - 20.dp.toPx()),
                    strokeWidth = 2f
                )

                chartData.forEachIndexed { idx, progress ->
                    val x = spacing + idx * (barWidth + spacing)
                    val ratio = progress.studyMinutes.toFloat() / maxMinutes
                    val heightRatio = ratio * (chartHeight - 40.dp.toPx())
                    val topY = (chartHeight - 20.dp.toPx()) - heightRatio

                    // Draw animated-looking cylinder bar
                    drawRoundRect(
                        color = AccentCyan,
                        topLeft = Offset(x, topY),
                        size = Size(barWidth, heightRatio),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )

                    // Draw short labels below bar
                    // Draw values above bar
                }
            }
        }

        // Plot 2: Quiz Accuracy Radial Progress
        Text("Average Quiz Accuracy Meter", fontWeight = FontWeight.Bold, color = AccentCyan)
        Card(
            colors = CardDefaults.cardColors(containerColor = Slate800),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = Slate900,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                        drawArc(
                            color = SoftPurple,
                            startAngle = -90f,
                            sweepAngle = 360f * 0.85f, // simulated 85% accuracy
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text("85%", color = GridWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Diagnostic Overview", fontWeight = FontWeight.Bold, color = GridWhite)
                    Text("• Overall Accuracy: 85% Score", color = GridWhite.copy(alpha = 0.8f), fontSize = 13.sp)
                    Text("• Weakness: Preemption timing thresholds", color = Color(0xFFEA580C), fontSize = 13.sp)
                    Text("• Strength: Coffman Deadlock structures", color = Color(0xFF059669), fontSize = 13.sp)
                }
            }
        }
    }
}

// --- Dynamic Bottom Navigation Bar ---
data class NavigationItem(val screen: ActiveScreen, val icon: ImageVector, val name: String)

@Composable
fun StudyNavigationBar(activeScreen: ActiveScreen, onScreenSelected: (ActiveScreen) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF1D192B)
    ) {
        val navItems = listOf(
            NavigationItem(ActiveScreen.Dashboard, Icons.Default.Dashboard, "Home"),
            NavigationItem(ActiveScreen.Notes, Icons.Default.LibraryBooks, "Library"),
            NavigationItem(ActiveScreen.Chat, Icons.Default.Forum, "Chat"),
            NavigationItem(ActiveScreen.Quizzes, Icons.Default.Quiz, "Quiz"),
            NavigationItem(ActiveScreen.Flashcards, Icons.Default.Style, "Cards"),
            NavigationItem(ActiveScreen.Planner, Icons.Default.CalendarMonth, "Planner"),
            NavigationItem(ActiveScreen.Research, Icons.Default.Science, "Scholarly"),
            NavigationItem(ActiveScreen.Progress, Icons.Default.Analytics, "Stats")
        )

        navItems.forEach { item ->
            val isSelected = activeScreen == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.name,
                        tint = if (isSelected) Color(0xFF1D192B) else Color(0xFF94A3B8)
                    )
                },
                label = {
                    Text(
                        item.name,
                        fontSize = 10.sp,
                        color = if (isSelected) Color(0xFF1D192B) else Color(0xFF94A3B8),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFE8DEF8)
                )
            )
        }
    }
}

// --- Dashboard Quick Action Button Composable ---
@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(bgColor, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )
    }
}
