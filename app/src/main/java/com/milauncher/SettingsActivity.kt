package com.milauncher

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.milauncher.util.OtaUpdater
import com.milauncher.util.SettingsManager
import kotlinx.coroutines.launch

class SettingsActivity : FragmentActivity() {

    private lateinit var settings: SettingsManager
    private lateinit var flipper: ViewFlipper
    
    // Screen 0
    private lateinit var btnMenuCategories: Button
    private lateinit var btnOta: Button
    
    // Screen 1
    private lateinit var btnCatHdmi: Button
    private lateinit var btnCatApps: Button
    
    // Screen 2
    private lateinit var tvConfigTitle: TextView
    private lateinit var btnToggleVisibility: Button
    private lateinit var btnCatLayoutType: Button
    private lateinit var btnCatCardSize: Button
    private lateinit var btnCatColumns: Button
    
    // State for Screen 2
    private var currentConfigCategory = "" // "HDMI" or "APPS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settings = SettingsManager(this)
        flipper = findViewById(R.id.settings_flipper)

        // Screen 0
        btnMenuCategories = findViewById(R.id.btn_menu_categories)
        btnOta = findViewById(R.id.btn_ota)
        
        // Screen 1
        btnCatHdmi = findViewById(R.id.btn_cat_hdmi)
        btnCatApps = findViewById(R.id.btn_cat_apps)
        
        // Screen 2
        tvConfigTitle = findViewById(R.id.tv_config_title)
        btnToggleVisibility = findViewById(R.id.btn_toggle_visibility)
        btnCatLayoutType = findViewById(R.id.btn_cat_layout_type)
        btnCatCardSize = findViewById(R.id.btn_cat_card_size)
        btnCatColumns = findViewById(R.id.btn_cat_columns)

        // --- SCREEN 0 LISTENERS ---
        btnMenuCategories.setOnClickListener {
            flipper.showNext() // Go to Screen 1
            btnCatHdmi.requestFocus()
        }
        
        btnOta.setOnClickListener {
            lifecycleScope.launch {
                OtaUpdater.checkAndUpdate(this@SettingsActivity)
            }
        }

        // --- SCREEN 1 LISTENERS ---
        btnCatHdmi.setOnClickListener {
            currentConfigCategory = "HDMI"
            updateScreen2Ui()
            flipper.showNext() // Go to Screen 2
            btnToggleVisibility.requestFocus()
        }
        
        btnCatApps.setOnClickListener {
            currentConfigCategory = "APPS"
            updateScreen2Ui()
            flipper.showNext() // Go to Screen 2
            btnToggleVisibility.requestFocus()
        }

        // --- SCREEN 2 LISTENERS ---
        btnToggleVisibility.setOnClickListener {
            if (currentConfigCategory == "HDMI") {
                settings.showHdmi = !settings.showHdmi
            } else if (currentConfigCategory == "APPS") {
                settings.showApps = !settings.showApps
            }
            updateScreen2Ui()
        }
        
        btnCatLayoutType.setOnClickListener {
            if (currentConfigCategory == "HDMI") {
                settings.hdmiLayout = if (settings.hdmiLayout == 0) 1 else 0
            } else if (currentConfigCategory == "APPS") {
                settings.appsLayout = if (settings.appsLayout == 0) 1 else 0
            }
            updateScreen2Ui()
        }

        btnCatCardSize.setOnClickListener {
            if (currentConfigCategory == "HDMI") {
                settings.hdmiSize = (settings.hdmiSize + 1) % 3
                // Reset columns when size changes
                settings.hdmiColumns = 0
            } else if (currentConfigCategory == "APPS") {
                settings.appsSize = (settings.appsSize + 1) % 3
                // Reset columns when size changes
                settings.appsColumns = 0
            }
            updateScreen2Ui()
        }

        btnCatColumns.setOnClickListener {
            val maxColumns = getMaxColumnsForCurrentSize()
            if (currentConfigCategory == "HDMI") {
                var cols = settings.hdmiColumns + 1
                if (cols > maxColumns) cols = 0
                settings.hdmiColumns = cols
            } else if (currentConfigCategory == "APPS") {
                var cols = settings.appsColumns + 1
                if (cols > maxColumns) cols = 0
                settings.appsColumns = cols
            }
            updateScreen2Ui()
        }
    }

    private fun getMaxColumnsForCurrentSize(): Int {
        val size = if (currentConfigCategory == "HDMI") settings.hdmiSize else settings.appsSize
        val widthDp = when (size) {
            0 -> 160
            1 -> 200
            else -> 240
        }
        // Approximate width: 1920px width TV is about 960dp on mdpi. 
        // Let's use standard Android TV width (960dp) minus padding (48*2 = 96dp) = 864dp
        val availableWidthDp = 864
        val marginDp = 16 // 8dp on each side
        return availableWidthDp / (widthDp + marginDp)
    }
    
    private fun updateScreen2Ui() {
        if (currentConfigCategory == "HDMI") {
            tvConfigTitle.text = "Cấu hình: Nguồn HDMI"
            btnToggleVisibility.text = "Trạng thái: ${if (settings.showHdmi) "ĐANG BẬT" else "ĐANG TẮT"}"
            btnCatLayoutType.text = "Kiểu Hiển Thị: ${if (settings.hdmiLayout == 0) "DẠNG TRƯỢT (ROW)" else "DẠNG LƯỚI (GRID)"}"
            val sizeText = when(settings.hdmiSize) { 0 -> "NHỎ"; 1 -> "VỪA"; else -> "LỚN" }
            btnCatCardSize.text = "Kích Thước Thẻ: $sizeText"
            btnCatColumns.text = "Số mục hiển thị: ${if (settings.hdmiColumns == 0) "TỰ ĐỘNG" else settings.hdmiColumns.toString()}"
        } else if (currentConfigCategory == "APPS") {
            tvConfigTitle.text = "Cấu hình: Ứng dụng"
            btnToggleVisibility.text = "Trạng thái: ${if (settings.showApps) "ĐANG BẬT" else "ĐANG TẮT"}"
            btnCatLayoutType.text = "Kiểu Hiển Thị: ${if (settings.appsLayout == 0) "DẠNG TRƯỢT (ROW)" else "DẠNG LƯỚI (GRID)"}"
            val sizeText = when(settings.appsSize) { 0 -> "NHỎ"; 1 -> "VỪA"; else -> "LỚN" }
            btnCatCardSize.text = "Kích Thước Thẻ: $sizeText"
            btnCatColumns.text = "Số mục hiển thị: ${if (settings.appsColumns == 0) "TỰ ĐỘNG" else settings.appsColumns.toString()}"
        }
    }

    override fun onBackPressed() {
        if (flipper.displayedChild > 0) {
            // Go back one screen
            flipper.showPrevious()
            
            // Restore focus properly
            if (flipper.displayedChild == 0) {
                btnMenuCategories.requestFocus()
            } else if (flipper.displayedChild == 1) {
                if (currentConfigCategory == "HDMI") btnCatHdmi.requestFocus()
                else btnCatApps.requestFocus()
            }
        } else {
            super.onBackPressed()
            overridePendingTransition(0, R.anim.slide_out_right)
        }
    }
}
