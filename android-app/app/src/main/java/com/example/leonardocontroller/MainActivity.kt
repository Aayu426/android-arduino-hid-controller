package com.example.leonardocontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.leonardocontroller.bluetooth.BluetoothManager
import com.example.leonardocontroller.ui.theme.LeonardoControllerTheme
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.graphics.Paint


class MainActivity : ComponentActivity() {

    private lateinit var bluetoothManager: BluetoothManager

    private val bluetoothPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            // After permission result, user can press CONNECT again.
        }

    private val bluetoothEnableLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // User can press CONNECT again after enabling Bluetooth.
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        bluetoothManager = BluetoothManager(this)

        setContent {
            LeonardoControllerTheme {
                HackerControllerUI(
                    context = this,
                    bluetoothManager = bluetoothManager,
                    requestBluetoothPermissions = {
                        requestBluetoothPermissions()
                    },
                    enableBluetooth = {
                        enableBluetooth()
                    }
                )
            }
        }
    }

    private fun requestBluetoothPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            bluetoothPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
    }

    private fun enableBluetooth() {
        val serviceManager = getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
        val adapter = serviceManager?.adapter

        if (adapter != null && !adapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(intent)
        }
    }

    override fun onDestroy() {
        bluetoothManager.disconnect()
        super.onDestroy()
    }
}


/* =========================================================
   MAIN UI
   ========================================================= */

@Composable
fun HackerControllerUI(
    context: Context,
    bluetoothManager: BluetoothManager,
    requestBluetoothPermissions: () -> Unit,
    enableBluetooth: () -> Unit
) {

    var connected by remember {
        mutableStateOf(
            bluetoothManager.isConnected()
        )
    }

    var statusText by remember {
        mutableStateOf(
            "DISCONNECTED"
        )
    }

    var activeWorkflow by remember {
        mutableIntStateOf(0)
    }

    var led1On by remember {
        mutableStateOf(false)
    }

    var led2On by remember {
        mutableStateOf(false)
    }

    var led3On by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0xFF020608)
            )
    ) {

        MatrixBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xAA020608),
                            Color(0x66020608),
                            Color(0xCC020608)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    horizontal = 18.dp,
                    vertical = 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Header()

            Spacer(
                modifier = Modifier.height(18.dp)
            )


            /* =================================================
               REAL HC-05 CONNECTION PANEL
               ================================================= */

            ConnectionPanel(
                connected = connected,
                statusText = statusText,
                onConnect = {

                    if (!bluetoothManager.isBluetoothSupported()) {
                        statusText = "BLUETOOTH NOT SUPPORTED"
                        return@ConnectionPanel
                    }

                    if (!bluetoothManager.isBluetoothEnabled()) {
                        statusText = "BLUETOOTH OFF"
                        enableBluetooth()
                        return@ConnectionPanel
                    }

                    if (
                        android.os.Build.VERSION.SDK_INT >= 31 &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestBluetoothPermissions()
                        statusText = "PERMISSION REQUIRED"
                        return@ConnectionPanel
                    }

                    statusText = "CONNECTING..."

                    scope.launch {
                        val result = bluetoothManager.connectToHC05()

                        if (result.isSuccess) {
                            connected = true
                            statusText = "CONNECTED"
                        } else {
                            connected = false
                            statusText =
                                result.exceptionOrNull()
                                    ?.message
                                    ?: "CONNECTION FAILED"
                        }
                    }
                },
                onDisconnect = {
                    bluetoothManager.disconnect()
                    connected = false
                    statusText = "DISCONNECTED"
                }
            )


            Spacer(
                modifier = Modifier.height(22.dp)
            )


            Text(
                text = "SELECT WORKFLOW",
                color = Color(0xFF00FF88),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )


            Spacer(
                modifier = Modifier.height(14.dp)
            )


            WorkflowCard(
                number = "01",
                title = "WORKFLOW ONE",
                description = "Execute predefined HID sequence",
                ledNumber = "LED 1",
                active = activeWorkflow == 1,
                accent = Color(0xFF00FF88),
                onClick = {
                    if (!connected) {
                        statusText = "CONNECT HC-05 FIRST"
                        return@WorkflowCard
                    }
                    activeWorkflow = 1
                    scope.launch {
                        bluetoothManager.sendCommand('1')
                    }
                }
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            WorkflowCard(
                number = "02",
                title = "WORKFLOW TWO",
                description = "Execute predefined HID sequence",
                ledNumber = "LED 2",
                active = activeWorkflow == 2,
                accent = Color(0xFF00E5FF),
                onClick = {
                    if (!connected) {
                        statusText = "CONNECT HC-05 FIRST"
                        return@WorkflowCard
                    }
                    activeWorkflow = 2
                    scope.launch {
                        bluetoothManager.sendCommand('2')
                    }
                }
            )


            Spacer(
                modifier = Modifier.height(12.dp)
            )


            WorkflowCard(
                number = "03",
                title = "WORKFLOW THREE",
                description = "Execute predefined HID sequence",
                ledNumber = "LED 3",
                active = activeWorkflow == 3,
                accent = Color(0xFFD000FF),
                onClick = {
                    if (!connected) {
                        statusText = "CONNECT HC-05 FIRST"
                        return@WorkflowCard
                    }
                    activeWorkflow = 3
                    scope.launch {
                        bluetoothManager.sendCommand('3')
                    }
                }
            )


            Spacer(
                modifier = Modifier.height(18.dp)
            )


            Text(
                text = "MANUAL LIGHT CONTROL",
                color = Color(0xFF00FF88),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )


            Spacer(
                modifier = Modifier.height(10.dp)
            )


            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                ManualLEDButton(
                    modifier = Modifier.weight(1f),
                    label = "LED 1",
                    enabled = led1On,
                    accent = Color(0xFF00FF88),
                    onClick = {
                        if (!connected) {
                            statusText = "CONNECT HC-05 FIRST"
                            return@ManualLEDButton
                        }
                        led1On = !led1On
                        scope.launch {
                            bluetoothManager.sendCommand(if (led1On) 'A' else 'a')
                        }
                    }
                )


                ManualLEDButton(
                    modifier = Modifier.weight(1f),
                    label = "LED 2",
                    enabled = led2On,
                    accent = Color(0xFF00E5FF),
                    onClick = {
                        if (!connected) {
                            statusText = "CONNECT HC-05 FIRST"
                            return@ManualLEDButton
                        }
                        led2On = !led2On
                        scope.launch {
                            bluetoothManager.sendCommand(if (led2On) 'B' else 'b')
                        }
                    }
                )


                ManualLEDButton(
                    modifier = Modifier.weight(1f),
                    label = "LED 3",
                    enabled = led3On,
                    accent = Color(0xFFD000FF),
                    onClick = {
                        if (!connected) {
                            statusText = "CONNECT HC-05 FIRST"
                            return@ManualLEDButton
                        }
                        led3On = !led3On
                        scope.launch {
                            bluetoothManager.sendCommand(if (led3On) 'C' else 'c')
                        }
                    }
                )
            }


            Spacer(
                modifier = Modifier.height(30.dp)
            )


            SystemStatus(
                status = statusText
            )
        }
    }
}


/* =========================================================
   CONNECTION PANEL
   ========================================================= */

@Composable
fun ConnectionPanel(
    connected: Boolean,
    statusText: String,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(14.dp)
            )
            .background(
                Color(0xCC08110E)
            )
            .border(
                1.dp,
                if (connected)
                    Color(0xFF00FF88)
                else
                    Color(0xFFAA3344),
                RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (connected)
                            Color(0xFF00FF88)
                        else
                            Color(0xFFAA3344),
                        CircleShape
                    )
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Column {

                Text(
                    text = "HC-05 LINK",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = statusText,
                    color = if (connected) Color(0xFF00FF88) else Color(0xFFFF5577),
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "9600 BAUD",
                color = Color(0xFF6B8278),
                fontSize = 9.sp
            )
        }


        Spacer(
            modifier = Modifier.height(12.dp)
        )


        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    if (connected)
                        Color(0x3322FF88)
                    else
                        Color(0x33222222)
                )
                .border(
                    1.dp,
                    if (connected)
                        Color(0xFF00FF88)
                    else
                        Color(0xFF53665E),
                    RoundedCornerShape(10.dp)
                )
                .clickable {
                    if (connected)
                        onDisconnect()
                    else
                        onConnect()
                },
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = if (connected) "DISCONNECT HC-05" else "CONNECT HC-05",
                color = if (connected) Color(0xFF00FF88) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}


/* =========================================================
   MANUAL LED BUTTON
   ========================================================= */

@Composable
fun ManualLEDButton(
    modifier: Modifier,
    label: String,
    enabled: Boolean,
    accent: Color,
    onClick: () -> Unit
) {

    Box(
        modifier = modifier
            .height(55.dp)
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                if (enabled)
                    accent.copy(alpha = 0.20f)
                else
                    Color(0xCC080E0C)
            )
            .border(
                1.dp,
                if (enabled)
                    accent
                else
                    Color(0xFF18352C),
                RoundedCornerShape(12.dp)
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (enabled)
                            accent
                        else
                            Color(0xFF26332E),
                        CircleShape
                    )
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = if (enabled) "$label // ON" else "$label // OFF",
                color = if (enabled) accent else Color(0xFF7D9189),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


/* =========================================================
   HEADER
   ========================================================= */

@Composable
fun Header() {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "LEONARDO",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 5.sp
        )

        Text(
            text = "CONTROLLER // HID TERMINAL",
            color = Color(0xFF00FF88),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
    }
}


/* =========================================================
   WORKFLOW CARD
   ========================================================= */

@Composable
fun WorkflowCard(
    number: String,
    title: String,
    description: String,
    ledNumber: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                if (active) 14.dp else 4.dp,
                RoundedCornerShape(18.dp)
            )
            .clip(
                RoundedCornerShape(18.dp)
            )
            .background(
                if (active)
                    Color(0xDD071712)
                else
                    Color(0xCC080E0C)
            )
            .border(
                if (active) 2.dp else 1.dp,
                if (active) accent else Color(0xFF18352C),
                RoundedCornerShape(18.dp)
            )
            .clickable {
                onClick()
            }
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    .background(
                        Brush.linearGradient(
                            listOf(
                                accent.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(
                        1.dp,
                        accent.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = number,
                    color = accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }


            Spacer(
                modifier = Modifier.width(15.dp)
            )


            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = description,
                    color = Color(0xFF7D9189),
                    fontSize = 10.sp
                )
            }


            Column(
                horizontalAlignment = Alignment.End
            ) {

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            if (active) accent else Color(0xFF26332E),
                            CircleShape
                        )
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = ledNumber,
                    color = accent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


/* =========================================================
   SYSTEM STATUS
   ========================================================= */

@Composable
fun SystemStatus(
    status: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = status,
            color = Color(0xFF00FF88),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Text(
            text = "HID // SERIAL",
            color = Color(0xFF53665E),
            fontSize = 9.sp,
            letterSpacing = 1.sp
        )
    }
}


/* =========================================================
   MATRIX BACKGROUND
   ========================================================= */

@Composable
fun MatrixBackground() {

    val transition =
        rememberInfiniteTransition(
            label = "matrix"
        )

    val movement by
    transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = 7000,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Restart
            ),
        label = "matrixMovement"
    )


    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {

        val columns = 22

        val columnWidth = size.width / columns


        for (column in 0 until columns) {

            val x = column * columnWidth

            val random = Random(column * 92821)

            val speed = 0.5f + random.nextFloat()

            val offset =
                (
                        movement *
                                size.height *
                                speed +
                                column * 73f
                        ) % size.height


            for (row in 0 until 18) {

                val y =
                    (
                            offset +
                                    row * 48f
                            ) % size.height


                val character =
                    when (
                        random.nextInt(0, 5)
                    ) {

                        0 -> "0"
                        1 -> "1"
                        2 -> ">"
                        3 -> "/"
                        else -> "•"
                    }


                val paint =
                    Paint().apply {

                        textSize = 13f

                        color =
                            android.graphics.Color.argb(
                                70,
                                0,
                                255,
                                100
                            )

                        isAntiAlias = true
                    }


                drawContext
                    .canvas
                    .nativeCanvas
                    .drawText(
                        character,
                        x,
                        y,
                        paint
                    )
            }
        }


        val scanY = movement * size.height


        drawLine(
            color = Color(0x2200FF88),
            start = Offset(0f, scanY),
            end = Offset(size.width, scanY),
            strokeWidth = 2f
        )


        drawRoundRect(
            color = Color(0x3300FF88),
            topLeft = Offset(8f, 8f),
            size = androidx.compose.ui.geometry.Size(
                size.width - 16f,
                size.height - 16f
            ),
            cornerRadius = CornerRadius(22f, 22f),
            style = Stroke(width = 1.5f)
        )
    }
}