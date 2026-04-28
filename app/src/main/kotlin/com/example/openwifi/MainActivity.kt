package com.example.openwifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * TV 系统设置助手 MainActivity
 *
 * 功能：
 * 1. 显示当前 WiFi 状态（开/关）及已连接的 WiFi 名称
 * 2. WiFi 关闭时，显示"开启 WiFi"按钮，点击后直接开启 WiFi
 * 3. 点击"WiFi 设置"跳转到系统 WiFi 设置页面
 * 4. 点击"输入法设置"跳转到系统输入法设置页面
 * 5. 点击"更多系统设置"跳转到系统设置主页面
 *
 * 适配 Android 6.0 (API 23)，针对 TV 遥控器焦点导航优化
 */
class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null

    private lateinit var tvWifiEnabled: TextView
    private lateinit var tvSsid: TextView
    private lateinit var btnEnableWifi: Button
    private lateinit var btnWifiSettings: Button
    private lateinit var btnImeSettings: Button
    private lateinit var btnSystemSettings: Button

    /**
     * 监听 WiFi 状态变化的广播接收器
     * 当 WiFi 开关切换、连接状态变化时自动刷新 UI
     */
    private val wifiStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == WifiManager.WIFI_STATE_CHANGED_ACTION ||
                action == WifiManager.NETWORK_STATE_CHANGED_ACTION
            ) {
                updateWifiStatus()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 获取 WifiManager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        // 绑定 UI 控件
        tvWifiEnabled = findViewById(R.id.tv_wifi_enabled)
        tvSsid = findViewById(R.id.tv_ssid)
        btnEnableWifi = findViewById(R.id.btn_enable_wifi)
        btnWifiSettings = findViewById(R.id.btn_wifi_settings)
        btnImeSettings = findViewById(R.id.btn_ime_settings)
        btnSystemSettings = findViewById(R.id.btn_system_settings)

        // 设置按钮点击事件
        setupButtons()

        // 初始化 WiFi 状态
        updateWifiStatus()
    }

    override fun onResume() {
        super.onResume()
        // 注册 WiFi 状态广播
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        registerReceiver(wifiStateReceiver, filter)
        // 每次 Resume 刷新一次状态（从设置页返回时保持最新）
        updateWifiStatus()
    }

    override fun onPause() {
        super.onPause()
        // 取消注册广播，避免内存泄漏
        runCatching { unregisterReceiver(wifiStateReceiver) }
    }

    /**
     * 配置各按钮的点击监听
     */
    private fun setupButtons() {
        btnEnableWifi.setOnClickListener { enableWifi() }
        btnWifiSettings.setOnClickListener { openSettings(Settings.ACTION_WIFI_SETTINGS, "无法打开 WiFi 设置") }
        btnImeSettings.setOnClickListener { openSettings(Settings.ACTION_INPUT_METHOD_SETTINGS, "无法打开输入法设置") }
        btnSystemSettings.setOnClickListener { openSettings(Settings.ACTION_SETTINGS, "无法打开系统设置") }

        // TV 遥控器：焦点优先落在第一个可见按钮上
        btnWifiSettings.requestFocus()
    }

    /**
     * 刷新 WiFi 状态 UI
     */
    private fun updateWifiStatus() {
        val manager = wifiManager ?: run {
            tvWifiEnabled.setText(R.string.status_unknown)
            tvSsid.setText(R.string.status_not_connected)
            return
        }

        when (manager.wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> {
                // WiFi 已开启
                tvWifiEnabled.setText(R.string.status_on)
                tvWifiEnabled.setTextColor(getColor(R.color.status_on))

                // 隐藏"开启 WiFi"按钮，清除焦点链
                btnEnableWifi.visibility = View.GONE
                btnWifiSettings.nextFocusUpId = View.NO_ID

                // 获取已连接的 SSID
                @Suppress("DEPRECATION")
                val rawSsid = manager.connectionInfo?.ssid
                val ssid = rawSsid
                    ?.removeSurrounding("\"")
                    ?.takeIf { it.isNotEmpty() && it != "<unknown ssid>" }

                if (ssid != null) {
                    tvSsid.text = ssid
                    tvSsid.setTextColor(getColor(R.color.status_on))
                } else {
                    tvSsid.setText(R.string.status_not_connected)
                    tvSsid.setTextColor(getColor(R.color.text_secondary))
                }
            }

            WifiManager.WIFI_STATE_ENABLING -> {
                // WiFi 开启中
                tvWifiEnabled.setText(R.string.status_connecting)
                tvWifiEnabled.setTextColor(getColor(R.color.text_secondary))
                tvSsid.setText(R.string.status_not_connected)
                tvSsid.setTextColor(getColor(R.color.text_secondary))
                btnEnableWifi.visibility = View.GONE
            }

            else -> {
                // WiFi 已关闭（DISABLED / DISABLING / UNKNOWN）
                tvWifiEnabled.setText(R.string.status_off)
                tvWifiEnabled.setTextColor(getColor(R.color.status_off))
                tvSsid.setText(R.string.status_not_connected)
                tvSsid.setTextColor(getColor(R.color.text_secondary))

                // 显示"开启 WiFi"按钮，更新焦点链
                btnEnableWifi.visibility = View.VISIBLE
                btnWifiSettings.nextFocusUpId = R.id.btn_enable_wifi
                btnEnableWifi.requestFocus()
            }
        }
    }

    /**
     * 开启 WiFi
     * Android 10+ setWifiEnabled 已废弃，引导用户到 WiFi 设置页
     */
    @Suppress("DEPRECATION")
    private fun enableWifi() {
        val manager = wifiManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 无法直接开启 WiFi
            Toast.makeText(this, "请在设置中手动开启 WiFi", Toast.LENGTH_SHORT).show()
            openSettings(Settings.ACTION_WIFI_SETTINGS, "无法打开 WiFi 设置")
        } else {
            val success = manager.setWifiEnabled(true)
            if (!success) {
                Toast.makeText(this, "开启 WiFi 失败，请在设置中手动开启", Toast.LENGTH_SHORT).show()
                openSettings(Settings.ACTION_WIFI_SETTINGS, "无法打开 WiFi 设置")
            }
        }
    }

    /**
     * 通用跳转系统设置页
     */
    private fun openSettings(action: String, errorMsg: String) {
        runCatching {
            startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 拦截遥控器 DPAD_CENTER / ENTER 键，确保焦点按钮被正确触发
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            val focused = currentFocus
            if (focused is Button) {
                focused.performClick()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

