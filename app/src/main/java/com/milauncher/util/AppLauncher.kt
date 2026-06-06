package com.milauncher.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.milauncher.R

object AppLauncher {
    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.getString(R.string.msg_app_not_installed), Toast.LENGTH_SHORT).show()
        }
    }

    fun launchSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun launchHdmi(context: Context, hdmiPort: Int) {
        // Xiaomi specific intent might be different. 
        // We show a toast for now to represent the action.
        Toast.makeText(context, "Chuyển sang HDMI $hdmiPort", Toast.LENGTH_SHORT).show()
    }
}
