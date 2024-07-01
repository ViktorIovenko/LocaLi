package com.example.locali.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule

class LocationService(private val activity: MainActivity) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val serverUrl = "https://iovenko.eu/wp-json/localme/v1/coordinates"
    private val timer = Timer()
    private var location: Location? = null
    init {
        initializeLocationRequest()
        initializeLocationCallback()
        startLocationUpdates()
    }
    private fun initializeLocationRequest() {
        Log.d("LocationService", "Initializing location request")
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, getInterval())
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(getInterval())
            .setMaxUpdateDelayMillis(getMaxUpdateDelay())
            .build()
    }

    private fun getInterval(): Long {
        return if (activity.isTrainingMode.value) 10000L else 60000L
    }

    private fun getMaxUpdateDelay(): Long {
        return if (activity.isTrainingMode.value) 20000L else 120000L
    }

    private fun initializeLocationCallback() {
        Log.d("LocationService", "Initializing location callback")
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    handleNewLocation(location)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        Log.d("LocationService", "Starting location updates")
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        startSendingData()
    }

    fun restartLocationUpdates() {
        Log.d("LocationService", "Restarting location updates")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        startLocationUpdates()
    }

    fun setMode(isTraining: Boolean) {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, getInterval())
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(getInterval())
            .setMaxUpdateDelayMillis(getMaxUpdateDelay())
            .build()
        restartLocationUpdates()
    }

    private fun handleNewLocation(location: Location) {
        Log.d("LocationService", "New location received: $location")
        activity.gpsStatus.value = true
        this.location = location
    }

    private fun startSendingData() {
        val interval = if (activity.isTrainingMode.value) 20000L else 120000L // 20 seconds for training, 2 minutes for normal
        timer.schedule(0, interval) {
            location?.let {
                sendLocationToServer(it)
            }
        }
    }

    private fun sendLocationToServer(location: Location) {
        if (!checkInternetConnection()) {
            Toast.makeText(activity, "No internet connection", Toast.LENGTH_SHORT).show()
            Log.e("LocationService", "No internet connection")
            return
        }
        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("time", System.currentTimeMillis())
            put("mode", if (activity.isTrainingMode.value) "training" else "normal")
            put("deviceName", "L")
            put("batteryLevel", getBatteryLevel())
            put("signalQuality", getSignalQuality())
            put("internetType", getInternetType())
        }
        val requestBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity.runOnUiThread {
                    activity.internetStatus.value = false
                    Toast.makeText(activity, "Failed to send data: ${e.message}", Toast.LENGTH_SHORT).show()
                    logError("Failed to send data: ${e.message}")
                    Log.e("LocationService", "Failed to send data", e)
                }
                retrySendLocationToServer(location)
            }

            override fun onResponse(call: Call, response: Response) {
                activity.runOnUiThread {
                    if (response.isSuccessful) {
                        activity.internetStatus.value = true
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        activity.lastUpdateTime.value = sdf.format(Date())
                        Toast.makeText(activity, "Data sent successfully", Toast.LENGTH_SHORT).show()
                        Log.d("LocationService", "Data sent successfully")
                    } else {
                        activity.internetStatus.value = false
                        Toast.makeText(activity, "Failed to send data: ${response.message}", Toast.LENGTH_SHORT).show()
                        logError("Failed to send data: ${response.message}")
                        Log.e("LocationService", "Failed to send data: ${response.message}")
                        retrySendLocationToServer(location)
                    }
                }
            }
        })
    }

    private fun retrySendLocationToServer(location: Location) {
        timer.schedule(30000L) {
            sendLocationToServer(location)
        }
    }

    private fun checkInternetConnection(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        activity.internetStatus.value = isConnected
        Log.d("LocationService", "Internet connection status: $isConnected")
        return isConnected
    }

    private fun getBatteryLevel(): Int {
        val batteryStatus = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    private fun getSignalQuality(): Int {
        // Implement signal quality retrieval
        return 100  // Placeholder
    }

    private fun getInternetType(): String {
        // Implement internet type retrieval
        return "WiFi"  // Placeholder
    }

    private fun logError(error: String) {
        val logFile = File(activity.filesDir, "error_log.txt")
        logFile.appendText("$error\n")
        // Удаление логов старше 2 дней
        val logFiles = activity.filesDir.listFiles { file -> file.name.startsWith("error_log") }
        logFiles?.forEach { file ->
            if (file.lastModified() < System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000) {
                file.delete()
            }
        }
    }
}
