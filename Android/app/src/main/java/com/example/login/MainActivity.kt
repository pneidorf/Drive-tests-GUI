@file:Suppress("DEPRECATION")

package com.example.login

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import android.telephony.cdma.CdmaCellLocation
import android.telephony.gsm.GsmCellLocation
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 101
        private const val TAG = "com.example.login.MainActivity"
        private const val UPDATE_INTERVAL = 2000L // 2 секунды
        private const val SERVER_URL = "http://45.90.218.73:8080"
        private const val WEBSOCKET_ENDPOINT = "/websocket"
    }

    private lateinit var state: MainActivityState
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var httpClient: OkHttpClient
    private lateinit var webSocket: WebSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = MainActivityState(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        httpClient = OkHttpClient()

        setContent {
            MainContent(state)
        }
        checkAndRequestPermissions()
    }

    private fun registerAndAuthenticateUser(username: String, password: String) {
        registerUser(username, password) { jwt ->
            jwt?.let {
                authenticateUser(username, password)
            }
        }
    }

    private fun registerUser(email: String, password: String, onComplete: (String?) -> Unit) {
        val jsonBody = Json.encodeToString(mapOf("email" to email, "password" to password))
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$SERVER_URL/api/user/register")
            .post(requestBody)
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to register user", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to register user: ${response.code}")
                        onComplete(null)
                        return
                    }
                    val jwt = response.body?.string()
                    Log.d(TAG, "JWT token received: $jwt")
                    onComplete(jwt)
                }
            }
        })
    }

//TO DO: РЕГИСТРАЦИЯ ПРОХОДИТ УСПЕШНО, ОТ СЕРВЕРА ПОСТУПАЕТ JWT-TOKEN. НУЖНО ДОРАБОТАТЬ АВТОРИЗАЦИЮ
//TO DO: ПРИ ПОПЫТКЕ АВТОРИЗАЦИИ - ОШИБКА 400 (Failed to authenticate user: 400)
    private fun authenticateUser(email: String, password: String) {
        // Создание тела запроса для аутентификации
        val requestBody = Json.encodeToString(mapOf("email" to email, "password" to password))
        val request = Request.Builder()
            .url("$SERVER_URL/api/user/auth")
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        // Отправка запроса на аутентификацию
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to authenticate user", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to authenticate user: ${response.code}")
                        return
                    }
                    val responseBody = response.body?.string()
                    // Десериализация JSON-ответа
                    val jsonResponse = Json.decodeFromString<Map<String, String>>(responseBody ?: "")
                    val email = jsonResponse["email"]
                    val jwt = jsonResponse["jwt"]
                    if (email != null && jwt != null) {
                        Log.d(TAG, "User authenticated successfully")
                        // Подключение WebSocket после успешной аутентификации
                        connectWebSocket(jwt)
                    } else {
                        Log.e(TAG, "Failed to authenticate user: Invalid response format")
                    }
                }
            }
        })
    }


    private fun connectWebSocket(jwt: String) {
        val client = OkHttpClient.Builder()
            .pingInterval(5, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("$SERVER_URL$WEBSOCKET_ENDPOINT")
            .header("Authorization", "Bearer $jwt")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connection established")
                lifecycleScope.launch {
                    while (true) {
                        delay(UPDATE_INTERVAL)
                        val jsonString = generateJSON(state)
                        webSocket.send(jsonString)
                        Log.d(TAG, "Sent JSON to server: $jsonString")
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message from server: $text")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket connection closed: $code, $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failure", t)
            }
        })
    }

    private fun generateJSON(state: MainActivityState): String {
        val locationData = LocationData(
            state.latitude,
            state.longitude,
            state.rsrp,
            state.rssi,
            state.rsrq,
            state.rssnr,
            state.cqi,
            state.bandwidth,
            state.cellId
        )
        return Json.encodeToString(locationData)
    }

    private fun checkAndRequestPermissions() {
        val context = applicationContext
        if (!checkPermissions(context)) {
            Log.d(TAG, "Requesting permissions")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            // Если разрешения уже предоставлены, начать процесс регистрации и аутентификации
            registerAndAuthenticateUser("test9@gmail.com", "password")
        }
    }

    private fun checkPermissions(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    fun MainContent(state: MainActivityState) {
        val context = LocalContext.current
        var permissionsGranted by remember { mutableStateOf(checkPermissions(context)) }

        LaunchedEffect(context) {
            permissionsGranted = checkPermissions(context)
        }

        if (permissionsGranted) {
            LaunchedEffect(Unit) {
                while (true) {
                    getLocation(state)
                    getSignalStrength(state)
                    delay(UPDATE_INTERVAL)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TabRow(selectedTabIndex = state.selectedTabIndex) {
                    Tab(
                        selected = state.selectedTabIndex == 0,
                        onClick = { state.selectedTabIndex = 0 },
                        text = { Text("Данные") }
                    )
                    Tab(
                        selected = state.selectedTabIndex == 1,
                        onClick = { state.selectedTabIndex = 1 },
                        text = { Text("Графики") }
                    )
                }
                when (state.selectedTabIndex) {
                    0 -> LoginScreen(state)
                    1 -> RSRPGraph(state)
                }
            }
        } else {
            Text("Waiting for permissions...")
        }
    }

    private fun getLocation(state: MainActivityState) {
        Log.d(TAG, "getLocation() called")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "No permission to access location")
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "No permission to write to external storage")
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = UPDATE_INTERVAL
            fastestInterval = UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    state.latitude = location.latitude.toString()
                    state.longitude = location.longitude.toString()
                    Log.d(TAG, "Location received: Lat=${state.latitude}, Lon=${state.longitude}")
                }
            }
        }, null)
    }

    private fun getSignalStrength(state: MainActivityState) {
        if (checkPhoneStatePermission(state.context)) {
            val telephonyManager =
                state.context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList.isNullOrEmpty()) {
                state.rsrp = "CellInfo list is empty"
                state.rssi = "CellInfo list is empty"
                state.rsrq = "CellInfo list is empty"
                state.rssnr = "CellInfo list is empty"
                state.cqi = "CellInfo list is empty"
                state.bandwidth = "CellInfo list is empty"
                state.cellId = "Cell Info not available"
            } else {
                for (info in cellInfoList) {
                    if (info is CellInfoLte) {
                        val cellSignalStrengthLte = info.cellSignalStrength
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            state.rsrp = "${cellSignalStrengthLte.rsrp} dBm"
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                state.rssi = "${cellSignalStrengthLte.rssi} dBm"
                            }
                            state.rsrq = "${cellSignalStrengthLte.rsrq} dB"
                            state.rssnr = "${cellSignalStrengthLte.rssnr} dB"
                            state.cqi = "${cellSignalStrengthLte.cqi}"
                            state.bandwidth = "${telephonyManager.dataNetworkType}"
                            state.cellId = when (val cellLocation = telephonyManager.cellLocation) {
                                is GsmCellLocation -> cellLocation.cid.toString()
                                is CdmaCellLocation -> cellLocation.baseStationId.toString()
                                else -> "Cell ID not available"
                            }
                            Log.d(TAG, "RSRP value: ${state.rsrp}")
                            Log.d(TAG, "RSSI value: ${state.rssi}")
                            Log.d(TAG, "RSRQ value: ${state.rsrq}")
                            Log.d(TAG, "RSSNR value: ${state.rssnr}")
                            Log.d(TAG, "CQI value: ${state.cqi}")
                            Log.d(TAG, "Bandwidth value: ${state.bandwidth}")
                            Log.d(TAG, "Cell ID value: ${state.cellId}")
                        }
                        break
                    }
                }
            }
        } else {
            state.rsrp = "No READ_PHONE_STATE permission"
            state.rssi = "No READ_PHONE_STATE permission"
            state.rsrq = "No READ_PHONE_STATE permission"
            state.rssnr = "No READ_PHONE_STATE permission"
            state.cqi = "No READ_PHONE_STATE permission"
            state.bandwidth = "No READ_PHONE_STATE permission"
            state.cellId = "No READ_PHONE_STATE permission"
        }
    }


    @Composable
    fun LoginScreen(state: MainActivityState) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RSRP value: ${state.rsrp}"
            )
            Text(
                text = "RSSI value: ${state.rssi}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "RSRQ value: ${state.rsrq}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "RSSNR value: ${state.rssnr}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "CQI value: ${state.cqi}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "Bandwidth: ${state.bandwidth}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "Cell ID: ${state.cellId}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "LAT: ${state.latitude}",
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "LON: ${state.longitude}",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    private fun checkPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Composable
    fun RSRPGraph(state: MainActivityState) {
        val cellData = remember { mutableStateListOf<Pair<String, Float>>() }

        LaunchedEffect(state.cellId) {
            cellData.add(Pair(state.cellId, state.rsrp.toFloatOrNull() ?: 0f))
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                color = Color.Black
            )

            if (cellData.size >= 2) {
                val xInterval = size.width / (cellData.size - 1)

                val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint()
                paint.color = Color.Black.toArgb()
                paint.textSize = 30f

                cellData.forEachIndexed { index, pair ->
                    val x = index * xInterval
                    val y = size.height - pair.second
                    drawCircle(color = Color.Blue, center = Offset(x, y), radius = 5f)
                    drawContext.canvas.nativeCanvas.drawText(pair.first, x, size.height + 20f, paint)
                }

                (0 until cellData.size - 1).forEach { index ->
                    val startX = index * xInterval
                    val startY = size.height - cellData[index].second
                    val endX = (index + 1) * xInterval
                    val endY = size.height - cellData[index + 1].second
                    drawLine(start = Offset(startX, startY), end = Offset(endX, endY), color = Color.Red, strokeWidth = 2f)
                }
            }
        }
    }

    class MainActivityState(val context: Context) {
        var latitude by mutableStateOf("")
        var longitude by mutableStateOf("")
        var rsrp by mutableStateOf("")
        var rssi by mutableStateOf("")
        var rsrq by mutableStateOf("")
        var rssnr by mutableStateOf("")
        var cqi by mutableStateOf("")
        var bandwidth by mutableStateOf("")
        var cellId by mutableStateOf("")
        var selectedTabIndex by mutableStateOf(0)
    }


    @Serializable
    data class LocationData(
        val latitude: String,
        val longitude: String,
        val rsrp: String,
        val rssi: String,
        val rsrq: String,
        val rssnr: String,
        val cqi: String,
        val bandwidth: String,
        val cellId: String
    )
}