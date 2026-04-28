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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * TV 系统设置助手 MainActivity
 *
 * 1. 显示当前 WiFi 状态（开/关）及已连接的 WiFi 名称
 * 2. WiFi 切换按钮：一键开启/关闭 WiFi（Android 10+ 跳转设置页）
 * 3. 显示当前系统配置的输入法名称
 * 4. 点击"WiFi 设置"跳转到系统 WiFi 设置页面
 * 5. 点击"输入法设置"跳转到系统输入法设置页面
 * 6. 点击"更多系统设置"跳转到系统设置主页面
 *
 * 适配 Android 6.0 (API 23)，针对 TV 遥控器焦点导航优化
 */
class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null

    private lateinit var tvWifiEnabled: TextView
    private lateinit var tvSsid: TextView
    private lateinit var tvIme: TextView
    private lateinit var btnToggleWifi: Button
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
        tvIme = findViewById(R.id.tv_ime)
        btnToggleWifi = findViewById(R.id.btn_toggle_wifi)
        btnWifiSettings = findViewById(R.id.btn_wifi_settings)
        btnImeSettings = findViewById(R.id.btn_ime_settings)
        btnSystemSettings = findViewById(R.id.btn_system_settings)

        // 设置按钮点击事件
        setupButtons()

        // 初始化 WiFi 状态
        updateWifiStatus()

        // 初始化输入法名称
        updateImeStatus()
    }

    override fun onResume() {
        super.onResume()
        // 注册 WiFi 状态广播
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        registerReceiver(wifiStateReceiver, filter)
        // 每次 Resume 刷新状态（从设置页返回时保持最新）
        updateWifiStatus()
        // 从输入法设置页返回后刷新输入法名称
        updateImeStatus()
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
        btnToggleWifi.setOnClickListener { toggleWifi() }
        btnWifiSettings.setOnClickListener { openSettings(Settings.ACTION_WIFI_SETTINGS, "无法打开 WiFi 设置") }
        btnImeSettings.setOnClickListener { openSettings(Settings.ACTION_INPUT_METHOD_SETTINGS, "无法打开输入法设置") }
        btnSystemSettings.setOnClickListener { openSettings(Settings.ACTION_SETTINGS, "无法打开系统设置") }

        // TV 遥控器：焦点优先落在 WiFi 切换按钮上
        btnToggleWifi.requestFocus()
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

                // 切换按钮文字为"关闭 WiFi"
                btnToggleWifi.setText(R.string.btn_toggle_wifi_on)

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
                btnToggleWifi.setText(R.string.btn_toggle_wifi_off)
            }

            WifiManager.WIFI_STATE_DISABLING -> {
                // WiFi 关闭中
                tvWifiEnabled.setText(R.string.status_connecting)
                tvWifiEnabled.setTextColor(getColor(R.color.text_secondary))
                tvSsid.setText(R.string.status_not_connected)
                tvSsid.setTextColor(getColor(R.color.text_secondary))
                btnToggleWifi.setText(R.string.btn_toggle_wifi_on)
            }

            else -> {
                // WiFi 已关闭（DISABLED / UNKNOWN）
                tvWifiEnabled.setText(R.string.status_off)
                tvWifiEnabled.setTextColor(getColor(R.color.status_off))
                tvSsid.setText(R.string.status_not_connected)
                tvSsid.setTextColor(getColor(R.color.text_secondary))

                // 切换按钮文字为"开启 WiFi"
                btnToggleWifi.setText(R.string.btn_toggle_wifi_off)
            }
        }
    }

    /**
     * 读取并显示当前系统配置的输入法名称
     * 通过 Settings.Secure.DEFAULT_INPUT_METHOD 获取当前输入法包名/组件名
     * 再从 InputMethodManager 中查找对应的应用标签名
     */
    private fun updateImeStatus() {
        val imeName = getCurrentImeName()
        tvIme.text = imeName ?: getString(R.string.status_unknown)
        tvIme.setTextColor(
            if (imeName != null) getColor(R.color.text_secondary)
            else getColor(R.color.status_off)
        )
    }

    /**
     * 获取当前默认输入法的友好名称
     * 返回 null 表示无法获取
     */
    private fun getCurrentImeName(): String? {
        return runCatching {
            // 读取当前默认输入法的组件名，格式：com.xxx.ime/.ServiceClass
            val defaultIme = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            ) ?: return null

            // 通过 InputMethodManager 查找对应 InputMethodInfo
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
                    as? android.view.inputmethod.InputMethodManager
                ?: return defaultIme.substringBefore("/")

            val imeInfo = imm.enabledInputMethodList
                .firstOrNull { it.id == defaultIme }
                ?: imm.inputMethodList
                    .firstOrNull { it.id == defaultIme }

            // 优先返回应用标签，降级返回包名
            imeInfo?.loadLabel(packageManager)?.toString()
                ?: defaultIme.substringBefore("/")
        }.getOrNull()
    }

    /**
     * 切换 WiFi 开关
     * Android 10+ setWifiEnabled 已废弃，引导用户到 WiFi 设置页
     */
    @Suppress("DEPRECATION")
    private fun toggleWifi() {
        val manager = wifiManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 无法直接操作 WiFi，跳转设置页
            Toast.makeText(this, "请在设置中手动切换 WiFi", Toast.LENGTH_SHORT).show()
            openSettings(Settings.ACTION_WIFI_SETTINGS, "无法打开 WiFi 设置")
            return
        }

        val isEnabled = manager.wifiState == WifiManager.WIFI_STATE_ENABLED ||
                manager.wifiState == WifiManager.WIFI_STATE_ENABLING

        val success = manager.setWifiEnabled(!isEnabled)
        if (!success) {
            Toast.makeText(this, "操作失败，请在设置中手动切换", Toast.LENGTH_SHORT).show()
            openSettings(Settings.ACTION_WIFI_SETTINGS, "无法打开 WiFi 设置")
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

