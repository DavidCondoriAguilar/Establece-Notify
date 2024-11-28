package com.example.notifyi

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notifyi.ui.theme.NotifyITheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solicitar permisos de notificación
        requestNotificationPermission()

        // Crear el canal de notificaciones
        createNotificationChannel()

        // Notificar que la aplicación se ha iniciado
        sendNotification("Aplicación iniciada", "NotifyI está lista para monitorear la hora.")

        setContent {
            NotifyITheme {
                var selectedHour by remember { mutableStateOf("") }
                var statusMessage by remember { mutableStateOf("") }
                var statusColor by remember { mutableStateOf(Color.Gray) }
                val scope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NotificationScreen(
                        selectedHour = selectedHour,
                        onHourChange = { selectedHour = it },
                        onSetNotification = {
                            if (isValidTime(selectedHour)) {
                                Log.i(TAG, "Hora válida ingresada: $selectedHour")
                                statusMessage = "Hora establecida: $selectedHour"
                                statusColor = Color.Green
                                sendNotification("Hora establecida", "Notificación programada para las $selectedHour.")
                                scope.launch(Dispatchers.IO) {
                                    monitorTimeAndSendNotification(selectedHour)
                                }
                            } else {
                                Log.e(TAG, "Hora inválida ingresada: $selectedHour")
                                statusMessage = "Error: Hora inválida"
                                statusColor = Color.Red
                                sendNotification("Error", "Por favor, ingresa una hora válida en formato HH:mm.")
                            }
                        },
                        statusMessage = statusMessage,
                        statusColor = statusColor,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                val requestPermissionLauncher = registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        Log.i(TAG, "Permiso de notificación concedido.")
                    } else {
                        Log.w(TAG, "Permiso de notificación denegado.")
                    }
                }
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Canal de Notificaciones"
            val descriptionText = "Canal para notificaciones con vibración"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun isValidTime(time: String): Boolean {
        return try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            format.isLenient = false
            format.parse(time)
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun monitorTimeAndSendNotification(selectedHour: String) {
        try {
            val timeZone = TimeZone.getTimeZone("America/Lima")
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.timeZone = timeZone

            while (true) {
                val currentTime = sdf.format(Date())
                Log.d(TAG, "Hora actual: $currentTime | Hora monitoreada: $selectedHour")
                if (currentTime == selectedHour) {
                    sendNotificationWithVibration()
                    break
                }
                Thread.sleep(1000) // Revisar cada minuto
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error monitoreando la hora", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotificationWithVibration() {
        sendNotification("¡Notificación!", "Es hora de tu notificación.")

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(vibrationEffect)
            } else {
                vibrator.vibrate(500)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "MainActivity"
        const val CHANNEL_ID = "notification_channel"
        const val NOTIFICATION_ID = 1
    }
}

@Composable
fun NotificationScreen(
    selectedHour: String,
    onHourChange: (String) -> Unit,
    onSetNotification: () -> Unit,
    statusMessage: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Notifications, contentDescription = "Icono de alarma", tint = Color(0xFF6200EA), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Establece una notificación", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // Agregar TimePickerButton para seleccionar hora
        TimePickerButton(onTimeChange = onHourChange)

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSetNotification) {
            Text(text = "Establecer Notificación")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = statusMessage, color = statusColor, style = MaterialTheme.typography.bodyLarge)
    }
}

@SuppressLint("NewApi")
@Composable
fun TimePickerButton(onTimeChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(LocalTime.now().hour) }
    var selectedMinute by remember { mutableStateOf(LocalTime.now().minute) }

    if (showDialog) {
        TimePickerDialog(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            onDismissRequest = { showDialog = false },
            onTimeSelected = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                onTimeChange(String.format("%02d:%02d", hour, minute))
                showDialog = false
            }
        )
    }

    Button(
        onClick = { showDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
    ) {
        Text("Seleccionar Hora", color = Color.White, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Selecciona la hora") },
        text = {
            Column {
                TimePicker(
                    hour = hour,
                    minute = minute,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(hour, minute)
            }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun TimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    Row {
        // Hour Picker
        NumberPicker(
            value = hour,
            range = 0..23,
            onValueChange = { onHourChange(it) }
        )
        Text(":", modifier = Modifier.padding(vertical = 10.dp))
        // Minute Picker
        NumberPicker(
            value = minute,
            range = 0..59,
            onValueChange = { onMinuteChange(it) }
        )
    }
}

@Composable
fun NumberPicker(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    var currentValue by remember { mutableStateOf(value) }
    Column {
        Button(onClick = {
            val newValue = (currentValue - 1 + range.last + 1) % (range.last + 1)
            currentValue = newValue
            onValueChange(currentValue)
        }) {
            Text("▼")
        }
        Text("$currentValue")
        Button(onClick = {
            val newValue = (currentValue + 1) % (range.last + 1)
            currentValue = newValue
            onValueChange(currentValue)
        }) {
            Text("▲")
        }
    }
}
