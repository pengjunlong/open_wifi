package com.example.openwifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * TV 系统设置助手 MainActivity
 * <p>
 * 功能：
 * 1. 显示当前 WiFi 状态（开/关）及已连接的 WiFi 名称
 * 2. WiFi 关闭时，显示"开启 WiFi"按钮，点击后直接开启 WiFi
 * 3. 点击"WiFi 设置"跳转到系统 WiFi 设置页面
 * 4. 点击"输入法设置"跳转到系统输入法设置页面
 * 5. 点击"更多系统设置"跳转到系统设置主页面
 * <p>
 * 适配 Android 6.0 (API 23)，针对 TV 遥控器焦点导航优化
 */
public class MainActivity extends AppCompatActivity {

    private WifiManager wifiManager;

    private TextView tvWifiEnabled;
    private TextView tvSsid;
    private Button btnEnableWifi;
    private Button btnWifiSettings;
    private Button btnImeSettings;
    private Button btnSystemSettings;

    /**
     * 监听 WiFi 状态变化的广播接收器
     * 当 WiFi 开关切换、连接状态变化时自动刷新 UI
     */
    private final BroadcastReceiver wifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)
                    || WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                updateWifiStatus();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 获取 WifiManager
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // 绑定 UI 控件
        tvWifiEnabled = findViewById(R.id.tv_wifi_enabled);
        tvSsid = findViewById(R.id.tv_ssid);
        btnEnableWifi = findViewById(R.id.btn_enable_wifi);
        btnWifiSettings = findViewById(R.id.btn_wifi_settings);
        btnImeSettings = findViewById(R.id.btn_ime_settings);
        btnSystemSettings = findViewById(R.id.btn_system_settings);

        // 设置按钮点击事件
        setupButtons();

        // 初始化 WiFi 状态
        updateWifiStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 注册 WiFi 状态广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, filter);
        // 每次 Resume 刷新一次状态（从设置页返回时保持最新）
        updateWifiStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 取消注册广播，避免内存泄漏
        try {
            unregisterReceiver(wifiStateReceiver);
        } catch (IllegalArgumentException e) {
            // 防止未注册时调用 unregister 抛出异常
        }
    }

    /**
     * 配置各按钮的点击监听
     */
    private void setupButtons() {
        // 开启 WiFi 按钮
        btnEnableWifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enableWifi();
            }
        });

        // WiFi 设置
        btnWifiSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWifiSettings();
            }
        });

        // 输入法设置
        btnImeSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImeSettings();
            }
        });

        // 系统设置
        btnSystemSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSystemSettings();
            }
        });

        // TV 遥控器：焦点优先落在第一个可见按钮上
        btnWifiSettings.requestFocus();
    }

    /**
     * 刷新 WiFi 状态 UI
     */
    private void updateWifiStatus() {
        if (wifiManager == null) {
            tvWifiEnabled.setText(getString(R.string.status_unknown));
            tvSsid.setText(getString(R.string.status_not_connected));
            return;
        }

        int wifiState = wifiManager.getWifiState();

        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED: {
                // WiFi 已开启
                tvWifiEnabled.setText(getString(R.string.status_on));
                tvWifiEnabled.setTextColor(getResources().getColor(R.color.status_on));

                // 隐藏"开启 WiFi"按钮，更新焦点链
                btnEnableWifi.setVisibility(View.GONE);
                btnWifiSettings.setNextFocusUpId(View.NO_ID);

                // 获取已连接的 SSID
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    String ssid = wifiInfo.getSSID();
                    // Android 返回的 SSID 带有双引号，需去除
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }
                    // <unknown ssid> 表示未连接或没有位置权限
                    if ("<unknown ssid>".equals(ssid) || ssid == null || ssid.isEmpty()) {
                        tvSsid.setText(getString(R.string.status_not_connected));
                        tvSsid.setTextColor(getResources().getColor(R.color.text_secondary));
                    } else {
                        tvSsid.setText(ssid);
                        tvSsid.setTextColor(getResources().getColor(R.color.status_on));
                    }
                } else {
                    tvSsid.setText(getString(R.string.status_not_connected));
                    tvSsid.setTextColor(getResources().getColor(R.color.text_secondary));
                }
                break;
            }

            case WifiManager.WIFI_STATE_ENABLING: {
                // WiFi 开启中
                tvWifiEnabled.setText(getString(R.string.status_connecting));
                tvWifiEnabled.setTextColor(getResources().getColor(R.color.text_secondary));
                tvSsid.setText(getString(R.string.status_not_connected));
                tvSsid.setTextColor(getResources().getColor(R.color.text_secondary));
                btnEnableWifi.setVisibility(View.GONE);
                break;
            }

            case WifiManager.WIFI_STATE_DISABLED:
            case WifiManager.WIFI_STATE_DISABLING:
            case WifiManager.WIFI_STATE_UNKNOWN:
            default: {
                // WiFi 已关闭
                tvWifiEnabled.setText(getString(R.string.status_off));
                tvWifiEnabled.setTextColor(getResources().getColor(R.color.status_off));
                tvSsid.setText(getString(R.string.status_not_connected));
                tvSsid.setTextColor(getResources().getColor(R.color.text_secondary));

                // 显示"开启 WiFi"按钮
                btnEnableWifi.setVisibility(View.VISIBLE);
                // 更新 WiFi 设置按钮的向上焦点指向"开启 WiFi"
                btnWifiSettings.setNextFocusUpId(R.id.btn_enable_wifi);

                // 焦点移到"开启 WiFi"
                btnEnableWifi.requestFocus();
                break;
            }
        }
    }

    /**
     * 开启 WiFi（Android 10 以上 setWifiEnabled 被废弃，引导用户到设置页）
     */
    @SuppressWarnings("deprecation")
    private void enableWifi() {
        if (wifiManager == null) return;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ 无法直接开启 WiFi，引导到 WiFi 设置页
            Toast.makeText(this, "请在设置中手动开启 WiFi", Toast.LENGTH_SHORT).show();
            openWifiSettings();
        } else {
            // Android 9 及以下可直接调用
            boolean result = wifiManager.setWifiEnabled(true);
            if (!result) {
                Toast.makeText(this, "开启 WiFi 失败，请在设置中手动开启", Toast.LENGTH_SHORT).show();
                openWifiSettings();
            }
        }
    }

    /**
     * 跳转到系统 WiFi 设置页
     */
    private void openWifiSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开 WiFi 设置", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 跳转到系统输入法设置页
     */
    private void openImeSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开输入法设置", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 跳转到系统设置主页
     */
    private void openSystemSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 拦截遥控器 DPAD_CENTER / ENTER 键，确保焦点按钮被正确触发
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            View focusedView = getCurrentFocus();
            if (focusedView instanceof Button) {
                focusedView.performClick();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}

