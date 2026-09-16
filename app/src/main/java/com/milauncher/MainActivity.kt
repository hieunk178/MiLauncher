package com.milauncher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.milauncher.models.AppItem
import com.milauncher.models.HomeCategory
import com.milauncher.models.SidebarItem
import com.milauncher.util.AppLauncher
import com.milauncher.util.SettingsManager
import com.milauncher.util.WallpaperManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class MainActivity : FragmentActivity() {

    private val httpClient = OkHttpClient()
    private var weatherJob: Job? = null
    private lateinit var settings: SettingsManager
    private lateinit var wallpaperManager: WallpaperManager

    // Views
    private lateinit var ivWallpaper: ImageView
    private lateinit var tvGreeting: TextView
    private lateinit var tvWeather: TextView
    private lateinit var rvSidebar: RecyclerView
    private lateinit var rvHome: RecyclerView
    private lateinit var searchContainer: View
    private lateinit var searchInput: EditText
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var ivNetworkStatus: ImageView
    private lateinit var btnHeaderSettings: ImageView

    // Adapters
    private lateinit var sidebarAdapter: SidebarAdapter
    private lateinit var homeAdapter: HomeAdapter

    // State
    private var currentSection = SidebarItem.Type.ALL_APPS
    private var allTvApps: List<AppItem> = emptyList()
    private var allPhoneApps: List<AppItem> = emptyList()

    private val LOCATION_PERMISSION_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsManager(this)
        wallpaperManager = WallpaperManager(this, settings)

        // Bind views
        ivWallpaper = findViewById(R.id.iv_wallpaper)
        tvGreeting = findViewById(R.id.tv_greeting)
        tvWeather = findViewById(R.id.tv_weather)
        rvSidebar = findViewById(R.id.rv_sidebar)
        rvHome = findViewById(R.id.rv_home_categories)
        searchContainer = findViewById(R.id.search_container)
        ivNetworkStatus = findViewById(R.id.iv_network_status)
        btnHeaderSettings = findViewById(R.id.btn_header_settings)

        btnHeaderSettings.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }
        ivNetworkStatus.setOnClickListener {
            try {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            } catch (e: Exception) {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS))
            }
        }

        setupGreeting()
        setupSidebar()
        setupHomeRecyclerView()
        setupSearchContainer()

        // Start wallpaper auto-fetch
        wallpaperManager.start(lifecycleScope, ivWallpaper)

        // Load apps
        loadData(SidebarItem.Type.ALL_APPS)

        // Weather
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startWeatherUpdater()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
            startWeatherUpdater(21.0285, 105.8542, "Hà Nội")
        }
    }

    private fun setupGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour in 5..10 -> "Chào buổi sáng ☀️"
            hour in 11..13 -> "Buổi trưa vui vẻ 🌤"
            hour in 14..17 -> "Chào buổi chiều 🌇"
            hour in 18..21 -> "Chào buổi tối 🌙"
            else -> "Khuya rồi đấy 🌃"
        }
        val name = settings.ownerName
        if (name.isNotEmpty()) {
            if (name.length > 12) {
                tvGreeting.text = "$greeting\n$name"
            } else {
                tvGreeting.text = "$greeting, $name"
            }
        } else {
            tvGreeting.text = greeting
        }
        if (!settings.showGreeting) tvGreeting.visibility = View.GONE
    }

    private fun setupSidebar() {
        val sidebarItems = listOf(
            SidebarItem(1, "HDMI", R.drawable.ic_hdmi_sidebar, SidebarItem.Type.HDMI),
            SidebarItem(2, "TV App", R.drawable.ic_apps, SidebarItem.Type.ALL_APPS),
            SidebarItem(3, "Mobile", R.drawable.ic_phone_app, SidebarItem.Type.PHONE_APPS),
            SidebarItem(4, "Tìm", R.drawable.ic_search, SidebarItem.Type.SEARCH),
            SidebarItem(5, "Store", R.drawable.ic_store, SidebarItem.Type.APP_STORE),
            SidebarItem(6, "Cài đặt", R.drawable.ic_settings, SidebarItem.Type.SETTINGS)
        )

        sidebarAdapter = SidebarAdapter(this, sidebarItems) { item ->
            onSidebarItemSelected(item)
        }

        rvSidebar.layoutManager = LinearLayoutManager(this)
        rvSidebar.adapter = sidebarAdapter
        // Default: select TV Apps (index 1)
        sidebarAdapter.setSelected(1)
    }

    private fun onSidebarItemSelected(item: SidebarItem) {
        currentSection = item.type
        when (item.type) {
            SidebarItem.Type.HDMI -> {
                searchContainer.visibility = View.GONE
                rvHome.visibility = View.VISIBLE
                loadData(SidebarItem.Type.HDMI)
            }
            SidebarItem.Type.ALL_APPS -> {
                searchContainer.visibility = View.GONE
                rvHome.visibility = View.VISIBLE
                loadData(SidebarItem.Type.ALL_APPS)
            }
            SidebarItem.Type.PHONE_APPS -> {
                searchContainer.visibility = View.GONE
                rvHome.visibility = View.VISIBLE
                loadData(SidebarItem.Type.PHONE_APPS)
            }
            SidebarItem.Type.SEARCH -> {
                rvHome.visibility = View.GONE
                searchContainer.visibility = View.VISIBLE
                searchInput.requestFocus()
            }
            SidebarItem.Type.APP_STORE -> {
                AppLauncher.launchAppStore(this)
            }
            SidebarItem.Type.SETTINGS -> {
                startActivity(android.content.Intent(this, SettingsActivity::class.java))
                overridePendingTransition(R.anim.slide_in_right, 0)
            }
            else -> {}
        }
    }

    private fun setupHomeRecyclerView() {
        homeAdapter = HomeAdapter(this, emptyList())
        rvHome.layoutManager = LinearLayoutManager(this)
        rvHome.adapter = homeAdapter
    }

    private fun setupSearchContainer() {
        // Inflate search layout into container
        val searchView = layoutInflater.inflate(R.layout.fragment_search, searchContainer as android.view.ViewGroup, false)
        (searchContainer as android.view.ViewGroup).addView(searchView)

        searchInput = searchView.findViewById(R.id.search_input)
        rvSearchResults = searchView.findViewById(R.id.rv_search_results)
        rvSearchResults.layoutManager = GridLayoutManager(this, 6)

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterSearch(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                filterSearch(searchInput.text.toString())
                true
            } else false
        }
    }

    private fun filterSearch(query: String) {
        val all = (allTvApps + allPhoneApps).distinctBy { it.packageName }
        val filtered = if (query.isEmpty()) all
        else all.filter { it.title.contains(query, ignoreCase = true) }
        rvSearchResults.adapter = CategoryItemAdapter(this, filtered, 0)
    }

    private fun loadData(section: SidebarItem.Type) {
        lifecycleScope.launch(Dispatchers.IO) {
            val categories = mutableListOf<HomeCategory>()

            when (section) {
                SidebarItem.Type.HDMI -> {
                    val hdmiItems = listOf(
                        AppItem(1, "HDMI 1", null, AppItem.ItemType.HDMI, subtitle = "Cổng HDMI 1"),
                        AppItem(2, "HDMI 2", null, AppItem.ItemType.HDMI, subtitle = "Cổng HDMI 2"),
                        AppItem(3, "HDMI 3", null, AppItem.ItemType.HDMI, subtitle = "Cổng HDMI 3"),
                        AppItem(4, "AV", null, AppItem.ItemType.HDMI, subtitle = "Cổng AV")
                    )
                    categories.add(HomeCategory(1, "📺 Nguồn phát", 0, 1, 4, hdmiItems))
                }
                SidebarItem.Type.ALL_APPS -> {
                    if (allTvApps.isEmpty()) allTvApps = getTvApps()
                    val featuredPkgs = settings.featuredPackages
                    val featured = allTvApps.filter { featuredPkgs.contains(it.packageName) }
                        .map { it.copy(isFeatured = true) }
                    val rest = allTvApps.filter { !featuredPkgs.contains(it.packageName) }

                    if (featured.isNotEmpty()) {
                        categories.add(HomeCategory(0, "Nổi bật", 0, 2, 0, featured, isFeatured = true))
                    }
                    categories.add(HomeCategory(2, "🎮 TV Apps", settings.appsLayout, settings.appsSize, settings.appsColumns, rest))
                }
                SidebarItem.Type.PHONE_APPS -> {
                    if (allPhoneApps.isEmpty()) allPhoneApps = getPhoneApps()
                    categories.add(HomeCategory(3, "📱 Ứng dụng điện thoại", settings.phoneAppsLayout, settings.phoneAppsSize, settings.phoneAppsColumns, allPhoneApps))
                }
                else -> {
                    if (allTvApps.isEmpty()) allTvApps = getTvApps()
                    categories.add(HomeCategory(2, "🎮 TV Apps", settings.appsLayout, settings.appsSize, settings.appsColumns, allTvApps))
                }
            }

            withContext(Dispatchers.Main) {
                homeAdapter.updateData(categories)
            }
        }
    }

    private fun getTvApps(): List<AppItem> {
        val pm = packageManager
        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LEANBACK_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .filter { (it.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .distinctBy { it.activityInfo.packageName }

        var id = 100L
        return apps.map { info ->
            val banner = try { info.activityInfo.loadBanner(pm) ?: info.activityInfo.applicationInfo.loadBanner(pm) } catch (e: Exception) { null }
            AppItem(id++, info.loadLabel(pm).toString(), info.activityInfo.packageName,
                AppItem.ItemType.APP, iconDrawable = info.loadIcon(pm), bannerDrawable = banner)
        }.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun getPhoneApps(): List<AppItem> {
        val pm = packageManager
        val tvPackages = getTvApps().map { it.packageName }.toSet()

        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN, null)
        intent.addCategory(android.content.Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .filter { !tvPackages.contains(it.activityInfo.packageName) }
            .filter { (it.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .distinctBy { it.activityInfo.packageName }

        var id = 1000L
        return apps.map { info ->
            AppItem(id++, info.loadLabel(pm).toString(), info.activityInfo.packageName,
                AppItem.ItemType.PHONE_APP, iconDrawable = info.loadIcon(pm))
        }.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    // ── Key Navigation ───────────────────────────────────────────────────────
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                android.view.KeyEvent.KEYCODE_DPAD_LEFT -> {
                    // Focus to sidebar when pressing left at leftmost content item
                    val focused = currentFocus
                    if (focused != null && rvSidebar.findContainingViewHolder(focused) == null) {
                        val nextFocus = focused.focusSearch(View.FOCUS_LEFT)
                        if (nextFocus == null || rvSidebar.findContainingViewHolder(nextFocus) != null) {
                            val selectedIndex = sidebarAdapter.getSelectedPosition()
                            val sidebarItem = rvSidebar.layoutManager?.findViewByPosition(selectedIndex) ?: rvSidebar.getChildAt(0)
                            sidebarItem?.requestFocus()
                            return true
                        }
                    }
                }
                android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    // Focus to content when pressing right from sidebar
                    val focused = currentFocus
                    if (focused != null && rvSidebar.findContainingViewHolder(focused) != null) {
                        rvHome.getChildAt(0)?.requestFocus()
                            ?: rvSearchResults.getChildAt(0)?.requestFocus()
                        return true
                    }
                }
                android.view.KeyEvent.KEYCODE_BACK -> {
                    // If in search, close search
                    if (searchContainer.visibility == View.VISIBLE) {
                        onSidebarItemSelected(SidebarItem(2, "TV App", R.drawable.ic_apps, SidebarItem.Type.ALL_APPS))
                        sidebarAdapter.setSelected(1)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ── Weather ─────────────────────────────────────────────────────────────
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startWeatherUpdater()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupGreeting()
        loadData(currentSection)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startWeatherUpdater()
        }
        startNetworkMonitor()
    }

    override fun onPause() {
        super.onPause()
        weatherJob?.cancel()
        networkJob?.cancel()
        wallpaperManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        wallpaperManager.stop()
        networkJob?.cancel()
    }

    private var networkJob: Job? = null

    private fun startNetworkMonitor() {
        networkJob?.cancel()
        networkJob = lifecycleScope.launch {
            while (isActive) {
                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val net = cm.activeNetwork
                val caps = cm.getNetworkCapabilities(net)
                
                val iconRes = when {
                    caps == null -> R.drawable.ic_wifi_off
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> R.drawable.ic_ethernet
                    else -> R.drawable.ic_wifi
                }
                ivNetworkStatus.setImageResource(iconRes)
                ivNetworkStatus.alpha = if (caps == null) 0.5f else 1.0f
                
                delay(3000L)
            }
        }
    }

    private fun startWeatherUpdater(lat: Double? = null, lon: Double? = null, name: String? = null) {
        weatherJob?.cancel()
        weatherJob = lifecycleScope.launch {
            while (isActive) {
                if (lat != null && lon != null && name != null) {
                    fetchWeather(lat, lon, name)
                } else {
                    updateLocationAndWeather()
                }
                delay(60 * 60 * 1000L)
            }
        }
    }

    private suspend fun updateLocationAndWeather() {
        withContext(Dispatchers.IO) {
            try {
                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                    val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val location: Location? = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    var lat = 21.0285; var lon = 105.8542; var locName = "Hà Nội"
                    if (location != null) {
                        lat = location.latitude; lon = location.longitude
                        try {
                            val geo = Geocoder(this@MainActivity, Locale("vi", "VN"))
                            val addrs = geo.getFromLocation(lat, lon, 1)
                            if (!addrs.isNullOrEmpty()) {
                                val a = addrs[0]
                                locName = a.subLocality ?: a.locality ?: a.adminArea ?: "Vị trí hiện tại"
                            }
                        } catch (e: Exception) {}
                    }
                    fetchWeather(lat, lon, locName)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private suspend fun fetchWeather(lat: Double, lon: Double, locName: String) {
        withContext(Dispatchers.IO) {
            try {
                if (!settings.showWeather) return@withContext
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
                val resp = httpClient.newCall(Request.Builder().url(url).build()).execute()
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string() ?: return@withContext)
                    val temp = json.getJSONObject("current_weather").getDouble("temperature")
                    val wmo = json.getJSONObject("current_weather").getInt("weathercode")
                    val icon = weatherIcon(wmo)
                    withContext(Dispatchers.Main) {
                        tvWeather.text = "$icon ${temp}°C • $locName"
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun weatherIcon(wmo: Int): String = when (wmo) {
        0 -> "☀️"; in 1..3 -> "⛅"; in 45..48 -> "🌫️"
        in 51..67 -> "🌧️"; in 71..77 -> "❄️"; in 80..82 -> "🌦️"
        in 95..99 -> "⛈️"; else -> "🌡️"
    }
}
