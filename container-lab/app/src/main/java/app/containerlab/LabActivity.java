package app.containerlab;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.View;
import android.widget.*;
import com.tegufy.blackbox.BlackBoxCore;
import com.tegufy.blackbox.entity.pm.InstallResult;
import com.tegufy.blackbox.core.system.pm.IBPackageManagerService;
import com.tegufy.blackbox.core.system.am.IBActivityManagerService;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.*;

public final class LabActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private TextView status, diagnostic;
    private Button importButton, launchButton, copyButton, enableLocationButton, disableLocationButton;
    private EditText latitude, longitude;
    private TextView locationState;
    private boolean busy;
    private boolean collecting;
    private String exitEvidence = "系统退出：点击复制精简诊断时读取";
    @Override public void onCreate(Bundle saved) {
        if (LabApplication.initializationError == null) {
            try { BlackBoxCore.get().onBeforeMainActivityOnCreate(this); }
            catch (Throwable error) { LabLog.error(this, "host before activity", error); }
        }
        super.onCreate(saved);
        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(24); page.setPadding(padding, padding, padding, padding);
        scroll.addView(page); setContentView(scroll);
        addText(page, "容器启动测试", 26);
        addText(page, "0.2.4 · 容器微信定位", 16);
        addText(page, "经纬度仅用于容器内微信。启用或关闭会重启这个微信实例，保留登录和数据。", 16);
        status = addText(page, LabApplication.initializationError == null ? "引擎调用已完成，尚未验证微信启动。" : LabApplication.initializationError, 18);
        locationState = addText(page, ContainerLocationSettings.summary(this), 17);
        latitude = coordinateInput(page,"纬度（WGS84，-90 到 90）",ContainerLocationSettings.latitude(this));
        longitude = coordinateInput(page,"经度（WGS84，-180 到 180）",ContainerLocationSettings.longitude(this));
        enableLocationButton = addButton(page,"启用并重启容器微信",v -> changeLocation(true));
        disableLocationButton = addButton(page,"关闭并重启容器微信",v -> changeLocation(false));
        importButton = addButton(page, "导入本机已安装的微信", v -> importWechat());
        launchButton = addButton(page, "请求启动容器内微信", v -> launchWechat());
        addButton(page, "刷新诊断", v -> collectDiagnostics(false));
        copyButton = addButton(page, "复制精简诊断", v -> collectDiagnostics(true));
        diagnostic = addText(page, "", 13); diagnostic.setTextIsSelectable(true);
        if (LabApplication.initializationError == null) {
            try { BlackBoxCore.get().onAfterMainActivityOnCreate(this); }
            catch (Throwable error) { LabLog.error(this, "host after activity", error); }
        }
        refresh();
        maybeRunWebProbe(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);maybeRunWebProbe(intent);
    }
    private void maybeRunWebProbe(Intent intent) {
        if (intent == null || !intent.getBooleanExtra(GuestWebProbe.HOST_REQUEST, false)) return;
        intent.removeExtra(GuestWebProbe.HOST_REQUEST);
        new Handler(getMainLooper()).post(() -> {
            try { GuestWebProbe.launch(this); }
            catch (Exception error) { LabLog.error(this, "网页自检启动", error); }
        });
    }
    private void importWechat() {
        if (busy || LabApplication.initializationError != null) return;
        busy(true); status.setText("正在检查微信并导入，请等待…");
        worker.execute(() -> {
            String message;
            try {
                PackageInfo info = getPackageManager().getPackageInfo(ImportPolicy.WECHAT, 0);
                Set<String> abis = new LinkedHashSet<>();
                List<String> apks = new ArrayList<>(); apks.add(info.applicationInfo.sourceDir);
                if (info.applicationInfo.splitSourceDirs != null) Collections.addAll(apks, info.applicationInfo.splitSourceDirs);
                for (String apk : apks) {
                    try (ZipFile zip = new ZipFile(apk)) {
                        Enumeration<? extends ZipEntry> entries = zip.entries();
                        while (entries.hasMoreElements()) {
                            String[] parts = entries.nextElement().getName().split("/");
                            if (parts.length >= 3 && "lib".equals(parts[0])) abis.add(parts[1]);
                        }
                    }
                }
                LabLog.append(this, "本机微信版本=" + info.versionName + "，APK数量=" + apks.size() + "，native ABI=" + abis);
                String error = ImportPolicy.architectureError(Build.SUPPORTED_ABIS, abis.toArray(new String[0]));
                if (error != null) throw new IllegalStateException(error);
                InstallResult result = BlackBoxCore.get().installPackageAsUser(ImportPolicy.WECHAT, 0);
                boolean installed = containerInstalled();
                boolean confirmed = result != null && ImportPolicy.importConfirmed(result.success, result.packageName, installed);
                if (!confirmed) throw new IllegalStateException("导入未确认：result=" + (result == null ? "null" : result.success + "/" + result.packageName + "/" + result.msg) + "，installed=" + installed);
                message = "容器安装记录已确认。下一步点击请求启动，并观察实际页面。";
                LabLog.append(this, message);
            } catch (Throwable error) {
                message = "导入失败：" + error.getMessage(); LabLog.error(this, "import", error);
            }
            final String finalMessage = message;
            runOnUiThread(() -> { if (!isDestroyed()) { status.setText(finalMessage); busy(false); refresh(); } });
        });
    }
    private void launchWechat() {
        if (busy || LabApplication.initializationError != null) return;
        try {
            if (!containerInstalled()) throw new IllegalStateException("请先完成导入。");
            IBPackageManagerService pm = BlackBoxCore.getBPackageManager().getService();
            IBActivityManagerService am = BlackBoxCore.getBActivityManager().getService();
            if (pm == null || am == null) throw new IllegalStateException("容器服务不可用。");
            Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(ImportPolicy.WECHAT);
            List<ResolveInfo> matches = pm.queryIntentActivities(query, 0, null, 0);
            if (matches == null || matches.isEmpty() || matches.get(0).activityInfo == null
                    || !ImportPolicy.WECHAT.equals(matches.get(0).activityInfo.packageName)) {
                throw new IllegalStateException("容器内没有可确认的微信启动入口。");
            }
            ActivityInfo info = matches.get(0).activityInfo;
            Intent launch = new Intent(query).setComponent(new ComponentName(info.packageName, info.name)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            am.startActivity(launch, 0);
            String message = "容器启动请求已返回，请观察实际页面；这不是启动成功的确认。";
            status.setText(message); LabLog.append(this, message); refresh();
        } catch (Throwable error) {
            status.setText("启动失败：" + error.getMessage()); LabLog.error(this, "launch", error); refresh();
        }
    }
    private void busy(boolean value) {
        busy = value; importButton.setEnabled(!value); launchButton.setEnabled(!value);
        enableLocationButton.setEnabled(!value); disableLocationButton.setEnabled(!value);
        latitude.setEnabled(!value); longitude.setEnabled(!value);
    }
    private EditText coordinateInput(LinearLayout page,String hint,String value){
        addText(page,hint,14);EditText field=new EditText(this);field.setSingleLine(true);
        field.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        field.setText(value);field.setTextSize(18);page.addView(field,new LinearLayout.LayoutParams(-1,-2));return field;
    }
    private void changeLocation(boolean enabled){
        if(busy || LabApplication.initializationError!=null)return;
        final double[] point;
        try{point=enabled?com.tegufy.blackbox.utils.compat.ScopedLocationPolicy.parse(latitude.getText().toString(),longitude.getText().toString()):null;}
        catch(IllegalArgumentException error){status.setText("坐标无效：请输入有效经纬度，纬度-90到90，经度-180到180。");return;}
        if(enabled && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},73);return;
        }
        busy(true);status.setText("正在保存微信专用定位并重启容器实例…");
        worker.execute(()->{
            try{
                if(!containerInstalled())throw new IllegalStateException("请先导入微信");
                ContainerLocationSettings.apply(this,point,enabled);
                // Core-only stop: never asks Android to stop the separately installed original WeChat.
                BlackBoxCore.getBPackageManager().getService().stopPackage(ImportPolicy.WECHAT,0);
                LabLog.append(this,ContainerLocationSettings.summary(this));
                runOnUiThread(()->{if(!isDestroyed()){busy(false);locationState.setText(ContainerLocationSettings.summary(this));launchWechat();}});
            }catch(Exception error){LabLog.error(this,"修改容器定位",error);runOnUiThread(()->{if(!isDestroyed()){busy(false);status.setText("设置失败："+error.getMessage());locationState.setText(ContainerLocationSettings.summary(this));refresh();}});}
        });
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==73){if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED)changeLocation(true);
            else status.setText("请允许精确位置权限后再启用容器定位。");}
    }
    private boolean containerInstalled() throws RemoteException {
        IBPackageManagerService service = BlackBoxCore.getBPackageManager().getService();
        if (service == null) throw new IllegalStateException("容器包管理服务不可用，不使用真实微信状态作为替代。");
        return service.isInstalled(ImportPolicy.WECHAT, 0);
    }
    private void refresh() { diagnostic.setText(LabLog.report(this, exitEvidence)); }
    private void collectDiagnostics(boolean copy) {
        if (collecting) return;
        collecting = true;
        copyButton.setEnabled(false);
        copyButton.setText("正在读取退出记录…");
        worker.execute(() -> {
            String evidence = SystemExitDiagnostics.collect(getApplicationContext());
            String report;
            try { report = LabLog.report(this, evidence); }
            catch (Exception error) { report = DiagnosticSummary.summarize("容器诊断 0.2.4", "诊断读取失败：" + error.getClass().getSimpleName(), evidence); }
            final String result = report;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                collecting = false;
                exitEvidence = evidence;
                copyButton.setEnabled(true);
                copyButton.setText("复制精简诊断");
                diagnostic.setText(result);
                if (copy) {
                    getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("容器启动诊断", result));
                    new AlertDialog.Builder(this).setMessage("已复制精简诊断（最多1600字符），包含可读取的系统退出原因。历史记录请核对时间。不会自动上传。").setPositiveButton("知道了", null).show();
                }
            });
        });
    }
    private TextView addText(LinearLayout page, String value, int sp) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(sp); text.setPadding(0, dp(8), 0, dp(8)); page.addView(text); return text;
    }
    private Button addButton(LinearLayout page, String value, View.OnClickListener listener) {
        Button button = new Button(this); button.setText(value); button.setMinHeight(dp(52)); button.setOnClickListener(listener); page.addView(button, new LinearLayout.LayoutParams(-1, -2)); return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    @Override protected void onResume() { super.onResume(); if (diagnostic != null) refresh(); }
    @Override protected void onDestroy() { worker.shutdown(); super.onDestroy(); }
}
