package com.tegufy.blackbox.core.system.user;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Per-profile configuration for device spoofing and LSXposed/Xposed settings.
 * Each virtual user (profile) has its own isolated config stored at:
 *   blackbox/system/user/{userId}/profile.conf
 */
public class BUserProfileConfig implements Parcelable {

    // ── Build Identity ────────────────────────────────────────────────────────
    public String spoofModel;        // ro.product.model
    public String spoofBrand;        // ro.product.brand
    public String spoofManufacturer; // ro.product.manufacturer
    public String spoofDevice;       // ro.product.device
    public String spoofProduct;      // ro.product.name  e.g. "p3sxxx"
    public String spoofFingerprint;  // ro.build.fingerprint
    public String spoofRelease;      // ro.build.version.release
    public String spoofSecurityPatch;// ro.build.version.security_patch
    public String spoofSerial;       // ro.serialno
    public String spoofHardware;     // ro.hardware (also drives sensor/codec vendor filter)
    public String spoofBoard;        // ro.product.board
    public String spoofDisplay;      // ro.build.display.id
    public String spoofBuildId;      // ro.build.id
    public String spoofIncremental;  // ro.build.version.incremental
    public String spoofSdkInt;       // ro.build.version.sdk (parsed as Int)
    public String spoofBuildTags;    // ro.build.tags  e.g. "release-keys"
    public String spoofBuildType;    // ro.build.type  "user" / "userdebug" / "eng"
    public String spoofBuildTime;    // ro.build.date.utc (epoch ms, parsed as Long)
    // Extended Build
    public String spoofCodename;     // ro.build.version.codename  "REL" for release builds
    public String spoofFirstSdkInt;  // ro.product.first_api_level (parsed as Int)
    public String spoofBaseband;     // gsm.version.baseband / ro.baseband  e.g. "g0s-S911BXXU3BWI1"
    // Samsung CSC (OEM-specific, ignored on non-Samsung)
    public String spoofSalesCode;    // ro.csc.sales_code  e.g. "XTC"
    public String spoofSalesRegion;  // ro.csc.country_iso  e.g. "SEA"
    public String spoofSalesCountry; // ro.csc.country  e.g. "VN"

    // ── Identifiers ───────────────────────────────────────────────────────────
    public String spoofAndroidId;    // Settings.Secure.ANDROID_ID — 16 lowercase hex chars
    public String spoofImei;         // 15 digits, Luhn-valid
    public String spoofImsi;         // 15 digits (MCC + MNC + subscriber)
    public String spoofIccid;        // 19–20 digits

    // ── Network ───────────────────────────────────────────────────────────────
    public String spoofMac;             // XX:XX:XX:XX:XX:XX
    public String spoofBluetoothMac;    // XX:XX:XX:XX:XX:XX
    public String spoofOperatorName;    // e.g. "T-Mobile"
    public String spoofOperatorNumeric; // 5–6 digits (MCC+MNC)

    // ── Display ───────────────────────────────────────────────────────────────
    public String spoofScreenWidth;  // DisplayMetrics.widthPixels  (parsed as Int)
    public String spoofScreenHeight; // DisplayMetrics.heightPixels (parsed as Int)
    public String spoofDensityDpi;   // DisplayMetrics.densityDpi   (parsed as Int)

    // ── Camera (Camera2) ──────────────────────────────────────────────────────
    public String spoofFocalLength;  // LENS_INFO_AVAILABLE_FOCAL_LENGTHS, mm (Float)
    public String spoofAperture;     // LENS_INFO_AVAILABLE_APERTURES, f-number (Float)
    public String spoofSensorWidth;  // SENSOR_INFO_PHYSICAL_SIZE width, mm (Float)
    public String spoofSensorHeight; // SENSOR_INFO_PHYSICAL_SIZE height, mm (Float)

    // ── OpenGL / GPU ──────────────────────────────────────────────────────────
    public String spoofGlRenderer;   // e.g. "Mali-G78 MP14"
    public String spoofGlVendor;     // e.g. "ARM"
    public String spoofGlVersion;    // e.g. "OpenGL ES 3.2 v1.r21p0-..."

    // ── Native / Hardware ─────────────────────────────────────────────────────
    public String spoofKernelVersion;// /proc/version string
    public String spoofTotalRamMb;   // ActivityManager.MemoryInfo.totalMem/1048576 (Int)

    // ── WebView ───────────────────────────────────────────────────────────────
    public String spoofUserAgent;    // navigator.userAgent / WebView UA override

    // ── Location ──────────────────────────────────────────────────────────────
    public String spoofLatitude;     // Decimal degrees WGS84 (Double)
    public String spoofLongitude;    // Decimal degrees WGS84 (Double)
    public String spoofAltitude;     // Meters above sea level (Double)
    public String spoofAccuracy;     // Horizontal accuracy radius in meters (Float)
    public String spoofNoiseMeter;   // Max random drift per request in meters (Float)

    // ── Package Hiding ────────────────────────────────────────────────────────
    public String spoofHiddenPackages; // comma-separated package names

    // ── Xposed/LSXposed settings per profile ──────────────────────────────────
    public boolean xposedEnabled;
    public boolean hideXposed;

    public BUserProfileConfig() {
    }

    /** Returns true if any spoof field is configured (any group). */
    public boolean hasSpoofConfig() {
        return nonEmpty(spoofModel) || nonEmpty(spoofBrand) || nonEmpty(spoofManufacturer)
                || nonEmpty(spoofDevice) || nonEmpty(spoofProduct) || nonEmpty(spoofFingerprint)
                || nonEmpty(spoofRelease) || nonEmpty(spoofSerial) || nonEmpty(spoofHardware)
                || nonEmpty(spoofBoard) || nonEmpty(spoofBuildId) || nonEmpty(spoofSdkInt)
                || nonEmpty(spoofAndroidId) || nonEmpty(spoofImei) || nonEmpty(spoofImsi)
                || nonEmpty(spoofIccid) || nonEmpty(spoofMac) || nonEmpty(spoofBluetoothMac)
                || nonEmpty(spoofOperatorName) || nonEmpty(spoofOperatorNumeric)
                || nonEmpty(spoofGlRenderer) || nonEmpty(spoofGlVendor)
                || nonEmpty(spoofLatitude) || nonEmpty(spoofLongitude)
                || nonEmpty(spoofUserAgent) || nonEmpty(spoofHiddenPackages);
    }

    private static boolean nonEmpty(String s) {
        return s != null && !s.isEmpty();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Build Identity (original fields first — preserve wire-format order)
        dest.writeString(spoofModel);
        dest.writeString(spoofBrand);
        dest.writeString(spoofManufacturer);
        dest.writeString(spoofDevice);
        dest.writeString(spoofFingerprint);
        dest.writeString(spoofRelease);
        dest.writeString(spoofSecurityPatch);
        dest.writeString(spoofSerial);
        dest.writeByte((byte) (xposedEnabled ? 1 : 0));
        dest.writeByte((byte) (hideXposed ? 1 : 0));
        // Extended Build Identity
        dest.writeString(spoofHardware);
        dest.writeString(spoofBoard);
        dest.writeString(spoofDisplay);
        dest.writeString(spoofBuildId);
        dest.writeString(spoofIncremental);
        dest.writeString(spoofSdkInt);
        dest.writeString(spoofBuildTags);
        dest.writeString(spoofBuildType);
        dest.writeString(spoofBuildTime);
        dest.writeString(spoofProduct);
        dest.writeString(spoofCodename);
        dest.writeString(spoofFirstSdkInt);
        dest.writeString(spoofBaseband);
        dest.writeString(spoofSalesCode);
        dest.writeString(spoofSalesRegion);
        dest.writeString(spoofSalesCountry);
        // Identifiers
        dest.writeString(spoofAndroidId);
        dest.writeString(spoofImei);
        dest.writeString(spoofImsi);
        dest.writeString(spoofIccid);
        // Network
        dest.writeString(spoofMac);
        dest.writeString(spoofBluetoothMac);
        dest.writeString(spoofOperatorName);
        dest.writeString(spoofOperatorNumeric);
        // Display
        dest.writeString(spoofScreenWidth);
        dest.writeString(spoofScreenHeight);
        dest.writeString(spoofDensityDpi);
        // Camera
        dest.writeString(spoofFocalLength);
        dest.writeString(spoofAperture);
        dest.writeString(spoofSensorWidth);
        dest.writeString(spoofSensorHeight);
        // OpenGL / GPU
        dest.writeString(spoofGlRenderer);
        dest.writeString(spoofGlVendor);
        dest.writeString(spoofGlVersion);
        // Native
        dest.writeString(spoofKernelVersion);
        dest.writeString(spoofTotalRamMb);
        // WebView
        dest.writeString(spoofUserAgent);
        // Location
        dest.writeString(spoofLatitude);
        dest.writeString(spoofLongitude);
        dest.writeString(spoofAltitude);
        dest.writeString(spoofAccuracy);
        dest.writeString(spoofNoiseMeter);
        // Package Hiding
        dest.writeString(spoofHiddenPackages);
    }

    protected BUserProfileConfig(Parcel in) {
        // Build Identity (original)
        spoofModel = in.readString();
        spoofBrand = in.readString();
        spoofManufacturer = in.readString();
        spoofDevice = in.readString();
        spoofFingerprint = in.readString();
        spoofRelease = in.readString();
        spoofSecurityPatch = in.readString();
        spoofSerial = in.readString();
        xposedEnabled = in.readByte() != 0;
        hideXposed = in.readByte() != 0;
        // Extended Build Identity
        spoofHardware = in.readString();
        spoofBoard = in.readString();
        spoofDisplay = in.readString();
        spoofBuildId = in.readString();
        spoofIncremental = in.readString();
        spoofSdkInt = in.readString();
        spoofBuildTags = in.readString();
        spoofBuildType = in.readString();
        spoofBuildTime = in.readString();
        spoofProduct = in.readString();
        spoofCodename = in.readString();
        spoofFirstSdkInt = in.readString();
        spoofBaseband = in.readString();
        spoofSalesCode = in.readString();
        spoofSalesRegion = in.readString();
        spoofSalesCountry = in.readString();
        // Identifiers
        spoofAndroidId = in.readString();
        spoofImei = in.readString();
        spoofImsi = in.readString();
        spoofIccid = in.readString();
        // Network
        spoofMac = in.readString();
        spoofBluetoothMac = in.readString();
        spoofOperatorName = in.readString();
        spoofOperatorNumeric = in.readString();
        // Display
        spoofScreenWidth = in.readString();
        spoofScreenHeight = in.readString();
        spoofDensityDpi = in.readString();
        // Camera
        spoofFocalLength = in.readString();
        spoofAperture = in.readString();
        spoofSensorWidth = in.readString();
        spoofSensorHeight = in.readString();
        // OpenGL / GPU
        spoofGlRenderer = in.readString();
        spoofGlVendor = in.readString();
        spoofGlVersion = in.readString();
        // Native
        spoofKernelVersion = in.readString();
        spoofTotalRamMb = in.readString();
        // WebView
        spoofUserAgent = in.readString();
        // Location
        spoofLatitude = in.readString();
        spoofLongitude = in.readString();
        spoofAltitude = in.readString();
        spoofAccuracy = in.readString();
        spoofNoiseMeter = in.readString();
        // Package Hiding
        spoofHiddenPackages = in.readString();
    }

    public static final Creator<BUserProfileConfig> CREATOR = new Creator<BUserProfileConfig>() {
        @Override
        public BUserProfileConfig createFromParcel(Parcel source) {
            return new BUserProfileConfig(source);
        }

        @Override
        public BUserProfileConfig[] newArray(int size) {
            return new BUserProfileConfig[size];
        }
    };

    @Override
    public String toString() {
        return "BUserProfileConfig{"
                + "model='" + spoofModel + '\''
                + ", brand='" + spoofBrand + '\''
                + ", fingerprint='" + spoofFingerprint + '\''
                + ", serial='" + spoofSerial + '\''
                + ", xposedEnabled=" + xposedEnabled
                + ", hideXposed=" + hideXposed
                + '}';
    }
}
