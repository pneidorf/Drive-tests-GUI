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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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
                authenticateUser(username, password, it)
            }
        }
    }

    private fun registerUser(email: String, password: String, onComplete: (String?) -> Unit) {
        val requestBody = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

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

    private fun authenticateUser(email: String, password: String, jwt: String) {
        val requestBody = FormBody.Builder()
            .add("username", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("$SERVER_URL/api/user/auth")
            .header("Authorization", "Bearer $jwt")
            .post(requestBody)
            .build()

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
                    Log.d(TAG, "User authenticated successfully")
                    // Логика при успешной аутентификации
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
            registerAndAuthenticateUser("example@example.com", "password")
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
                LoginScreen(state)
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
                        }
                        Log.d(TAG, "RSRP value: ${state.rsrp}")
                        Log.d(TAG, "RSSI value: ${state.rssi}")
                        Log.d(TAG, "RSRQ value: ${state.rsrq}")
                        Log.d(TAG, "RSSNR value: ${state.rssnr}")
                        Log.d(TAG, "CQI value: ${state.cqi}")
                        Log.d(TAG, "Bandwidth value: ${state.bandwidth}")
                        Log.d(TAG, "Cell ID value: ${state.cellId}")
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
