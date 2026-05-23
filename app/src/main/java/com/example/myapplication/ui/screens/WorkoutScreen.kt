package com.example.myapplication.ui.screens

import android.app.Activity
import android.media.AudioManager
import android.media.ToneGenerator
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.model.WorkoutRepository
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val exercises = remember { WorkoutRepository.getExercises() }
    var currentExerciseIndex by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(exercises.getOrNull(0)?.durationSeconds ?: 0) }
    var isWorkoutFinished by remember { mutableStateOf(false) }

    val currentExercise = exercises.getOrNull(currentExerciseIndex)
    val nextExercise = exercises.getOrNull(currentExerciseIndex + 1)
    val initialDuration = currentExercise?.durationSeconds ?: 1

    // 1. WAKELOCK: Bildschirm anlassen
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // 2. SOUND: ToneGenerator initialisieren
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100) }
    DisposableEffect(Unit) {
        onDispose {
            toneGenerator.release()
        }
    }

    // 3. ANIMATION: Pulsieren bei < 5 Sekunden
    val isLowTime = timeLeft <= 5 && timeLeft > 0 && !isWorkoutFinished
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )
    val animatedScale = if (isLowTime) pulseScale else 1.0f
    val timerColor = if (isLowTime) Color.Red else MaterialTheme.colorScheme.primary

    // Timer Logic
    LaunchedEffect(key1 = currentExerciseIndex, key2 = isWorkoutFinished) {
        if (isWorkoutFinished || currentExercise == null) return@LaunchedEffect

        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
            // Optional: Kurzer Beep bei jeder der letzten 3 Sekunden
            if (timeLeft in 1..3) {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            }
        }

        if (currentExerciseIndex < exercises.size - 1) {
            // SOUND: Signal für Übungswechsel
            toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 300)
            currentExerciseIndex++
            timeLeft = exercises[currentExerciseIndex].durationSeconds
        } else {
            // SOUND: Signal für Workout-Ende
            toneGenerator.startTone(ToneGenerator.TONE_DTMF_D, 500)
            isWorkoutFinished = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                actions = {
                    if (!isWorkoutFinished) {
                        TextButton(onClick = onBack) {
                            Text("ABBRECHEN", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isWorkoutFinished) {
                WorkoutFinishedScreen(onBack)
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Übungsname mit Animation
                    AnimatedContent(
                        targetState = currentExercise?.name ?: "",
                        transitionSpec = {
                            fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                        }, label = "ExerciseNameAnimation"
                    ) { name ->
                        Text(
                            text = name,
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Circular Timer mit Pulsieren
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer(
                            scaleX = animatedScale,
                            scaleY = animatedScale
                        )
                    ) {
                        CircularProgressIndicator(
                            progress = { timeLeft.toFloat() / initialDuration.toFloat() },
                            modifier = Modifier.size(250.dp),
                            color = timerColor,
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.surface,
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            text = timeLeft.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = timerColor
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4. ÜBUNGSVORSCHAU: Nächste Übung anzeigen
                    if (nextExercise != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Nächste: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "${nextExercise.name} (${nextExercise.durationSeconds}s)",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Letzte Übung!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Fortschritt
                    Text(
                        text = "Übung ${currentExerciseIndex + 1} von ${exercises.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutFinishedScreen(onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "Workout abgeschlossen!",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Gute Arbeit! Du hast alle Übungen erfolgreich gemeistert.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("ZURÜCK ZUM START", fontWeight = FontWeight.Bold)
        }
    }
}
