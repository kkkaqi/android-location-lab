package app.fixedlocation;

import android.location.Location;
import android.app.AppOpsManager;
import android.content.Context;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.SystemClock;

final class AndroidBackend implements MockSession.Backend {
    private final LocationManager manager;
    private final Context context;

    AndroidBackend(Context context, LocationManager manager) { this.context = context; this.manager = manager; }

    public void ensureAuthorized() {
        AppOpsManager ops = context.getSystemService(AppOpsManager.class);
        if (ops.checkOpNoThrow("android:mock_location", android.os.Process.myUid(), context.getPackageName()) != AppOpsManager.MODE_ALLOWED) {
            throw new SecurityException("模拟定位授权已取消；无法确认模拟来源已清理");
        }
    }

    public void install(String provider) {
        ensureAuthorized();
        ProviderProperties properties = new ProviderProperties.Builder()
                .setHasSatelliteRequirement(LocationManager.GPS_PROVIDER.equals(provider))
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setAccuracy(ProviderProperties.ACCURACY_FINE)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .build();
        manager.addTestProvider(provider, properties);
        try { manager.setTestProviderEnabled(provider, true); ensureAuthorized(); }
        catch (RuntimeException e) {
            try { manager.removeTestProvider(provider); }
            catch (RuntimeException cleanup) { e.addSuppressed(cleanup); }
            throw e;
        }
    }

    public void send(String provider, Coordinates coordinates) {
        ensureAuthorized();
        Location location = new Location(provider);
        location.setLatitude(coordinates.latitude);
        location.setLongitude(coordinates.longitude);
        location.setAccuracy(8f);
        location.setAltitude(0d);
        location.setVerticalAccuracyMeters(10f);
        location.setSpeed(0f);
        location.setSpeedAccuracyMetersPerSecond(0.1f);
        location.setBearing(0f);
        location.setBearingAccuracyDegrees(1f);
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        manager.setTestProviderLocation(provider, location);
        ensureAuthorized();
    }

    public void remove(String provider) {
        ensureAuthorized();
        manager.removeTestProvider(provider);
        ensureAuthorized();
    }
}
