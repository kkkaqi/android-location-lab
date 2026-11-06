package com.tegufy.blackbox.utils.compat;
import android.content.Intent;
import android.os.IBinder;
/** The null-start regression must never access a binder or Android parcel. */
public final class BundleCompat {
 public static IBinder getBinder(Intent intent,String key){throw new AssertionError("Unexpected binder access in null-start test");}
 public static void putBinder(Intent intent,String key,IBinder value){throw new AssertionError("Unexpected binder access in null-start test");}
}
