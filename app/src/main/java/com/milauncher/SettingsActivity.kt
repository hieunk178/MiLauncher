package com.milauncher

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.ViewFlipper
import android.widget.PopupMenu
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.milauncher.util.AppLauncher
import com.milauncher.util.OtaUpdater
import com.milauncher.util.SettingsManager
import kotlinx.coroutines.launch
import android.content.Intent

class SettingsActivity : FragmentActivity() {

    private lateinit var settings: SettingsManager
    private lateinit var flipper: ViewFlipper
    
    // Screen 0
    private lateinit var btnMenuCategories: Button
    private lateinit var btnFocusAnimation: Button
    private lateinit var btnOwnerName: Button
    private lateinit var btnDeviceSettings: Button
    private lateinit var btnAppStore: Button
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
        btnFocusAnimation = findViewById(R.id.btn_focus_animation)
        btnOwnerName = findViewById(R.id.btn_owner_name)
        btnDeviceSettings = findViewById(R.id.btn_device_settings)
        btnAppStore = findViewById(R.id.btn_app_store)
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

        // Update initial UI for screen 0
        updateFocusAnimationBtnText()

        // --- SCREEN 0 LISTENERS ---
        btnMenuCategories.setOnClickListener {
            flipper.showNext() // Go to Screen 1
            btnCatHdmi.requestFocus()
        }
        
        btnFocusAnimation.setOnClickListener { view ->
            val options = listOf("MẶC ĐỊNH", "NHỊP TIM ĐẬP", "HƠI THỞ")
            showDropdownMenu(view, options) { selectedIndex ->
                settings.focusAnimationStyle = selectedIndex
                updateFocusAnimationBtnText()
            }
        }
        
        btnOwnerName.text = "Tên chủ sở hữu: ${if (settings.ownerName.isEmpty()) "CHƯA ĐẶT" else settings.ownerName}"
        btnOwnerName.setOnClickListener {
            val input = android.widget.EditText(this).apply {
                setText(settings.ownerName)
                setSingleLine()
            }
            // Optional: add some padding to the EditText
            val container = android.widget.FrameLayout(this)
            val params = android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(48, 24, 48, 24)
            input.layoutParams = params
            container.addView(input)

            android.app.AlertDialog.Builder(this)
                .setTitle("Nhập tên chủ sở hữu")
                .setView(container)
                .setPositiveButton("Lưu") { _, _ ->
                    settings.ownerName = input.text.toString().trim()
                    btnOwnerName.text = "Tên chủ sở hữu: ${if (settings.ownerName.isEmpty()) "CHƯA ĐẶT" else settings.ownerName}"
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
        
        btnDeviceSettings.setOnClickListener {
            AppLauncher.launchSettings(this)
        }

        btnAppStore.setOnClickListener {
            AppLauncher.launchAppStore(this)
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
        
        btnCatLayoutType.setOnClickListener { view ->
            val options = listOf("DẠNG TRƯỢT (ROW)", "DẠNG LƯỚI (GRID)")
            showDropdownMenu(view, options) { selectedIndex ->
                if (currentConfigCategory == "HDMI") {
                    settings.hdmiLayout = selectedIndex
                } else if (currentConfigCategory == "APPS") {
                    settings.appsLayout = selectedIndex
                }
                updateScreen2Ui()
            }
        }

        btnCatCardSize.setOnClickListener { view ->
            val options = listOf("NHỎ", "VỪA", "LỚN")
            showDropdownMenu(view, options) { selectedIndex ->
                if (currentConfigCategory == "HDMI") {
                    settings.hdmiSize = selectedIndex
                    // Reset columns when size changes
                    settings.hdmiColumns = 0
                } else if (currentConfigCategory == "APPS") {
                    settings.appsSize = selectedIndex
                    // Reset columns when size changes
                    settings.appsColumns = 0
                }
                updateScreen2Ui()
            }
        }

        btnCatColumns.setOnClickListener { view ->
            val maxColumns = getMaxColumnsForCurrentSize()
            val options = mutableListOf("TỰ ĐỘNG")
            for (i in 1..maxColumns) {
                options.add(i.toString())
            }
            showDropdownMenu(view, options) { selectedIndex ->
                if (currentConfigCategory == "HDMI") {
                    settings.hdmiColumns = selectedIndex
                } else if (currentConfigCategory == "APPS") {
                    settings.appsColumns = selectedIndex
                }
                updateScreen2Ui()
            }
        }
    }

    private fun showDropdownMenu(anchor: View, options: List<String>, onItemSelected: (Int) -> Unit) {
        val popup = PopupMenu(this, anchor)
        options.forEachIndexed { index, option ->
            popup.menu.add(0, index, index, option)
        }
        popup.setOnMenuItemClickListener { item ->
            onItemSelected(item.itemId)
            true
        }
        popup.show()
    }

    private fun updateFocusAnimationBtnText() {
        val styleText = when (settings.focusAnimationStyle) {
            1 -> "NHỊP TIM ĐẬP"
            2 -> "HƠI THỞ"
            else -> "MẶC ĐỊNH"
        }
        btnFocusAnimation.text = "Hiệu ứng Focus: $styleText"
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
