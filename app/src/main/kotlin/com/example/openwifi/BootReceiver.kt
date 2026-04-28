package com.example.openwifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 开机自启广播接收器
 *
 * 系统开机完成后自动启动 WifiMonitorService，
 * 无需用户手动打开 App 即可让监控服务在后台运行。
 *
 * 需要在 AndroidManifest.xml 中声明权限：
 *   android.permission.RECEIVE_BOOT_COMPLETED
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "开机完成，启动 WifiMonitorService")
            WifiMonitorService.start(context)
        }
    }
}

