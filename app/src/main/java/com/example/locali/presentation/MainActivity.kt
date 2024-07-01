package com.example.locali.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import android.location.LocationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import android.widget.Toast

class MainActivity : ComponentActivity() {
    val isTrainingMode = mutableStateOf(false)
    val gpsStatus = mutableStateOf(false)
    val internetStatus = mutableStateOf(false)
    var lastUpdateTime = mutableStateOf("N/A")
    private lateinit var locationService: LocationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationService = LocationService(this)
        checkAndRequestPermissions()
        setContent {
            LocalMeApp(
                isTrainingMode = isTrainingMode,
                onModeChange = { switchMode(it) },
                lastUpdateTime = lastUpdateTime.value,
                gpsStatus = gpsStatus,
                internetStatus = internetStatus
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_WIFI_STATE
        )
        val permissionsToRequest = permissions.filter {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            initializeServices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeServices()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeServices() {
        locationService.startLocationUpdates()
        checkGpsStatus()
        checkInternetConnection()
    }

    private fun checkGpsStatus() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gpsStatus.value = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        Log.d("MainActivity", "GPS Status: ${gpsStatus.value}")
    }

    private fun checkInternetConnection() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        internetStatus.value = capabilities != null &&
                (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        Log.d("MainActivity", "Internet Status: ${internetStatus.value}")
    }

    private fun switchMode(isTraining: Boolean) {
        isTrainingMode.value = isTraining
        locationService.setMode(isTraining)
    }

    companion object {
        private const val REQUEST_PERMISSIONS = 1
    }
}
