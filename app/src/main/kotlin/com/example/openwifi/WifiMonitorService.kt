package com.example.openwifi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * WiFi 常驻监控服务
 *
 * 以前台 Service 方式运行，确保在 TV 后台不被系统杀死。
 * 双重保障机制：
 *   1. BroadcastReceiver：监听 WiFi 状态变化广播，实时响应
 *   2. 定时轮询（每 30 秒）：防止广播漏发，兜底检查
 *
 * Android 10+ 无法直接调用 setWifiEnabled，此时静默跳过（TV 设备多为 Android 9 以下）。
 */
class WifiMonitorService : Service() {

    companion object {
        private const val TAG = "WifiMonitorService"
        private const val CHANNEL_ID = "wifi_monitor_channel"
        private const val NOTIFICATION_ID = 1001

        /** 定时轮询间隔（毫秒） */
        private const val POLL_INTERVAL_MS = 30_000L

        fun start(context: Context) {
            val intent = Intent(context, WifiMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WifiMonitorService::class.java))
        }
    }

    private var wifiManager: WifiManager? = null
    private val handler = Handler(Looper.getMainLooper())

    /** 定时轮询 Runnable */
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkAndEnableWifi("poll")
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    /** 监听 WiFi 状态变化的广播接收器 */
    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.WIFI_STATE_CHANGED_ACTION) {
                val state = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN
                )
                if (state == WifiManager.WIFI_STATE_DISABLED) {
                    Log.d(TAG, "WiFi 已关闭，尝试重新开启")
                    checkAndEnableWifi("broadcast")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // 注册 WiFi 状态广播
        val filter = IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION)
        registerReceiver(wifiStateReceiver, filter)

        // 启动定时轮询
        handler.post(pollRunnable)

        Log.d(TAG, "WifiMonitorService 已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY：Service 被杀后系统会自动重启
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        runCatching { unregisterReceiver(wifiStateReceiver) }
        Log.d(TAG, "WifiMonitorService 已停止")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── 核心逻辑 ──────────────────────────────────────────────────────────────

    /**
     * 检查 WiFi 状态，如果关闭则尝试开启
     * @param trigger 触发来源，仅用于日志
     */
    @Suppress("DEPRECATION")
    private fun checkAndEnableWifi(trigger: String) {
        val manager = wifiManager ?: return
        val state = manager.wifiState

        if (state == WifiManager.WIFI_STATE_DISABLED) {
            Log.i(TAG, "[$trigger] WiFi 已关闭，正在开启...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 应用无法直接开启 WiFi（系统限制）
                // TV 设备通常为 Android 9 以下，此处仅做日志记录
                Log.w(TAG, "Android 10+ 无法直接开启 WiFi，请手动开启")
            } else {
                val success = manager.setWifiEnabled(true)
                Log.i(TAG, "[$trigger] setWifiEnabled(true) 结果: $success")
                // 更新通知文案
                updateNotification("WiFi 已关闭，正在自动开启...")
            }
        } else {
            Log.d(TAG, "[$trigger] WiFi 状态正常: $state")
            updateNotification("WiFi 监控中，当前状态正常")
        }
    }

    // ── 通知相关 ─────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WiFi 监控",
                NotificationManager.IMPORTANCE_LOW  // LOW 级别：无声音，不打扰用户
            ).apply {
                description = "保持 WiFi 常开的后台服务"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String = "WiFi 监控中，保持网络连接"): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WiFi 自动保持服务")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)       // 常驻，用户无法滑动关闭
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }
}

