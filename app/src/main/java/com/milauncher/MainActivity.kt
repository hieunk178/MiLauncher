package com.milauncher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

class MainActivity : FragmentActivity() {
    
    private val httpClient = OkHttpClient()
    private var weatherJob: Job? = null
    private lateinit var tvWeather: TextView

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var settings: com.milauncher.util.SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        settings = com.milauncher.util.SettingsManager(this)
        
        tvWeather = findViewById(R.id.tv_weather)
        
        findViewById<android.widget.ImageButton>(R.id.btn_settings).setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, 0)
        }
        
        val rvHomeCategories = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_home_categories)
        rvHomeCategories.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        
        homeAdapter = HomeAdapter(this, emptyList())
        rvHomeCategories.adapter = homeAdapter

        loadData()

        checkLocationPermissions()
    }
    
    private lateinit var homeAdapter: HomeAdapter

    private fun loadData() {
        val categories = mutableListOf<com.milauncher.models.HomeCategory>()

        // 1. HDMI Items
        if (settings.showHdmi) {
            val hdmiItems = listOf(
                com.milauncher.models.AppItem(1, "HDMI 1", null, com.milauncher.models.AppItem.ItemType.HDMI),
                com.milauncher.models.AppItem(2, "HDMI 2", null, com.milauncher.models.AppItem.ItemType.HDMI),
                com.milauncher.models.AppItem(3, "HDMI 3", null, com.milauncher.models.AppItem.ItemType.HDMI)
            )
            categories.add(com.milauncher.models.HomeCategory(1, "Nguồn phát", settings.hdmiLayout, settings.hdmiSize, settings.hdmiColumns, hdmiItems))
        }

        // 2. Apps Items
        if (settings.showApps) {
            val appsList = listOf(
                com.milauncher.models.AppItem(4, "FPT Play", "com.fplay.alotv", com.milauncher.models.AppItem.ItemType.APP),
                com.milauncher.models.AppItem(5, "VieON", "viva.vieon.tv", com.milauncher.models.AppItem.ItemType.APP),
                com.milauncher.models.AppItem(6, "TV360", "com.viettel.tv360.tv", com.milauncher.models.AppItem.ItemType.APP),
                com.milauncher.models.AppItem(7, "K+", "com.vstv.kplus.tv", com.milauncher.models.AppItem.ItemType.APP),
                com.milauncher.models.AppItem(8, "VTV Go", "vn.vtv.vtvgo.tv", com.milauncher.models.AppItem.ItemType.APP)
            )
            categories.add(com.milauncher.models.HomeCategory(2, "Ứng dụng", settings.appsLayout, settings.appsSize, settings.appsColumns, appsList))
        }

        homeAdapter.updateData(categories)
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startWeatherUpdater()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWeatherUpdater()
            } else {
                startWeatherUpdater(21.0285, 105.8542, "Hà Nội")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startWeatherUpdater()
        }
    }

    override fun onPause() {
        super.onPause()
        weatherJob?.cancel()
    }

    private fun startWeatherUpdater(fallbackLat: Double? = null, fallbackLon: Double? = null, fallbackName: String? = null) {
        weatherJob?.cancel()
        weatherJob = lifecycleScope.launch {
            while (isActive) {
                if (fallbackLat != null && fallbackLon != null && fallbackName != null) {
                    fetchWeather(fallbackLat, fallbackLon, fallbackName)
                } else {
                    updateLocationAndWeather()
                }
                delay(60 * 60 * 1000L) // 1 hour
            }
        }
    }

    private suspend fun updateLocationAndWeather() {
        withContext(Dispatchers.IO) {
            try {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location: Location? = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    
                    var lat = 21.0285
                    var lon = 105.8542
                    var locationName = "Hà Nội"

                    if (location != null) {
                        lat = location.latitude
                        lon = location.longitude
                        try {
                            val geocoder = Geocoder(this@MainActivity, Locale("vi", "VN"))
                            val addresses = geocoder.getFromLocation(lat, lon, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val address = addresses[0]
                                val subLocality = address.subLocality ?: address.locality ?: address.subAdminArea ?: ""
                                val adminArea = address.adminArea ?: ""
                                locationName = if (subLocality.isNotEmpty() && adminArea.isNotEmpty()) {
                                    "$subLocality, $adminArea"
                                } else if (adminArea.isNotEmpty()) {
                                    adminArea
                                } else {
                                    "Vị trí hiện tại"
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    fetchWeather(lat, lon, locationName)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double, locationName: String) {
        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (responseBody != null) {
                        val json = JSONObject(responseBody)
                        val current = json.getJSONObject("current_weather")
                        val temp = current.getDouble("temperature")
                        
                        withContext(Dispatchers.Main) {
                            tvWeather.text = "${temp}°C • $locationName"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN && event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
            val focusedView = currentFocus
            if (focusedView != null && focusedView.id != R.id.btn_settings) {
                // Check if Leanback can find a view above
                val nextFocus = focusedView.focusSearch(android.view.View.FOCUS_UP)
                if (nextFocus == null || nextFocus == focusedView) {
                    // We reached the top, jump to btn_settings
                    val btnSettings = findViewById<android.widget.ImageButton>(R.id.btn_settings)
                    if (btnSettings != null) {
                        btnSettings.requestFocus()
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
