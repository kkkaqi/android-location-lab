package app.fixedlocation;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

public final class MainActivity extends Activity {
    private EditText latitude, longitude;
    private TextView status, detail;
    private Button start, stop;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long waitingSince;
    private String localError;
    private final Runnable refresh = new Runnable() {
        public void run() { render(); handler.postDelayed(this, 700); }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(24), dp(24), dp(24), dp(32));
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();
        add(page, text("定点", 30, true), 0);
        add(page, text("1.0.1 诊断版 · 设置一个位置，随时开始或停止。", 16, false), 8);
        status = text("尚未开始", 20, true);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        add(page, status, 28);
        detail = text("", 14, false);
        detail.setTextIsSelectable(true);
        add(page, detail, 8);

        add(page, text("目标位置", 20, true), 28);
        add(page, text("使用 GPS 坐标（WGS84）。纬度在前，经度在后。", 14, false), 8);
        latitude = field(page, "纬度", "例如 39.908700", 18);
        longitude = field(page, "经度", "例如 116.397500", 14);
        android.content.SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        latitude.setText(prefs.getString("latitude", ""));
        longitude.setText(prefs.getString("longitude", ""));
        latitude.setSaveEnabled(true); longitude.setSaveEnabled(true);
        latitude.setId(101); longitude.setId(102);

        start = button("开始模拟定位", v -> begin());
        add(page, start, 20);
        stop = button("停止模拟定位", v -> {
            waitingSince = 0; localError = null;
            stopService(new Intent(this, MockLocationService.class));
            render();
        });
        add(page, stop, 8);
        add(page, button("复制运行诊断", v -> {
            String current = MockLocationService.state.message;
            if (MockLocationService.state.running) current += "\n距最近更新：" + Math.max(0, SystemClock.elapsedRealtime() - MockLocationService.state.lastUpdate) / 1000d + "秒";
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("定点运行诊断", RunDiagnostics.report(this, current)));
            new AlertDialog.Builder(this).setMessage("运行诊断已复制。包括设备型号、系统版本、目标坐标和系统来源状态，不包含微信内容。可粘贴到聊天中排查；不会自动上传。").setPositiveButton("知道了", null).show();
        }), 8);
        add(page, text("首次使用", 20, true), 28);
        add(page, text("1. 开启手机的开发者选项。通常在“关于手机”中连续点击版本号。\n2. 在开发者选项中找到“选择模拟位置信息应用”，选择“定点”。\n3. 开启系统定位，返回这里填写坐标，点击开始。", 15, false), 8);
        add(page, button("打开开发者选项", v -> openSettings(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))), 12);
        add(page, button("打开系统定位设置", v -> openSettings(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))), 8);
        add(page, button("打开应用权限设置", v -> openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())))), 8);
        add(page, button("打开电池优化设置", v -> openSettings(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))), 8);
        add(page, text("切到微信后请保留本应用后台运行；从最近任务中划掉本应用会停止模拟定位。若手机省电策略限制运行，可在应用设置中允许后台运行。", 14, false), 18);
        add(page, text("兼容性说明", 20, true), 24);
        add(page, text("运行状态表示本应用正在发送模拟位置；缓存回读用于检查系统来源，不代表微信或百度已经采纳。地图与小程序可能另用网络定位，或过滤模拟位置。\n\n发生回跳后，请回到这里点击“复制运行诊断”，保留各来源状态、更新轮数和最长间隔。若均为新鲜模拟位置，目标应用仍显示实际位置，则需进一步检查目标应用的定位途径，不能只靠增加后台权限解决。\n\n腾讯、高德等地图常用的 GCJ-02 坐标不能直接当成 WGS84 输入，否则会有偏移。本版本不含地图搜索和坐标转换。\n\n停止后，其他应用可能暂时保留旧位置。请等待重新定位；如仍异常，可在开发者选项取消模拟应用或重启手机。", 14, false), 8);
    }

    private void begin() {
        localError = null;
        latitude.setError(null); longitude.setError(null);
        try { Coordinates.parse(latitude.getText().toString(), longitude.getText().toString()); }
        catch (IllegalArgumentException error) {
            localError = error.getMessage();
            if (localError.contains("纬度")) latitude.setError(localError); else longitude.setError(localError);
            render(); return;
        }
        if (!getSystemService(LocationManager.class).isLocationEnabled()) {
            localError = "请先打开系统定位，下面有定位设置入口。";
            render(); return;
        }
        AppOpsManager ops = getSystemService(AppOpsManager.class);
        if (ops.checkOpNoThrow("android:mock_location", android.os.Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED) {
            localError = "请在开发者选项的“选择模拟位置信息应用”中选择“定点”。";
            render(); return;
        }
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 20);
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                && !getPreferences(MODE_PRIVATE).getBoolean("askedNotifications", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("askedNotifications", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 21);
            return;
        }
        launch();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void launch() {
        String lat = latitude.getText().toString().trim();
        String lon = longitude.getText().toString().trim();
        getPreferences(MODE_PRIVATE).edit().putString("latitude", lat).putString("longitude", lon).apply();
        try {
            waitingSince = SystemClock.elapsedRealtime();
            startForegroundService(new Intent(this, MockLocationService.class).putExtra("latitude", lat).putExtra("longitude", lon));
            View focus = getCurrentFocus();
            if (focus != null) getSystemService(InputMethodManager.class).hideSoftInputFromWindow(focus.getWindowToken(), 0);
        } catch (RuntimeException error) {
            waitingSince = 0;
            localError = "无法启动定位服务，请检查定位权限后重试。";
        }
        render();
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 20) {
            if (hasLocationPermission()) begin();
            else { localError = "需要定位权限才能运行前台定位服务。可在应用权限设置中开启。"; render(); }
        } else if (request == 21) begin();
    }

    private void render() {
        MockLocationService.State state = MockLocationService.state;
        if (state.running || (waitingSince != 0 && SystemClock.elapsedRealtime() - waitingSince > 1800)) waitingSince = 0;
        boolean waiting = waitingSince != 0;
        long age = Math.max(0, SystemClock.elapsedRealtime() - state.lastUpdate);
        status.setText(state.running ? !UpdateHealth.isFresh(state.lastUpdate, SystemClock.elapsedRealtime()) ? "更新已延迟，需要检查" : "正在发送模拟位置" : waiting ? "正在启动…" : localError != null ? "需要处理" : "模拟定位未运行");
        String message = state.message;
        if (state.running) message += String.format(java.util.Locale.CHINA, "\n距最近更新：%.1f秒", age / 1000d);
        else if (state.lastUpdate == 0 && state.message.startsWith("尚未开始")) message += "\n\n上次保存的诊断（不是当前运行状态）：\n" + RunDiagnostics.previous(this);
        detail.setText(localError != null ? localError : waiting ? "正在检查系统授权并注入位置。" : message);
        start.setEnabled(!state.running && !waiting);
        stop.setEnabled(state.running || waiting);
        latitude.setEnabled(!state.running && !waiting);
        longitude.setEnabled(!state.running && !waiting);
    }

    private void openSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException e) { localError = "无法直接打开此设置，请到手机设置中手动查找。"; render(); }
    }

    private TextView text(String value, int size, boolean bold) {
        TextView result = new TextView(this);
        result.setText(value); result.setTextSize(size);
        result.setTextColor(getColor(R.color.ink));
        result.setLineSpacing(dp(3), 1f);
        if (bold) result.setTypeface(null, Typeface.BOLD);
        return result;
    }

    private EditText field(LinearLayout page, String label, String hint, int margin) {
        TextView name = text(label, 15, true);
        add(page, name, margin);
        EditText input = new EditText(this);
        input.setTextSize(18); input.setSingleLine(true); input.setMinHeight(dp(56));
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setHint(hint); input.setSelectAllOnFocus(true);
        input.setHintTextColor(getColor(R.color.secondary_ink));
        add(page, input, 4);
        // IDs assigned to fields in onCreate are stable for state restoration.
        name.setTag(input);
        name.post(() -> name.setLabelFor(input.getId()));
        return input;
    }

    private Button button(String title, View.OnClickListener listener) {
        Button result = new Button(this);
        result.setText(title); result.setAllCaps(false); result.setTextSize(16);
        result.setMinHeight(dp(52)); result.setOnClickListener(listener);
        return result;
    }

    private void add(LinearLayout page, View view, int margin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(margin); page.addView(view, params);
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
}
