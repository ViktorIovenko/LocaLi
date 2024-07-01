package com.example.locali.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class LocationService(private val activity: MainActivity) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private val serverUrl = "https://iovenko.eu/api/coordinates"

    init {
        initializeLocationRequest()
        initializeLocationCallback()
    }

    private fun initializeLocationRequest() {
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, if (activity.isTrainingMode.value) 10000 else 60000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(if (activity.isTrainingMode.value) 10000 else 60000)
            .setMaxUpdateDelayMillis(if (activity.isTrainingMode.value) 20000 else 120000)
            .build()
    }

    private fun initializeLocationCallback() {
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
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    fun restartLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        startLocationUpdates()
    }

    private fun handleNewLocation(location: Location) {
        activity.gpsStatus.value = true
        sendLocationToServer(location)
    }

    private fun sendLocationToServer(location: Location) {
        if (!checkInternetConnection()) {
            Toast.makeText(activity, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        val client = OkHttpClient()
        val json = JSONObject().apply {
            put("latitude", location.latitude)
            put("longitude", location.longitude)
            put("time", System.currentTimeMillis())
            put("mode", if (activity.isTrainingMode.value) "training" else "normal")
            put("deviceName", "L")  // or "R" for the other device
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
                    Log.e("LocalMeApp", "Failed to send data", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                activity.runOnUiThread {
                    if (response.isSuccessful) {
                        activity.internetStatus.value = true
                        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        activity.lastUpdateTime.value = sdf.format(Date())
                        Toast.makeText(activity, "Data sent successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        activity.internetStatus.value = false
                        Toast.makeText(activity, "Failed to send data: ${response.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LocalMeApp", "Failed to send data: ${response.message}")
                    }
                }
            }
        })
    }

    private fun checkInternetConnection(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isConnected = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))

        activity.internetStatus.value = isConnected
        return isConnected
    }

    private fun getBatteryLevel(): Int {
        // Implement battery level retrieval
        return 100  // Placeholder
    }

    private fun getSignalQuality(): Int {
        // Implement signal quality retrieval
        return 100  // Placeholder
    }

    private fun getInternetType(): String {
        // Implement internet type retrieval
        return "WiFi"  // Placeholder
    }
}
