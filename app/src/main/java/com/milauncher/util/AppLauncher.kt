package com.milauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import com.milauncher.R

object AppLauncher {

    fun launchApp(context: Context, packageName: String) {
        var intent = context.packageManager.getLaunchIntentForPackage(packageName)
        
        // If standard launcher intent is not found, try Leanback launcher (for pure TV apps)
        if (intent == null) {
            val leanbackIntent = Intent(Intent.ACTION_MAIN)
            leanbackIntent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            leanbackIntent.setPackage(packageName)
            val activities = context.packageManager.queryIntentActivities(leanbackIntent, 0)
            if (activities.isNotEmpty()) {
                val activityInfo = activities[0].activityInfo
                intent = Intent(Intent.ACTION_MAIN)
                intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                intent.setClassName(activityInfo.packageName, activityInfo.name)
            }
        }

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Mở Xiaomi TV Settings (com.xiaomi.mitv.settings) — đúng component.
     * Fallback về Android Settings nếu không tìm thấy.
     */
    fun launchSettings(context: Context) {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    "com.xiaomi.mitv.settings",
                    "com.xiaomi.mitv.settings.entry.MainActivity"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: standard Android Settings
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Không mở được Cài đặt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Chuyển nguồn HDMI trên TV Xiaomi nội địa.
     *
     * Dùng cùng chiến lược fallback như Projectivy Launcher v4.66:
     *   1. com.xiaomi.mitv.tvplayer.EXTSRC_PLAY  (Xiaomi TV Player - chính)
     *   2. android.intent.action.VIEW + TvContract passthrough URI (AOSP TV Input)
     *   3. com.xiaomi.mitv.settings.INPUTSOURCE_POPUP broadcast (popup chọn nguồn)
     *   4. Toast thông báo nếu tất cả đều thất bại
     *
     * Input ID mappings (từ DroidLogic / Xiaomi):
     *   HDMI1 → "Hdmi1InputService/HW5"
     *   HDMI2 → "Hdmi2InputService/HW6"
     *   HDMI3 → "Hdmi3InputService/HW7"
     *   AV    → "AV1InputService/HW1"
     */
    fun launchHdmi(context: Context, hdmiPort: Int) {
        val inputName = when (hdmiPort) {
            1 -> "HDMI1"
            2 -> "HDMI2"
            3 -> "HDMI3"
            4 -> "HDMI4"
            else -> "HDMI1"
        }
        val passthroughId = when (hdmiPort) {
            1 -> "com.droidlogic.tvinput/.services.Hdmi1InputService/HW5"
            2 -> "com.droidlogic.tvinput/.services.Hdmi2InputService/HW6"
            3 -> "com.droidlogic.tvinput/.services.Hdmi3InputService/HW7"
            4 -> "com.droidlogic.tvinput/.services.Hdmi4InputService/HW8"
            else -> "com.droidlogic.tvinput/.services.Hdmi1InputService/HW5"
        }

        // --- Cách 1: Xiaomi TV Player intent (chính, dùng trong Projectivy) ---
        if (tryXiaomiTvPlayerIntent(context, inputName)) return

        // --- Cách 2: Android TV TvContract passthrough URI ---
        if (tryTvContractIntent(context, passthroughId)) return

        // --- Cách 3: Popup chọn nguồn input của Xiaomi ---
        if (tryXiaomiInputPopup(context)) return

        // --- Fallback cuối: thông báo ---
        Toast.makeText(context, "Chuyển sang $inputName", Toast.LENGTH_SHORT).show()
    }

    private fun tryXiaomiTvPlayerIntent(context: Context, inputName: String): Boolean {
        return try {
            val intent = Intent("com.xiaomi.mitv.tvplayer.EXTSRC_PLAY").apply {
                putExtra("input", inputName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tryTvContractIntent(context: Context, passthroughId: String): Boolean {
        return try {
            val uri: Uri = TvContract.buildChannelUriForPassthroughInput(passthroughId)
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun tryXiaomiInputPopup(context: Context): Boolean {
        return try {
            // Thử broadcast trước (cách Projectivy dùng)
            context.sendBroadcast(Intent("com.xiaomi.mitv.settings.INPUTSOURCE_POPUP"))
            true
        } catch (e: Exception) {
            try {
                // Fallback: startActivity
                val intent = Intent("com.xiaomi.mitv.settings.INPUTSOURCE_POPUP").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    /**
     * Mở App Store thay thế (MStore hoặc Aptoide TV).
     * Không cần Google Play.
     */
    fun launchAppStore(context: Context) {
        // Thử MStore trước (từ bộ kit MI3S_HANOI)
        val mstoreIntent = context.packageManager.getLaunchIntentForPackage("tungbui.mstore")
        if (mstoreIntent != null) {
            mstoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(mstoreIntent)
            return
        }
        // Fallback: Aptoide TV
        val aptoideIntent = context.packageManager.getLaunchIntentForPackage("cm.aptoidetv.pt")
        if (aptoideIntent != null) {
            aptoideIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(aptoideIntent)
            return
        }
        // Fallback: ADB downloader
        val dlIntent = context.packageManager.getLaunchIntentForPackage("com.dl.hakison")
        if (dlIntent != null) {
            dlIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(dlIntent)
            return
        }
        Toast.makeText(context, "Chưa cài App Store. Hãy cài MStore hoặc Aptoide TV.", Toast.LENGTH_LONG).show()
    }
}
